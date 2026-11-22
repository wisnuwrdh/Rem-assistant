package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dailyos_habits")
data class DailyOsHabitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String = "General",
    val color: String = "#22C55E",
    val icon: String = "star",
    val activeDaysJson: String = "[]",
    val subTasksJson: String = "[]",
    val historyJson: String = "{}",
    val collapsed: Boolean = false,
    val archived: Boolean = false,
    val createdAt: String = "",
    val sortOrder: Int = 0
)

@Entity(tableName = "dailyos_todos")
data class DailyOsTodoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val priority: String = "urgent", // "urgent" | "soon" | "normal"
    val deadline: String = "",
    val done: Boolean = false,
    val completedAt: String? = null,
    val subtasksJson: String = "[]",
    val sortOrder: Int = 0
)
