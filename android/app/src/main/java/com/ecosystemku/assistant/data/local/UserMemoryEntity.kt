package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_memories",
    indices = [Index(value = ["factKey"], unique = true)]
)
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "identitas" | "preferensi" | "kebiasaan" | "tujuan" | "catatan"
    val factKey: String,
    val factValue: String,
    val confidence: Float = 0.8f,
    val sourceMessageId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
