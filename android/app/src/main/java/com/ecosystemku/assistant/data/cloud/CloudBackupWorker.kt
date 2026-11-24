package com.ecosystemku.assistant.data.cloud

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ecosystemku.assistant.data.backup.EcosystemBackupRepository
import com.ecosystemku.assistant.data.local.ApiKeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object CloudBackupPrefsKeys {
    const val PREF_CONNECTED = "asisten_cloud_connected"
    const val PREF_LAST_BACKUP = "asisten_cloud_last_backup"
    const val PREF_LAST_STATUS = "asisten_cloud_last_status"
    const val PREF_AUTO_SCHEDULE = "asisten_cloud_auto_schedule" // "off" | "daily" | "weekly"
    const val PREF_WIFI_ONLY = "asisten_cloud_wifi_only"
    const val PREF_REMOTE_SIZE = "asisten_cloud_remote_size"
    const val PREF_REMOTE_ENCRYPTED = "asisten_cloud_remote_encrypted"
}

object CloudBackupScheduler {
    private const val UNIQUE_WORK_NAME = "rem_cloud_auto_backup"

    fun syncSchedule(context: Context, schedule: String, wifiOnly: Boolean) {
        val wm = WorkManager.getInstance(context.applicationContext)
        val intervalHours = when (schedule) {
            "daily" -> 24L
            "weekly" -> 24L * 7L
            else -> {
                wm.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val req = PeriodicWorkRequestBuilder<CloudBackupWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        wm.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class CloudBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("asisten_prefs", Context.MODE_PRIVATE)
        val connected = prefs.getBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, false)
        val schedule = prefs.getString(CloudBackupPrefsKeys.PREF_AUTO_SCHEDULE, "off") ?: "off"
        if (!connected || schedule == "off") {
            return Result.success()
        }

        ApiKeyStore.init(applicationContext)
        val backupRepo = EcosystemBackupRepository(applicationContext)
        val driveClient = GoogleDriveBackupClient(applicationContext)

        val zipBytes = runCatching {
            backupRepo.buildEcosystemZipBytes(updateLocalStamp = false)
        }.getOrElse { err ->
            prefs.edit()
                .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Auto-backup gagal membaca data: ${err.message}")
                .apply()
            return Result.failure()
        }

        val passphrase = ApiKeyStore.getCloudPassphrase(applicationContext)
        val (payload, encrypted) = if (passphrase.isNotEmpty()) {
            CloudBackupCrypto.encrypt(zipBytes, passphrase) to true
        } else {
            zipBytes to false
        }

        return when (val res = driveClient.uploadBackup(payload, encrypted)) {
            is CloudSyncResult.Success -> {
                val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID")).format(Date())
                prefs.edit()
                    .putString(CloudBackupPrefsKeys.PREF_LAST_BACKUP, stamp)
                    .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Auto-backup berhasil ($stamp)")
                    .putLong(CloudBackupPrefsKeys.PREF_REMOTE_SIZE, res.data.sizeBytes.takeIf { it > 0L } ?: payload.size.toLong())
                    .putBoolean(CloudBackupPrefsKeys.PREF_REMOTE_ENCRYPTED, encrypted)
                    .apply()
                Result.success()
            }
            CloudSyncResult.NeedsReauth -> {
                prefs.edit()
                    .putBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, false)
                    .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Perlu hubungkan ulang Google Drive")
                    .apply()
                Result.failure()
            }
            is CloudSyncResult.Failure -> {
                prefs.edit()
                    .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, res.message)
                    .apply()
                if (res.isTransientNetwork && runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }
    }
}
