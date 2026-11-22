package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user" | "assistant"
    val content: String,
    val expressionTag: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
