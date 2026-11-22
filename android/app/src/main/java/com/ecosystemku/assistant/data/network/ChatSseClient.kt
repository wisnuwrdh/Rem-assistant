package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        .build()

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
                val statusInfo = response?.let { "HTTP ${it.code}" }
                val message = t?.message ?: statusInfo ?: "Gagal menghubungi server"
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
