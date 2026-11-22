package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_summaries")
data class ConversationSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val summaryText: String,
    val turnCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)
