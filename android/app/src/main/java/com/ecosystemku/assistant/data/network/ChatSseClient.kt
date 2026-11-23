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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

@Serializable
data class ChatTurnDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatContextDto(
    val systemPrompt: String,
    val expression: String
)

@Serializable
data class ChatRequestDto(
    val messages: List<ChatTurnDto>,
    val context: ChatContextDto
)

@Serializable
private data class LlmPingRequestDto(
    val model: String,
    val messages: List<ChatTurnDto>,
    val stream: Boolean = false
)

@Serializable
private data class SseDeltaPayload(val delta: String = "")

@Serializable
private data class SseDonePayload(val text: String = "", val tts: String = "")

@Serializable
private data class SseErrorPayload(val error: String = "Terjadi kesalahan saat streaming")

sealed interface ChatStreamEvent {
    data class Delta(val text: String) : ChatStreamEvent
    data class Done(val text: String, val tts: String) : ChatStreamEvent
    data class Error(val message: String) : ChatStreamEvent
}

class ChatSseClient(
    private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val headers = ApiKeyStore.activeHeaders()
            if (headers.isEmpty()) {
                chain.proceed(chain.request())
            } else {
                val builder = chain.request().newBuilder()
                headers.forEach { (k, v) -> builder.header(k, v) }
                chain.proceed(builder.build())
            }
        }
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
        if (!llmBase.startsWith("http://", ignoreCase = true) &&
            !llmBase.startsWith("https://", ignoreCase = true)
        ) {
            return@withContext false to "Base URL harus diawali http:// atau https://"
        }

        // 1. Cek jangkauan server backend
        val backendCheck = runCatching {
            val req = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/chat")
                .method("OPTIONS", null)
                .build()
            rawTestClient.newCall(req).execute().use { resp ->
                resp.isSuccessful || resp.code in 200..499
            }
        }.getOrElse { err ->
            return@withContext false to "Backend: ${err.message?.take(32) ?: "Tidak terjangkau"}"
        }
        if (!backendCheck) {
            return@withContext false to "Server backend tidak merespons"
        }

        // 2. Uji endpoint LLM kustom secara langsung
        val trimmedBase = llmBase.trimEnd('/')
        val chatUrl = if (trimmedBase.endsWith("/chat/completions", ignoreCase = true)) {
            trimmedBase
        } else {
            "$trimmedBase/chat/completions"
        }

        runCatching {
            val pingJson = json.encodeToString(
                LlmPingRequestDto(
                    model = llmModel,
                    messages = listOf(ChatTurnDto("user", "ok")),
                    stream = false
                )
            )
            val llmReq = Request.Builder()
                .url(chatUrl)
                .header("Authorization", "Bearer $llmKey")
                .post(pingJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            rawTestClient.newCall(llmReq).execute().use { resp ->
                when {
                    resp.isSuccessful -> true to "Terhubung ($llmModel)"
                    resp.code == 401 || resp.code == 403 -> false to "API Key LLM ditolak (${resp.code})"
                    resp.code == 404 -> false to "Base URL / Model tidak ditemukan (404)"
                    resp.code == 429 -> false to "Rate limit / kuota LLM habis (429)"
                    else -> false to "LLM HTTP ${resp.code}"
                }
            }
        }.getOrElse { err ->
            false to "LLM gagal: ${err.message?.take(32) ?: "Tidak terjangkau"}"
        }
    }

    fun streamChat(
        messages: List<ChatTurnDto>,
        systemPrompt: String,
        currentExpression: String
    ): Flow<ChatStreamEvent> = callbackFlow {
        val payload = ChatRequestDto(
            messages = messages.takeLast(10),
            context = ChatContextDto(
                systemPrompt = systemPrompt,
                expression = currentExpression
            )
        )

        val bodyJson = json.encodeToString(payload)
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/chat")
            .header("Accept", "text/event-stream")
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val factory = EventSources.createFactory(httpClient)
        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                when (type) {
                    "delta" -> {
                        runCatching { json.decodeFromString<SseDeltaPayload>(data) }
                            .onSuccess { trySend(ChatStreamEvent.Delta(it.delta)) }
                    }
                    "done" -> {
                        val parsed = runCatching { json.decodeFromString<SseDonePayload>(data) }
                            .getOrNull()
                        if (parsed != null) {
                            trySend(ChatStreamEvent.Done(parsed.text, parsed.tts))
                        } else {
                            trySend(ChatStreamEvent.Error("Format respons done tidak valid"))
                        }
                        close()
                    }
                    "error" -> {
                        val msg = runCatching { json.decodeFromString<SseErrorPayload>(data).error }
                            .getOrElse { "Terjadi kesalahan saat streaming" }
                        trySend(ChatStreamEvent.Error(msg))
                        close()
                    }
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val rawBody = runCatching { response?.body?.string() }.getOrNull().orEmpty()
                val bodyErr = if (rawBody.isNotBlank()) {
                    runCatching { json.decodeFromString<SseErrorPayload>(rawBody).error }.getOrNull()
                } else null
                val statusInfo = response?.let { "HTTP ${it.code}" }
                val message = bodyErr ?: t?.message ?: statusInfo ?: "Gagal menghubungi server"
                trySend(ChatStreamEvent.Error(message))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = factory.newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }
}
