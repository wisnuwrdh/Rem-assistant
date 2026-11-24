package com.ecosystemku.assistant.data.cloud

import android.content.IntentSender

/**
 * Metadata berkas cadangan Ecosystem ZIP v3 di penyimpanan cloud.
 */
data class CloudBackupInfo(
    val fileId: String,
    val fileName: String,
    val modifiedAtMs: Long,
    val sizeBytes: Long,
    val encrypted: Boolean
)

sealed interface CloudAuthResult {
    data class Authorized(val accessToken: String) : CloudAuthResult
    data class NeedsResolution(val intentSender: IntentSender) : CloudAuthResult
    data class Failure(val message: String) : CloudAuthResult
}

sealed interface CloudSyncResult<out T> {
    data class Success<T>(val data: T) : CloudSyncResult<T>
    data object NeedsReauth : CloudSyncResult<Nothing>
    data class Failure(val message: String, val isTransientNetwork: Boolean = false) : CloudSyncResult<Nothing>
}

interface CloudBackupProvider {
    suspend fun authorizeInteractive(): CloudAuthResult
    suspend fun getRemoteBackupInfo(): CloudSyncResult<CloudBackupInfo?>
    suspend fun uploadBackup(bytes: ByteArray, encrypted: Boolean): CloudSyncResult<CloudBackupInfo>
    suspend fun downloadBackup(fileId: String): CloudSyncResult<ByteArray>
}

data class CloudBackupUiState(
    val connected: Boolean = false,
    val lastBackupLabel: String = "Belum pernah",
    val lastStatusLabel: String = "Belum terhubung",
    val remoteBackupInfo: CloudBackupInfo? = null,
    val autoSchedule: String = "off", // "off" | "daily" | "weekly"
    val wifiOnly: Boolean = true,
    val encryptionEnabled: Boolean = false,
    val isSyncing: Boolean = false
)

sealed interface CloudRestoreFetchResult {
    data class Ready(val zipBytes: ByteArray) : CloudRestoreFetchResult
    data class NeedsPassphrase(val encryptedBytes: ByteArray, val wrongSavedPassphrase: Boolean = false) : CloudRestoreFetchResult
    data object NeedsReauth : CloudRestoreFetchResult
    data class Error(val message: String) : CloudRestoreFetchResult
}
