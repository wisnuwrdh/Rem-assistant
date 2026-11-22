package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "linkku_links")
data class LinkkuLinkEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String = "",
    val note: String = "",
    val cat: String = "",
    val pcat: String = "",
    val tagsJson: String = "[]",
    val isPrivate: Boolean = false,
    val thumb: String = "",
    val ts: Long = System.currentTimeMillis()
)

@Entity(tableName = "linkku_categories")
data class LinkkuCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isPrivate: Boolean = false,
    val sortOrder: Int = 0
)

@Serializable
data class LinkkuBackupLinkDto(
    val id: String = "",
    val url: String = "",
    val ti: String = "",
    val note: String = "",
    val cat: String = "",
    val pcat: String = "",
    val tags: List<String> = emptyList(),
    val p: Boolean = false,
    val thumb: String = "",
    val ts: Long = 0L
)

@Serializable
data class LinkkuBackupCatDto(
    val id: String = "",
    val n: String = ""
)

@Serializable
data class LinkkuEncryptedPrivatePayloadDto(
    val links: List<LinkkuBackupLinkDto> = emptyList(),
    val cats: List<LinkkuBackupCatDto> = emptyList()
)

@Serializable
data class LinkkuBackupWrapperDto(
    val L: List<LinkkuBackupLinkDto> = emptyList(),
    val links: List<LinkkuBackupLinkDto> = emptyList(),
    val C: List<LinkkuBackupCatDto> = emptyList(),
    val cats: List<LinkkuBackupCatDto> = emptyList(),
    val PC: List<LinkkuBackupCatDto> = emptyList(),
    val ep: String? = null,
    val ts: Long = 0L,
    val v: Int = 5
)
