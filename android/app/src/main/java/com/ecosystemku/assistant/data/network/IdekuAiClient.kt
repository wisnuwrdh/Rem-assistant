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
private data class IdekuAiRequestDto(
    val title: String,
    val notes: String,
    val category: String,
    val priority: String,
    val mode: String
)

@Serializable
private data class IdekuAiResponseDto(
    val result: String? = null,
    val error: String? = null
)

sealed interface IdekuAiResult {
    data class Success(val text: String) : IdekuAiResult
    data class Failure(val message: String) : IdekuAiResult
}

class IdekuAiClient(
    private val httpClient: OkHttpClient,
    private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generate(
        title: String,
        notes: String,
        category: String,
        priority: String,
        mode: String
    ): IdekuAiResult = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            return@withContext IdekuAiResult.Failure("Judul ide kosong")
        }
        runCatching {
            val bodyJson = json.encodeToString(
                IdekuAiRequestDto(
                    title = cleanTitle.take(200),
                    notes = notes.take(1500),
                    category = category.ifBlank { "Personal" },
                    priority = priority.ifBlank { "Nanti" },
                    mode = if (mode == "breakdown") "breakdown" else "perkaya"
                )
            )
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/ideku")
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string()
                val parsed = raw?.let { runCatching { json.decodeFromString<IdekuAiResponseDto>(it) }.getOrNull() }
                if (!resp.isSuccessful) {
                    val errText = when (parsed?.error) {
                        "quota" -> "Kuota AI habis, coba beberapa saat lagi."
                        else -> parsed?.error ?: "Gagal menghubungi server AI (${resp.code})."
                    }
                    return@use IdekuAiResult.Failure(errText)
                }
                val result = parsed?.result?.trim()
                if (!result.isNullOrEmpty()) {
                    IdekuAiResult.Success(result)
                } else {
                    IdekuAiResult.Failure("Respons AI kosong.")
                }
            }
        }.getOrElse {
            IdekuAiResult.Failure("Gagal menghubungi AI. Periksa koneksi internet.")
        }
    }
}
