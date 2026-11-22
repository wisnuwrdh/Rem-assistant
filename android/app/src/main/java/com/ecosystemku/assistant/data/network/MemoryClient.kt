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
data class ExistingFactDto(
    val factKey: String,
    val factValue: String
)

@Serializable
private data class MemoryExtractRequestDto(
    val messages: List<ChatTurnDto>,
    val existingFacts: List<ExistingFactDto>
)

@Serializable
data class ExtractedFactDto(
    val category: String = "catatan",
    val factKey: String = "",
    val factValue: String = "",
    val confidence: Float = 0.8f
)

@Serializable
data class MemoryExtractResponseDto(
    val facts: List<ExtractedFactDto> = emptyList(),
    val summary: String? = null
)

class MemoryClient(
    private val httpClient: OkHttpClient,
    private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun extractMemories(
        messages: List<ChatTurnDto>,
        existingFacts: List<ExistingFactDto>
    ): MemoryExtractResponseDto? = withContext(Dispatchers.IO) {
        runCatching {
            val bodyJson = json.encodeToString(
                MemoryExtractRequestDto(
                    messages = messages.takeLast(8),
                    existingFacts = existingFacts.take(25)
                )
            )
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/memory/extract")
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val raw = resp.body?.string() ?: return@use null
                json.decodeFromString<MemoryExtractResponseDto>(raw)
            }
        }.getOrNull()
    }
}
