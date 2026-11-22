package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "ideku_ideas")
data class IdekuIdeaEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val notes: String = "",
    val category: String = "Dev",
    val jalur: String = "",
    val nextStep: String = "",
    val priority: String = "Nanti", // Penting | Nanti | Kapan-kapan
    val status: String = "Belum", // Belum | Proses | Selesai | Parkir
    val type: String = "Idea", // Idea | Wishlist
    val price: Long = 0L,
    val image: String = "bag",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class IdekuIdeaDto(
    val id: Long = 0L,
    val title: String = "",
    val notes: String = "",
    val category: String = "Dev",
    val jalur: String = "",
    val next: String = "",
    val priority: String = "Nanti",
    val status: String = "Belum",
    val type: String = "Idea",
    val price: Long = 0L,
    val image: String = "bag"
)

@Serializable
data class IdekuBackupWrapperDto(
    val ideas: List<IdekuIdeaDto> = emptyList()
)
