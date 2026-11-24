package com.ecosystemku.assistant.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ecosystemku.assistant.MainActivity
import com.ecosystemku.assistant.R
import java.util.Calendar

object DailyOsNotificationHelper {
    const val CHANNEL_ID = "dailyos_reminders"
    const val EXTRA_LAUNCH_ACTION = "launch_action"
    const val ACTION_KENALI_AKU = "kenali_aku"
    private const val REQ_HABIT = 1001
    private const val REQ_TODO = 1002
    private const val REQ_KG_WEEKLY = 1003

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat Daily OS",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Jadwal pengingat Habit & To-Do harian dari Rem"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        launchAction: String? = null
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!launchAction.isNullOrBlank()) {
                putExtra(EXTRA_LAUNCH_ACTION, launchAction)
            }
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    fun postTestNotification(context: Context): Boolean {
        return postNotification(
            context = context,
            notificationId = 9001,
            title = "Rem • Pengingat Daily OS",
            body = "Halo Tuan! Notifikasi pengingat Habit & To-Do sudah aktif dan siap mengingatkan Tuan. 💙"
        )
    }

    fun syncSchedules(
        context: Context,
        habitEnabled: Boolean,
        habitTime: String,
        todoEnabled: Boolean,
        todoTime: String
    ) {
        ensureChannel(context)
        scheduleSingle(
            context = context,
            requestCode = REQ_HABIT,
            enabled = habitEnabled,
            timeStr = habitTime,
            title = "Rem • Pengingat Habit Hari Ini",
            body = "Tuan, jangan lupa cek dan selesaikan habit Daily OS hari ini ya. 💙"
        )
        scheduleSingle(
            context = context,
            requestCode = REQ_TODO,
            enabled = todoEnabled,
            timeStr = todoTime,
            title = "Rem • Pengingat Tugas Pending",
            body = "Tuan, masih ada daftar To-Do di Daily OS yang menunggu diselesaikan. 💙"
        )
    }

    private fun scheduleSingle(
        context: Context,
        requestCode: Int,
        enabled: Boolean,
        timeStr: String,
        title: String,
        body: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyOsAlarmReceiver::class.java).apply {
            putExtra("notif_id", requestCode)
            putExtra("title", title)
            putExtra("body", body)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        if (!enabled) return

        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pending
            )
        }
    }

    fun scheduleOneTimeReminder(
        context: Context,
        reminderId: Long,
        triggerAtMs: Long,
        title: String,
        body: String
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        ensureChannel(context)
        val reqCode = 20_000 + ((reminderId.coerceAtLeast(1L) % 100_000L).toInt())
        val intent = Intent(context, DailyOsAlarmReceiver::class.java).apply {
            putExtra("notif_id", reqCode)
            putExtra("reminder_id", reminderId)
            putExtra("title", title)
            putExtra("body", body)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return runCatching {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
            }
            true
        }.getOrDefault(false)
    }

    fun syncWeeklyReflectionSchedule(
        context: Context,
        enabled: Boolean,
        dayOfWeek: Int = Calendar.SUNDAY,
        timeStr: String = "20:00"
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyOsAlarmReceiver::class.java).apply {
            putExtra("notif_id", REQ_KG_WEEKLY)
            putExtra("title", "Rem • Refleksi Mingguan Otak Digital")
            putExtra("body", "Tuan, yuk luangkan 2 menit untuk Sesi Kenali Aku mingguan agar peta pola pikirmu makin akurat. 💙")
            putExtra(EXTRA_LAUNCH_ACTION, ACTION_KENALI_AKU)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQ_KG_WEEKLY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        if (!enabled) return

        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val targetDay = dayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY)

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, targetDay)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7L,
                pending
            )
        }
    }

    fun cancelOneTimeReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reqCode = 20_000 + ((reminderId.coerceAtLeast(1L) % 100_000L).toInt())
        val intent = Intent(context, DailyOsAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }
}

class DailyOsAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("notif_id", 1001)
        val title = intent.getStringExtra("title") ?: "Rem • Pengingat Daily OS"
        val body = intent.getStringExtra("body") ?: "Jangan lupa cek Daily OS hari ini, Tuan. 💙"
        val launchAction = intent.getStringExtra(DailyOsNotificationHelper.EXTRA_LAUNCH_ACTION)
        DailyOsNotificationHelper.postNotification(context, id, title, body, launchAction)
    }
}
