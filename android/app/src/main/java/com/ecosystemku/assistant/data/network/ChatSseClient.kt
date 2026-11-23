package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.BuildConfig
import com.ecosystemku.assistant.data.local.AiConfigField
import com.ecosystemku.assistant.data.local.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Serializable
data class ChatTurnDto(
    val role: String,
    val content: String
)

@Serializable
private data class LlmChatRequestDto(
    val model: String,
    val messages: List<ChatTurnDto>,
    val temperature: Double = 0.75,
    val max_tokens: Int = 1000,
    val stream: Boolean = true
)

@Serializable
private data class LlmPingRequestDto(
    val model: String,
    val messages: List<ChatTurnDto>,
    val max_tokens: Int = 16,
    val stream: Boolean = false
)

sealed interface ChatStreamEvent {
    data class Delta(val text: String) : ChatStreamEvent
    data class Done(val text: String, val tts: String) : ChatStreamEvent
    data class Error(val message: String) : ChatStreamEvent
}

class ChatSseClient(
    @Suppress("unused") private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val rawTestClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val llmBase = ApiKeyStore.getValue(AiConfigField.LLM_BASE_URL)
        val llmModel = ApiKeyStore.getValue(AiConfigField.LLM_MODEL)
        val llmKey = ApiKeyStore.getValue(AiConfigField.LLM_API_KEY)
        if (llmBase.isBlank() || llmModel.isBlank() || llmKey.isBlank()) {
            return@withContext false to "Lengkapi Base URL, Model, & API Key LLM"
        }
        val config = DirectLlmClient.resolveConfig()
            ?: return@withContext false to "Base URL harus diawali http:// atau https://"

        runCatching {
            val pingJson = json.encodeToString(
                LlmPingRequestDto(
                    model = config.model,
                    messages = listOf(ChatTurnDto("user", "ok")),
                    max_tokens = 16,
                    stream = false
                )
            )
            val llmReq = Request.Builder()
                .url(config.url)
                .header("Authorization", "Bearer ${config.key}")
                .post(pingJson.toRequestBody(jsonMediaType))
                .build()
            rawTestClient.newCall(llmReq).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                val upstreamErr = DirectLlmClient.extractErrorMessage(rawBody)
                when {
                    resp.isSuccessful -> true to "Terhubung (${config.model})"
                    resp.code == 401 || resp.code == 403 ->
                        false to (upstreamErr ?: "API Key LLM ditolak (${resp.code})")
                    resp.code == 404 ->
                        false to (upstreamErr ?: "Base URL / Model tidak ditemukan (404)")
                    resp.code == 429 ->
                        false to "Rate limit / kuota LLM habis (429)"
                    else -> false to (upstreamErr ?: "LLM HTTP ${resp.code}")
                }
            }
        }.getOrElse { err ->
            false to "LLM gagal: ${err.message?.take(40) ?: "Tidak terjangkau"}"
        }
    }

    fun streamChat(
        messages: List<ChatTurnDto>,
        systemPrompt: String,
        @Suppress("unused") currentExpression: String
    ): Flow<ChatStreamEvent> = callbackFlow {
        val config = DirectLlmClient.resolveConfig()
        if (config == null) {
            trySend(ChatStreamEvent.Error(DirectLlmClient.LLM_MISSING_MESSAGE))
            close()
            awaitClose {}
            return@callbackFlow
        }

        val sanitized = messages
            .filter { it.content.isNotBlank() }
            .takeLast(10)
            .map {
                ChatTurnDto(
                    role = if (it.role == "assistant") "assistant" else "user",
                    content = it.content.take(4000)
                )
            }

        if (sanitized.isEmpty()) {
            trySend(ChatStreamEvent.Error("Tidak ada pesan valid"))
            close()
            awaitClose {}
            return@callbackFlow
        }

        val fullMessages = buildList {
            add(ChatTurnDto(role = "system", content = systemPrompt + JA_RULE))
            addAll(sanitized)
        }

        val bodyJson = json.encodeToString(
            LlmChatRequestDto(
                model = config.model,
                messages = fullMessages,
                temperature = 0.75,
                max_tokens = 1000,
                stream = true
            )
        )

        val request = Request.Builder()
            .url(config.url)
            .header("Authorization", "Bearer ${config.key}")
            .header("Accept", "text/event-stream")
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()

        val rawFull = StringBuilder()
        val finished = AtomicBoolean(false)

        fun emitDoneAndClose() {
            if (!finished.compareAndSet(false, true)) return
            val (text, tts) = splitTextAndJapanese(rawFull.toString())
            if (text.isBlank()) {
                trySend(ChatStreamEvent.Error("Respons AI kosong"))
            } else {
                trySend(ChatStreamEvent.Done(text, tts))
            }
            close()
        }

        val factory = EventSources.createFactory(httpClient)
        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (finished.get()) return
                val payload = data.trim()
                if (payload.isEmpty()) return
                if (payload == "[DONE]") {
                    emitDoneAndClose()
                    eventSource.cancel()
                    return
                }

                val delta = extractStreamDelta(payload)
                if (!delta.isNullOrEmpty()) {
                    rawFull.append(delta)
                    trySend(ChatStreamEvent.Delta(delta))
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (finished.get()) return
                val rawBody = runCatching { response?.body?.string() }.getOrNull().orEmpty()

                // Fallback jika provider mengabaikan stream=true dan mengembalikan JSON 200 OK
                if (response?.isSuccessful == true && rawBody.isNotBlank()) {
                    val fallbackText = DirectLlmClient.extractCompletionContent(rawBody)
                    if (fallbackText.isNotBlank()) {
                        rawFull.append(fallbackText)
                        emitDoneAndClose()
                        return
                    }
                }

                if (!finished.compareAndSet(false, true)) return
                val upstreamErr = DirectLlmClient.extractErrorMessage(rawBody)
                val message = when {
                    response?.code == 429 -> "Rate limit LLM provider, coba lagi sebentar (429)"
                    response?.code == 401 || response?.code == 403 ->
                        upstreamErr ?: "API Key LLM ditolak (${response.code})"
                    response?.code == 404 ->
                        upstreamErr ?: "Base URL / Model LLM tidak ditemukan (404)"
                    upstreamErr != null -> upstreamErr
                    response != null -> "AI provider error (HTTP ${response.code})"
                    else -> t?.message ?: "Gagal menghubungi LLM provider"
                }
                trySend(ChatStreamEvent.Error(message))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                if (!finished.get()) {
                    if (rawFull.isNotEmpty()) {
                        emitDoneAndClose()
                    } else if (finished.compareAndSet(false, true)) {
                        trySend(ChatStreamEvent.Error("Koneksi stream ditutup sebelum menerima respons"))
                        close()
                    }
                }
            }
        }

        val eventSource = factory.newEventSource(request, listener)
        awaitClose {
            finished.set(true)
            eventSource.cancel()
        }
    }

    private fun extractStreamDelta(payloadJson: String): String? {
        return runCatching {
            val root = json.parseToJsonElement(payloadJson).jsonObject
            val choices = root["choices"] as? JsonArray ?: return@runCatching null
            val first = choices.firstOrNull() as? JsonObject ?: return@runCatching null
            val deltaObj = first["delta"] as? JsonObject
            val deltaContent = (deltaObj?.get("content") as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
            if (!deltaContent.isNullOrEmpty()) return@runCatching deltaContent

            val msgObj = first["message"] as? JsonObject
            (msgObj?.get("content") as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
        }.getOrNull()
    }

    private fun splitTextAndJapanese(full: String): Pair<String, String> {
        val m = JA_CAPTURE_REGEX.find(full)
        val tts = m?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val text = full.replace(JA_STRIP_REGEX, "").trim()
        return text to tts
    }

    companion object {
        private val JA_CAPTURE_REGEX = Regex("""\[JA]([\s\S]*?)\[/JA]""", RegexOption.IGNORE_CASE)
        private val JA_STRIP_REGEX = Regex("""\[JA][\s\S]*?(\[/JA]|$)""", RegexOption.IGNORE_CASE)

        private const val JA_RULE = """

ATURAN WAJIB (format output):
1. Tulis jawabanmu dalam bahasa Indonesia seperti biasa.
2. Di baris paling akhir, tambahkan terjemahan Jepang natural bergaya sopan dari jawaban itu, dibungkus [JA]...[/JA].
3. Terjemahan Jepang tanpa romaji, tanpa markdown, tanpa emoji.
Contoh: Halo, Tuan. Ada yang bisa Rem bantu? [JA]こんにちは、ご主人様。何かお手伝いできることはありますか？[/JA]"""
    }
}
