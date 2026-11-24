package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rem_reminders")
data class RemReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val triggerAtMs: Long,
    val fired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
