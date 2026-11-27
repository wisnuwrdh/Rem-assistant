package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "hesych_items")
data class HesychItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val username: String,
    val password: String,
    val notes: String = "",
    val color: Int = 0,
    val favorite: Boolean = false,
    val totpSecret: String = "",
    val customFieldsJson: String = "[]",
    val category: String? = null,
    val tags: String? = null,
    val breachStatus: String? = null,
    val breachCheckedAt: String? = null,
    val metaVersion: Int = 6,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "hesych_history",
    indices = [Index(value = ["itemId"])]
)
data class HesychHistoryEntity(
    @PrimaryKey(autoGenerate = true) val hid: Long = 0L,
    val itemId: Long,
    val encPassword: String,
    val changedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hesych_meta")
data class HesychMetaEntity(
    @PrimaryKey val key: String,
    val value: String
)

enum class HesychCategory(val key: String, val label: String) {
    SOCIAL("social", "Sosial"),
    FINANCE("finance", "Keuangan"),
    EMAIL("email", "Email"),
    WORK("work", "Kerja"),
    SHOPPING("shopping", "Belanja"),
    GAMING("gaming", "Game"),
    OTHER("other", "Lainnya");

    companion object {
        fun fromKey(raw: String?): HesychCategory {
            if (raw.isNullOrBlank()) return OTHER
            return entries.firstOrNull { it.key.equals(raw.trim(), ignoreCase = true) } ?: OTHER
        }
    }
}

@Serializable
data class HesychCustomFieldDto(
    val name: String = "",
    /** Base64 AES-256-GCM ciphertext; empty string when blank. */
    val value: String = "",
    val type: String = "text" // "text" | "password"
)

@Serializable
data class HesychArgon2ParamsDto(
    val name: String = "argon2id",
    val memoryKiB: Int = 65536,
    val iterations: Int = 3,
    val parallelism: Int = 1
)

@Serializable
data class HesychPwEnvelopeDto(
    val v: Int = 2,
    val salt: String = "",
    val wrap: String = "",
    val kdf: HesychArgon2ParamsDto? = null
)

/**
 * In-memory decrypted vault item. Sensitive secrets ([password], [totpSecret],
 * and [customFields] values) stay as AES-256-GCM ciphertexts until revealed/copied.
 */
data class HesychVaultItem(
    val id: Long = 0L,
    val title: String,
    val username: String,
    val password: String,
    val notes: String = "",
    val color: Int = 0,
    val favorite: Boolean = false,
    val category: HesychCategory = HesychCategory.OTHER,
    val tags: List<String> = emptyList(),
    val breachStatus: Int? = null,
    val breachCheckedAt: Long? = null,
    val totpSecret: String = "",
    val customFields: List<HesychCustomFieldDto> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
