package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "kronik_entries")
data class KronikEntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val title: String = "",
    val content: String = "",
    val sortOrder: Int = 0,
    val moodStatus: String? = null,
    val moodSvgKey: String? = null,
    val habitsLogJson: String = "[]",
    val source: String = "manual",
    val generatedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val completedAt: String? = null,
    val archivedAt: String? = null,
    val isArchived: Boolean = false,
    val pinned: Boolean = false
)

@Serializable
data class KronikHabitLogDto(
    val habitName: String = "Habit",
    val category: String = "",
    val completedAt: String = ""
)

@Serializable
data class KronikMoodDto(
    val status: String,
    val svgKey: String
)

@Serializable
data class KronikBackupEntryDto(
    val id: String = "",
    val date: String = "",
    val title: String = "",
    val content: String = "",
    val order: Int = 0,
    val mood: KronikMoodDto? = null,
    val habits_log: List<KronikHabitLogDto> = emptyList(),
    val source: String = "manual",
    val generatedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val completedAt: String? = null,
    val archivedAt: String? = null,
    val isArchived: Boolean = false,
    val pinned: Boolean = false
)

@Serializable
data class KronikBackupWrapperDto(
    val version: Int = 2,
    val entries: List<KronikBackupEntryDto> = emptyList(),
    val exportedAt: String = ""
)
