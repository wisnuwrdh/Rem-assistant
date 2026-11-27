package com.ecosystemku.assistant.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ecosystemku.assistant.data.local.AssistantDatabase
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores AlarmManager-based schedules and WorkManager proactive checks after device reboot
 * or app package replacement.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val appContext = context.applicationContext
        val asistenPrefs = appContext.getSharedPreferences("asisten_prefs", Context.MODE_PRIVATE)
        val dailyOsPrefs = appContext.getSharedPreferences("dailyos_prefs", Context.MODE_PRIVATE)

        val habitEnabled = asistenPrefs.getBoolean("asisten_notif_habit_on", true)
        val todoEnabled = asistenPrefs.getBoolean("asisten_notif_todo_on", true)
        val habitTime = dailyOsPrefs.getString("do_notif_habit", "07:00") ?: "07:00"
        val todoTime = dailyOsPrefs.getString("do_notif_todo", "15:00") ?: "15:00"

        DailyOsNotificationHelper.syncSchedules(
            context = appContext,
            habitEnabled = habitEnabled,
            habitTime = habitTime,
            todoEnabled = todoEnabled,
            todoTime = todoTime
        )

        val kgWeeklyOn = asistenPrefs.getBoolean("asisten_kg_weekly_enabled", false)
        val kgWeeklyDay = asistenPrefs.getInt("asisten_kg_weekly_day", Calendar.SUNDAY)
            .coerceIn(Calendar.SUNDAY, Calendar.SATURDAY)
        val kgWeeklyTime = asistenPrefs.getString("asisten_kg_weekly_time", "20:00")
            ?.takeIf { it.contains(":") } ?: "20:00"

        DailyOsNotificationHelper.syncWeeklyReflectionSchedule(
            context = appContext,
            enabled = kgWeeklyOn,
            dayOfWeek = kgWeeklyDay,
            timeStr = kgWeeklyTime
        )

        ProactiveCheckScheduler.syncFromPrefs(appContext)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val name = DailyOsNotificationHelper.getProfileName(appContext)
                val activeReminders = AssistantDatabase.getInstance(appContext)
                    .remReminderDao()
                    .getActive(now)
                activeReminders.forEach { reminder ->
                    DailyOsNotificationHelper.scheduleOneTimeReminder(
                        context = appContext,
                        reminderId = reminder.id,
                        triggerAtMs = reminder.triggerAtMs,
                        title = "Rem · Pengingat",
                        body = "$name, pengingat dari Rem: ${reminder.title}",
                        launchAction = null
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
