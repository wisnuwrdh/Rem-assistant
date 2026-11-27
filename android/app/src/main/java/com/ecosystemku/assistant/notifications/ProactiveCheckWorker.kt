package com.ecosystemku.assistant.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ecosystemku.assistant.data.local.AssistantDatabase
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ProactivePrefsKeys {
    const val PREF_ENABLED = "asisten_proactive_enabled"
    const val PREF_FINANCE = "asisten_proactive_finance"
    const val PREF_HABITS = "asisten_proactive_habits"
    const val PREF_REFLECTION = "asisten_proactive_reflection"
    const val PREF_IDEAS = "asisten_proactive_ideas"
    const val PREF_QUIET_START = "asisten_proactive_quiet_start"
    const val PREF_QUIET_END = "asisten_proactive_quiet_end"

    private const val COOLDOWN_PREFS_NAME = "proactive_notif_prefs"
    private const val KEY_FIRED_DATE = "fired_date"
    private const val KEY_FIRED_COUNT = "fired_count"
    private const val KEY_COOLDOWN_PREFIX = "cd_"

    fun readPreferences(context: Context): ProactivePreferences {
        val prefs = context.applicationContext.getSharedPreferences("asisten_prefs", Context.MODE_PRIVATE)
        return ProactivePreferences(
            enabled = prefs.getBoolean(PREF_ENABLED, true),
            financeEnabled = prefs.getBoolean(PREF_FINANCE, true),
            habitsEnabled = prefs.getBoolean(PREF_HABITS, true),
            reflectionEnabled = prefs.getBoolean(PREF_REFLECTION, true),
            ideasEnabled = prefs.getBoolean(PREF_IDEAS, true),
            quietStart = prefs.getString(PREF_QUIET_START, "22:00")?.takeIf { it.contains(":") } ?: "22:00",
            quietEnd = prefs.getString(PREF_QUIET_END, "07:00")?.takeIf { it.contains(":") } ?: "07:00",
            dailyCap = 2
        )
    }

    fun readCooldownState(context: Context, nowMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): ProactiveCooldownState {
        val prefs = context.applicationContext.getSharedPreferences(COOLDOWN_PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate().toString()
        val storedDate = prefs.getString(KEY_FIRED_DATE, "").orEmpty()
        val count = if (storedDate == todayStr) prefs.getInt(KEY_FIRED_COUNT, 0) else 0
        val map = buildMap {
            prefs.all.forEach { (k, v) ->
                if (k.startsWith(KEY_COOLDOWN_PREFIX) && v is Long) {
                    put(k.removePrefix(KEY_COOLDOWN_PREFIX), v)
                }
            }
        }
        return ProactiveCooldownState(
            firedTodayCount = count,
            lastFiredByKeyMs = map
        )
    }

    fun recordFired(context: Context, alert: ProactiveAlert, nowMs: Long, zoneId: ZoneId = ZoneId.systemDefault()) {
        val prefs = context.applicationContext.getSharedPreferences(COOLDOWN_PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate().toString()
        val prevDate = prefs.getString(KEY_FIRED_DATE, "").orEmpty()
        val nextCount = if (prevDate == todayStr) prefs.getInt(KEY_FIRED_COUNT, 0) + 1 else 1
        prefs.edit()
            .putString(KEY_FIRED_DATE, todayStr)
            .putInt(KEY_FIRED_COUNT, nextCount)
            .putLong("$KEY_COOLDOWN_PREFIX${alert.cooldownKey}", nowMs)
            .apply()
    }
}

sealed interface ProactiveRunResult {
    data object PermissionMissing : ProactiveRunResult
    data object Disabled : ProactiveRunResult
    data object NoAlertNeeded : ProactiveRunResult
    data class Notified(val alert: ProactiveAlert) : ProactiveRunResult
}

object ProactiveCheckRunner {
    suspend fun runOnce(context: Context, manualTrigger: Boolean = false): ProactiveRunResult {
        val appContext = context.applicationContext
        val prefs = ProactivePrefsKeys.readPreferences(appContext)
        if (!prefs.enabled && !manualTrigger) return ProactiveRunResult.Disabled
        if (!DailyOsNotificationHelper.hasNotificationPermission(appContext)) {
            return ProactiveRunResult.PermissionMissing
        }

        val nowMs = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val db = AssistantDatabase.getInstance(appContext)
        val cuciDao = db.cuciMoneyDao()
        val dailyDao = db.dailyOsDao()
        val kronikDao = db.kronikDao()
        val idekuDao = db.idekuDao()

        val snapshot = ProactiveSnapshot(
            profileName = DailyOsNotificationHelper.getProfileName(appContext),
            books = cuciDao.getAllBooks(),
            transactions = cuciDao.getAllTransactions(),
            masterBudgets = cuciDao.getAllMasterBudgets(),
            wishlists = cuciDao.getAllWishlists(),
            habits = dailyDao.getAllHabits(),
            todos = dailyDao.getAllTodos(),
            kronikEntries = kronikDao.getVisibleEntries(limit = 10),
            ideas = idekuDao.getAll()
        )

        val cooldownState = ProactivePrefsKeys.readCooldownState(appContext, nowMs, zoneId)
        val effectivePrefs = if (manualTrigger) prefs.copy(enabled = true) else prefs
        val alert = ProactiveEvaluator.evaluate(
            snapshot = snapshot,
            prefs = effectivePrefs,
            cooldownState = cooldownState,
            nowMs = nowMs,
            zoneId = zoneId,
            ignoreQuietHours = manualTrigger,
            ignoreDailyCap = manualTrigger
        ) ?: return ProactiveRunResult.NoAlertNeeded

        val posted = DailyOsNotificationHelper.postNotification(
            context = appContext,
            notificationId = alert.notificationId,
            title = alert.title,
            body = alert.body,
            launchAction = alert.launchAction,
            channelId = alert.category.channelId,
            showActionButtons = false
        )
        if (!posted) return ProactiveRunResult.PermissionMissing

        if (!manualTrigger) {
            ProactivePrefsKeys.recordFired(appContext, alert, nowMs, zoneId)
        }
        return ProactiveRunResult.Notified(alert)
    }
}

object ProactiveCheckScheduler {
    private const val UNIQUE_WORK_NAME = "rem_proactive_cross_module_check"

    fun syncFromPrefs(context: Context) {
        val prefs = ProactivePrefsKeys.readPreferences(context)
        syncSchedule(context, prefs.enabled)
    }

    fun syncSchedule(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            wm.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val req = PeriodicWorkRequestBuilder<ProactiveCheckWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        wm.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class ProactiveCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            ProactiveCheckRunner.runOnce(applicationContext, manualTrigger = false)
            Result.success()
        }.getOrElse {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
