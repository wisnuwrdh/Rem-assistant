package com.ecosystemku.assistant.data.cloud

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
private data class DriveFileDto(
    val id: String = "",
    val name: String = "",
    val modifiedTime: String = "",
    val size: String? = null
)

@Serializable
private data class DriveFileListDto(
    val files: List<DriveFileDto> = emptyList()
)

@Serializable
private data class DriveCreateMetaDto(
    val name: String,
    val parents: List<String> = listOf("appDataFolder")
)

@Serializable
private data class DrivePatchMetaDto(
    val name: String
)

/**
 * Klien Google Drive REST API v3 menggunakan ruang tersembunyi `appDataFolder`
 * dan Google Identity AuthorizationClient + OkHttp tanpa SDK Drive Java yang berat.
 */
class GoogleDriveBackupClient(
    private val appContext: Context,
    private val httpClient: OkHttpClient = OkHttpClient()
) : CloudBackupProvider {

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val PLAIN_FILE_NAME = "asisten-rem-backup.zip"
        const val ENCRYPTED_FILE_NAME = "asisten-rem-backup.rembak"
        private const val FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_ENDPOINT = "https://www.googleapis.com/upload/drive/v3/files"
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val authClient by lazy { Identity.getAuthorizationClient(appContext) }

    override suspend fun authorizeInteractive(): CloudAuthResult {
        return runCatching {
            val req = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
                .build()
            val result = authClient.authorize(req).awaitResult()
            if (result.hasResolution()) {
                val pending = result.pendingIntent
                if (pending != null) {
                    CloudAuthResult.NeedsResolution(pending.intentSender)
                } else {
                    CloudAuthResult.Failure("Izin Google Drive membutuhkan konfirmasi pengguna")
                }
            } else {
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    CloudAuthResult.Authorized(token)
                } else {
                    CloudAuthResult.Failure("Token Google Drive tidak tersedia")
                }
            }
        }.getOrElse { err ->
            CloudAuthResult.Failure(err.message ?: "Gagal menghubungi layanan Google Identity")
        }
    }

    fun extractTokenFromIntent(data: Intent?): CloudAuthResult {
        if (data == null) return CloudAuthResult.Failure("Otorisasi Google Drive dibatalkan")
        return runCatching {
            val res = authClient.getAuthorizationResultFromIntent(data)
            val token = res.accessToken
            if (!token.isNullOrBlank()) {
                CloudAuthResult.Authorized(token)
            } else {
                CloudAuthResult.Failure("Gagal membaca token otorisasi Google Drive")
            }
        }.getOrElse { err ->
            CloudAuthResult.Failure(err.message ?: "Otorisasi Google Drive gagal")
        }
    }

    private suspend fun getSilentToken(): String? {
        return runCatching {
            val req = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
                .build()
            val result: AuthorizationResult = authClient.authorize(req).awaitResult()
            if (result.hasResolution()) null else result.accessToken?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    override suspend fun getRemoteBackupInfo(): CloudSyncResult<CloudBackupInfo?> = withContext(Dispatchers.IO) {
        val token = getSilentToken() ?: return@withContext CloudSyncResult.NeedsReauth
        fetchRemoteBackupInfoWithToken(token)
    }

    private fun fetchRemoteBackupInfoWithToken(token: String): CloudSyncResult<CloudBackupInfo?> {
        val query = URLEncoder.encode(
            "(name = '$PLAIN_FILE_NAME' or name = '$ENCRYPTED_FILE_NAME') and trashed = false",
            "UTF-8"
        )
        val url = "$FILES_ENDPOINT?spaces=appDataFolder&q=$query&fields=files(id,name,modifiedTime,size)&orderBy=modifiedTime%20desc&pageSize=5"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return CloudSyncResult.NeedsReauth
                }
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return CloudSyncResult.Failure("Google Drive error ${resp.code}: ${raw.take(180)}")
                }
                val parsed = json.decodeFromString<DriveFileListDto>(raw)
                val first = parsed.files.firstOrNull { it.id.isNotBlank() }
                CloudSyncResult.Success(first?.toBackupInfo())
            }
        } catch (ioe: IOException) {
            CloudSyncResult.Failure("Koneksi jaringan bermasalah: ${ioe.message}", isTransientNetwork = true)
        } catch (t: Throwable) {
            CloudSyncResult.Failure("Gagal memeriksa backup cloud: ${t.message}")
        }
    }

    override suspend fun uploadBackup(bytes: ByteArray, encrypted: Boolean): CloudSyncResult<CloudBackupInfo> =
        withContext(Dispatchers.IO) {
            val token = getSilentToken() ?: return@withContext CloudSyncResult.NeedsReauth
            val existingRes = fetchRemoteBackupInfoWithToken(token)
            val existingId = when (existingRes) {
                is CloudSyncResult.Success -> existingRes.data?.fileId
                is CloudSyncResult.NeedsReauth -> return@withContext CloudSyncResult.NeedsReauth
                is CloudSyncResult.Failure -> return@withContext existingRes
            }

            val fileName = if (encrypted) ENCRYPTED_FILE_NAME else PLAIN_FILE_NAME
            val metaJson = if (existingId != null) {
                json.encodeToString(DrivePatchMetaDto(name = fileName))
            } else {
                json.encodeToString(DriveCreateMetaDto(name = fileName))
            }

            val metaPart = metaJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
            val dataPart = bytes.toRequestBody(
                (if (encrypted) "application/octet-stream" else "application/zip").toMediaType()
            )
            val body = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(metaPart)
                .addPart(dataPart)
                .build()

            val targetUrl = if (existingId != null) {
                "$UPLOAD_ENDPOINT/$existingId?uploadType=multipart&fields=id,name,modifiedTime,size"
            } else {
                "$UPLOAD_ENDPOINT?uploadType=multipart&fields=id,name,modifiedTime,size"
            }

            val reqBuilder = Request.Builder()
                .url(targetUrl)
                .header("Authorization", "Bearer $token")
            val req = if (existingId != null) {
                reqBuilder.patch(body).build()
            } else {
                reqBuilder.post(body).build()
            }

            try {
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.code == 401 || resp.code == 403) {
                        return@withContext CloudSyncResult.NeedsReauth
                    }
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext CloudSyncResult.Failure("Upload gagal (${resp.code}): ${raw.take(180)}")
                    }
                    val dto = json.decodeFromString<DriveFileDto>(raw)
                    CloudSyncResult.Success(dto.toBackupInfo())
                }
            } catch (ioe: IOException) {
                CloudSyncResult.Failure("Upload terputus: ${ioe.message}", isTransientNetwork = true)
            } catch (t: Throwable) {
                CloudSyncResult.Failure("Upload gagal: ${t.message}")
            }
        }

    override suspend fun downloadBackup(fileId: String): CloudSyncResult<ByteArray> = withContext(Dispatchers.IO) {
        val token = getSilentToken() ?: return@withContext CloudSyncResult.NeedsReauth
        val req = Request.Builder()
            .url("$FILES_ENDPOINT/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return@withContext CloudSyncResult.NeedsReauth
                }
                if (!resp.isSuccessful) {
                    val msg = resp.body?.string().orEmpty().take(180)
                    return@withContext CloudSyncResult.Failure("Download gagal (${resp.code}): $msg")
                }
                val bytes = resp.body?.bytes()
                    ?: return@withContext CloudSyncResult.Failure("Respons Google Drive kosong")
                CloudSyncResult.Success(bytes)
            }
        } catch (ioe: IOException) {
            CloudSyncResult.Failure("Download terputus: ${ioe.message}", isTransientNetwork = true)
        } catch (t: Throwable) {
            CloudSyncResult.Failure("Download gagal: ${t.message}")
        }
    }

    private fun DriveFileDto.toBackupInfo(): CloudBackupInfo {
        val modifiedMs = runCatching {
            if (modifiedTime.isBlank()) 0L else Instant.parse(modifiedTime).toEpochMilli()
        }.getOrDefault(0L)
        return CloudBackupInfo(
            fileId = id,
            fileName = name,
            modifiedAtMs = modifiedMs,
            sizeBytes = size?.toLongOrNull() ?: 0L,
            encrypted = name.endsWith(".rembak", ignoreCase = true)
        )
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { res ->
        if (cont.isActive) cont.resume(res)
    }
    addOnFailureListener { err ->
        if (cont.isActive) cont.resumeWith(Result.failure(err))
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}
