package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.data.local.AiConfigField
import com.ecosystemku.assistant.data.local.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class LlmConfig(
    val url: String,
    val model: String,
    val key: String
)

@Serializable
private data class DirectCompletionRequestDto(
    val model: String,
    val messages: List<ChatTurnDto>,
    val temperature: Double,
    val max_tokens: Int,
    val stream: Boolean = false
)

object DirectLlmClient {
    const val LLM_MISSING_MESSAGE =
        "LLM belum dikonfigurasi. Isi Base URL, API Key, dan Model di Setelan."

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun resolveConfig(): LlmConfig? {
        val base = ApiKeyStore.getValue(AiConfigField.LLM_BASE_URL).trim()
        val model = ApiKeyStore.getValue(AiConfigField.LLM_MODEL).trim()
        val key = ApiKeyStore.getValue(AiConfigField.LLM_API_KEY).trim()
        if (base.isEmpty() || model.isEmpty() || key.isEmpty()) return null
        if (!base.startsWith("http://", ignoreCase = true) &&
            !base.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }

        val trimmed = base.trimEnd('/')
        val url = if (trimmed.endsWith("/chat/completions", ignoreCase = true)) {
            trimmed
        } else {
            "$trimmed/chat/completions"
        }
        return LlmConfig(url = url, model = model, key = key)
    }

    suspend fun complete(
        httpClient: OkHttpClient,
        messages: List<ChatTurnDto>,
        temperature: Double = 0.6,
        maxTokens: Int = 700
    ): Result<String> = withContext(Dispatchers.IO) {
        val config = resolveConfig() ?: return@withContext Result.failure(
            IllegalStateException(LLM_MISSING_MESSAGE)
        )

        runCatching {
            val payload = json.encodeToString(
                DirectCompletionRequestDto(
                    model = config.model,
                    messages = messages,
                    temperature = temperature,
                    max_tokens = maxTokens,
                    stream = false
                )
            )
            val req = Request.Builder()
                .url(config.url)
                .header("Authorization", "Bearer ${config.key}")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    if (resp.code == 429) {
                        error("quota")
                    }
                    val upstreamMsg = extractErrorMessage(raw)
                    val msg = when {
                        resp.code == 401 || resp.code == 403 ->
                            upstreamMsg ?: "API Key LLM ditolak (${resp.code})"
                        resp.code == 404 ->
                            upstreamMsg ?: "Base URL / Model tidak ditemukan (404)"
                        else -> upstreamMsg ?: "AI provider error (${resp.code})"
                    }
                    error(msg)
                }

                val content = extractCompletionContent(raw)
                if (content.isBlank()) {
                    error("Respons AI kosong")
                }
                content
            }
        }
    }

    fun extractCompletionContent(rawJson: String): String {
        if (rawJson.isBlank()) return ""
        return runCatching {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val choices = root["choices"] as? JsonArray ?: return@runCatching ""
            val first = choices.firstOrNull() as? JsonObject ?: return@runCatching ""
            val message = first["message"] as? JsonObject ?: return@runCatching ""
            val content = message["content"] as? JsonPrimitive
            if (content != null && content.isString) content.content.trim() else ""
        }.getOrDefault("")
    }

    fun extractErrorMessage(rawBody: String): String? {
        if (rawBody.isBlank()) return null
        return runCatching {
            val root = json.parseToJsonElement(rawBody).jsonObject
            val errObj = root["error"]
            when (errObj) {
                is JsonObject -> (errObj["message"] as? JsonPrimitive)
                    ?.takeIf { it.isString }
                    ?.content
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                is JsonPrimitive -> errObj.takeIf { it.isString }
                    ?.content
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                else -> (root["message"] as? JsonPrimitive)
                    ?.takeIf { it.isString }
                    ?.content
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            }
        }.getOrNull()
    }

    fun stripJsonFences(raw: String): String {
        var clean = raw
            .replace(Regex("""```json\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""```\s*"""), "")
            .trim()
        val firstBracket = clean.indexOf('[')
        val firstBrace = clean.indexOf('{')
        if (firstBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
            val lastBracket = clean.lastIndexOf(']')
            if (lastBracket > firstBracket) return clean.slice(firstBracket..lastBracket)
        }
        if (firstBrace != -1) {
            val lastBrace = clean.lastIndexOf('}')
            if (lastBrace > firstBrace) return clean.slice(firstBrace..lastBrace)
        }
        return clean
    }
}
