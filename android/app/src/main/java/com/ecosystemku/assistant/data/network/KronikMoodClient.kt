package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
private data class KronikMoodRequestDto(val text: String)

@Serializable
private data class KronikMoodResponseDto(
    val mood: String? = null,
    val error: String? = null
)

sealed interface KronikMoodDetectResult {
    data class Success(val mood: String) : KronikMoodDetectResult
    data class Failure(val message: String) : KronikMoodDetectResult
}

class KronikMoodClient(
    private val httpClient: OkHttpClient,
    private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun detectMood(text: String): KronikMoodDetectResult = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.length < 10) {
            return@withContext KronikMoodDetectResult.Failure("Tulis dulu isi jurnalnya ya...")
        }
        runCatching {
            val bodyJson = json.encodeToString(KronikMoodRequestDto(clean.take(900)))
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/kronik/mood")
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string()
                val parsed = raw?.let { runCatching { json.decodeFromString<KronikMoodResponseDto>(it) }.getOrNull() }
                if (!resp.isSuccessful) {
                    return@use KronikMoodDetectResult.Failure(
                        parsed?.error ?: "Gagal terhubung ke AI (${resp.code})."
                    )
                }
                val mood = parsed?.mood?.trim()
                if (!mood.isNullOrEmpty()) {
                    KronikMoodDetectResult.Success(mood)
                } else {
                    KronikMoodDetectResult.Failure("Mood tidak terdeteksi, pilih manual ya")
                }
            }
        }.getOrElse {
            KronikMoodDetectResult.Failure("Gagal terhubung ke AI. Coba lagi.")
        }
    }
}
