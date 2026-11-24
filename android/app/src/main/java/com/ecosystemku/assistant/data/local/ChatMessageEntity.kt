package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ChatChoicesData(
    val mode: String = "single", // "single" | "multi"
    val items: List<String> = emptyList(),
    val selected: List<String> = emptyList()
)

object ChatChoicesCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(data: ChatChoicesData?): String? {
        if (data == null || data.items.isEmpty()) return null
        return runCatching { json.encodeToString(data) }.getOrNull()
    }

    fun decode(raw: String?): ChatChoicesData? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<ChatChoicesData>(raw) }
            .getOrNull()
            ?.takeIf { it.items.isNotEmpty() }
    }
}

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Obrolan Baru",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val rollingSummary: String? = null,
    val summarizedCount: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user" | "assistant"
    val content: String,
    val expressionTag: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val choicesJson: String? = null,
    val sessionId: Long = 1L
)
