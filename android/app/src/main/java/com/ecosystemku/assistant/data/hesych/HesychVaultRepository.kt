package com.ecosystemku.assistant.data.hesych

import com.ecosystemku.assistant.data.local.HesychCategory
import com.ecosystemku.assistant.data.local.HesychCustomFieldDto
import com.ecosystemku.assistant.data.local.HesychDao
import com.ecosystemku.assistant.data.local.HesychHistoryEntity
import com.ecosystemku.assistant.data.local.HesychItemEntity
import com.ecosystemku.assistant.data.local.HesychMetaEntity
import com.ecosystemku.assistant.data.local.HesychVaultItem
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface HesychUnlockResult {
    data class Unlocked(val dek: SecretKey, val upgradedToArgon2id: Boolean) : HesychUnlockResult
    data object NotInitialized : HesychUnlockResult
    data object WrongPassword : HesychUnlockResult
}

/**
 * Coordinates encrypted vault storage in Room ([HesychDao]) with [HesychCrypto],
 * supporting Argon2id v2 envelopes, transparent v1 PBKDF2 upgrades, and automatic
 * password history rotation (up to 10 entries per item).
 */
class HesychVaultRepository(
    private val dao: HesychDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        const val META_PW_ENVELOPE = "pw_envelope"
        const val META_VAULT_VERIFIER = "vault_ver"
        const val META_VAULT_VER_MAGIC = "vault_ver_magic"
        const val META_SECRET_LOCKS = "secret_locks"
        const val ROW_META_V6 = 6
        const val HIST_MAX = 10
        private const val DEFAULT_MAGIC = "hesych-vault-ok-v1"
    }

    suspend fun isVaultInitialized(): Boolean = withContext(Dispatchers.IO) {
        dao.getMeta(META_PW_ENVELOPE) != null
    }

    suspend fun setupMasterPassword(masterPassword: String): SecretKey = withContext(Dispatchers.IO) {
        val rawDek = HesychCrypto.generateRawDek()
        val envelope = HesychCrypto.createPasswordEnvelope(masterPassword, rawDek)
        val dek = HesychCrypto.importAesKey(rawDek)
        rawDek.fill(0)

        val verifier = HesychCrypto.buildVerifier(dek, DEFAULT_MAGIC)
        dao.upsertMetaAll(
            listOf(
                HesychMetaEntity(META_PW_ENVELOPE, HesychCrypto.encodeEnvelope(envelope)),
                HesychMetaEntity(META_VAULT_VER_MAGIC, DEFAULT_MAGIC),
                HesychMetaEntity(META_VAULT_VERIFIER, verifier)
            )
        )
        dek
    }

    suspend fun unlock(masterPassword: String): HesychUnlockResult = withContext(Dispatchers.IO) {
        val envMeta = dao.getMeta(META_PW_ENVELOPE) ?: return@withContext HesychUnlockResult.NotInitialized
        val envelope = HesychCrypto.decodeEnvelope(envMeta.value)
            ?: return@withContext HesychUnlockResult.NotInitialized

        val rawDek = runCatching {
            HesychCrypto.unwrapWithPassword(envelope, masterPassword)
        }.getOrNull() ?: return@withContext HesychUnlockResult.WrongPassword

        val dek = runCatching { HesychCrypto.importAesKey(rawDek) }.getOrNull()
        if (dek == null) {
            rawDek.fill(0)
            return@withContext HesychUnlockResult.WrongPassword
        }

        val ver = dao.getMeta(META_VAULT_VERIFIER)?.value
        val magic = dao.getMeta(META_VAULT_VER_MAGIC)?.value ?: DEFAULT_MAGIC
        if (!ver.isNullOrBlank() && !HesychCrypto.checkVerifier(dek, ver, magic)) {
            rawDek.fill(0)
            return@withContext HesychUnlockResult.WrongPassword
        }

        var upgraded = false
        if (HesychCrypto.needsUpgrade(envelope)) {
            val nextEnvelope = HesychCrypto.createPasswordEnvelope(masterPassword, rawDek)
            dao.upsertMeta(HesychMetaEntity(META_PW_ENVELOPE, HesychCrypto.encodeEnvelope(nextEnvelope)))
            upgraded = true
        }
        rawDek.fill(0)
        HesychUnlockResult.Unlocked(dek = dek, upgradedToArgon2id = upgraded)
    }

    suspend fun loadDecryptedItems(dek: SecretKey): List<HesychVaultItem> = withContext(Dispatchers.IO) {
        dao.getAllItems().mapNotNull { row ->
            runCatching { decryptRow(dek, row) }.getOrNull()
        }
    }

    fun decryptRow(dek: SecretKey, row: HesychItemEntity): HesychVaultItem {
        val title = HesychCrypto.decryptWith(dek, row.title)
        val username = HesychCrypto.decryptWith(dek, row.username)
        val notes = if (row.notes.isNotBlank()) HesychCrypto.decryptWith(dek, row.notes) else ""
        val customFields = runCatching {
            json.decodeFromString<List<HesychCustomFieldDto>>(row.customFieldsJson)
        }.getOrDefault(emptyList())

        val category = if (row.metaVersion == ROW_META_V6) {
            HesychCategory.fromKey(HesychCrypto.decryptMetaValue(dek, row.category, "other"))
        } else {
            HesychCategory.fromKey(row.category)
        }
        val tags: List<String> = if (row.metaVersion == ROW_META_V6) {
            HesychCrypto.decryptMetaValue<List<String>>(dek, row.tags, emptyList())
        } else {
            emptyList()
        }
        val breachStatus = if (row.metaVersion == ROW_META_V6) {
            HesychCrypto.decryptMetaValue<Int?>(dek, row.breachStatus, null)
        } else {
            row.breachStatus?.toIntOrNull()
        }
        val breachCheckedAt = if (row.metaVersion == ROW_META_V6) {
            HesychCrypto.decryptMetaValue<Long?>(dek, row.breachCheckedAt, null)
        } else {
            row.breachCheckedAt?.toLongOrNull()
        }

        return HesychVaultItem(
            id = row.id,
            title = title,
            username = username,
            password = row.password, // remains encrypted until explicitly revealed
            notes = notes,
            color = row.color,
            favorite = row.favorite,
            category = category,
            tags = tags,
            breachStatus = breachStatus,
            breachCheckedAt = breachCheckedAt,
            totpSecret = row.totpSecret, // remains encrypted until TOTP generated
            customFields = customFields,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt
        )
    }

    fun revealSecret(dek: SecretKey, ciphertext: String): String {
        if (ciphertext.isBlank()) return ""
        return HesychCrypto.decryptWith(dek, ciphertext)
    }

    fun encryptSecret(dek: SecretKey, plaintext: String): String {
        if (plaintext.isBlank()) return ""
        return HesychCrypto.encryptWith(dek, plaintext)
    }

    suspend fun saveItem(
        dek: SecretKey,
        id: Long = 0L,
        title: String,
        username: String,
        plainPassword: String,
        notes: String = "",
        color: Int = 0,
        favorite: Boolean = false,
        category: HesychCategory = HesychCategory.OTHER,
        tags: List<String> = emptyList(),
        plainTotpSecret: String = "",
        plainCustomFields: List<Triple<String, String, String>> = emptyList(), // (name, plainValue, type)
        breachStatus: Int? = null,
        breachCheckedAt: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = if (id > 0L) dao.getItemById(id) else null
        val oldPlainPassword = existing?.let {
            runCatching { HesychCrypto.decryptWith(dek, it.password) }.getOrNull()
        }

        val encCustomFields = plainCustomFields
            .filter { it.first.isNotBlank() || it.second.isNotBlank() }
            .map { (fieldName, fieldVal, fieldType) ->
                HesychCustomFieldDto(
                    name = fieldName.trim(),
                    value = if (fieldVal.isNotEmpty()) HesychCrypto.encryptWith(dek, fieldVal) else "",
                    type = if (fieldType == "password") "password" else "text"
                )
            }

        val entity = HesychItemEntity(
            id = existing?.id ?: 0L,
            title = HesychCrypto.encryptWith(dek, title.trim()),
            username = HesychCrypto.encryptWith(dek, username.trim()),
            password = HesychCrypto.encryptWith(dek, plainPassword),
            notes = if (notes.isNotBlank()) HesychCrypto.encryptWith(dek, notes) else "",
            color = color,
            favorite = favorite,
            totpSecret = if (plainTotpSecret.isNotBlank()) HesychCrypto.encryptWith(dek, plainTotpSecret.trim()) else "",
            customFieldsJson = json.encodeToString(encCustomFields),
            category = HesychCrypto.encryptMetaValue(dek, category.key),
            tags = HesychCrypto.encryptMetaValue(dek, tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()),
            breachStatus = HesychCrypto.encryptMetaValue(dek, breachStatus),
            breachCheckedAt = HesychCrypto.encryptMetaValue(dek, breachCheckedAt),
            metaVersion = ROW_META_V6,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        val savedId = dao.upsertItem(entity)
        val finalId = if (entity.id > 0L) entity.id else savedId

        if (existing != null && oldPlainPassword != null && oldPlainPassword != plainPassword) {
            dao.insertHistory(
                HesychHistoryEntity(
                    itemId = finalId,
                    encPassword = existing.password,
                    changedAt = now
                )
            )
            dao.trimHistoryForItem(finalId, HIST_MAX)
        }
        finalId
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteHistoryForItem(id)
        dao.deleteItemById(id)
    }
}
