package com.ecosystemku.assistant.data.hesych

import com.ecosystemku.assistant.data.local.HesychCustomFieldDto
import com.ecosystemku.assistant.data.local.HesychHistoryEntity
import com.ecosystemku.assistant.data.local.HesychItemEntity
import com.ecosystemku.assistant.data.local.HesychMetaEntity
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.longOrNull

@Serializable
data class HesychWebBackupRowDto(
    val id: Long = 0L,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val notes: String = "",
    val color: Int = 0,
    val favorite: Boolean = false,
    val totp_secret: String = "",
    val custom_fields: List<HesychCustomFieldDto> = emptyList(),
    val category: String? = null,
    val tags: JsonElement? = null,
    val breachStatus: JsonElement? = null,
    val breachCheckedAt: JsonElement? = null,
    val _metaV: Int = 6,
    val updatedAt: Long? = null,
    val createdAt: Long? = null
)

@Serializable
data class HesychWebBackupBundleDto(
    val type: String = "vault-encrypted-backup",
    val pwMode: String = "master", // "master" | "custom"
    val v: Int = 4,
    val salt: String? = null,
    val iv: String? = null,
    val dekEnv: String? = null,
    val data: JsonElement? = null
)

@Serializable
data class HesychCustomInnerPayloadDto(
    val v: Int = 3,
    val salt: String = "",
    val data: List<HesychWebBackupRowDto> = emptyList()
)

@Serializable
data class HesychEcosystemZipDto(
    val version: Int = 1,
    val items: List<HesychWebBackupRowDto> = emptyList(),
    val history: List<HesychHistoryZipDto> = emptyList(),
    val meta: Map<String, String> = emptyMap()
)

@Serializable
data class HesychHistoryZipDto(
    val hid: Long = 0L,
    val itemId: Long = 0L,
    val encPassword: String = "",
    val changedAt: Long = 0L
)

class HesychBackupNeedsPasswordException(
    val isMasterBackup: Boolean
) : IllegalStateException(
    if (isMasterBackup) "Masukkan Master Password dari file backup lama tersebut"
    else "Backup ini memerlukan password backup"
)

/**
 * Pure Kotlin backup codec compatible with Hesych Web (`lib/backup.ts`) for both
 * `master` (v3 PBKDF2 & v4 DEK envelope) and `custom` (v1) encrypted JSON bundles,
 * plus Ecosystem ZIP v3.
 */
object HesychBackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun exportMasterBackupJson(
        items: List<HesychItemEntity>,
        dekEnvelopeJson: String?
    ): String {
        val rows = items.map { it.toWebDto() }
        val bundle = HesychWebBackupBundleDto(
            type = "vault-encrypted-backup",
            pwMode = "master",
            v = 4,
            dekEnv = dekEnvelopeJson,
            data = json.encodeToJsonElement(
                kotlinx.serialization.builtins.ListSerializer(HesychWebBackupRowDto.serializer()),
                rows
            )
        )
        return json.encodeToString(bundle)
    }

    fun exportCustomBackupJson(
        items: List<HesychItemEntity>,
        vaultDek: SecretKey,
        backupPassword: String
    ): String {
        val saltBytes = HesychCrypto.randomBytes(HesychCrypto.SALT_BYTES)
        val saltB64 = HesychCrypto.toBase64(saltBytes)
        val backupKey = HesychCrypto.derivePbkdf2Key(backupPassword, saltBytes)

        val reencrypted = items.map { row ->
            reencryptRow(row.toWebDto(), fromKey = vaultDek, toKey = backupKey)
        }
        val innerJson = json.encodeToString(
            HesychCustomInnerPayloadDto(v = 3, salt = saltB64, data = reencrypted)
        )
        val iv = HesychCrypto.randomBytes(HesychCrypto.AES_IV_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, backupKey, GCMParameterSpec(HesychCrypto.GCM_TAG_BITS, iv))
        val ct = cipher.doFinal(innerJson.toByteArray(StandardCharsets.UTF_8))

        val bundle = HesychWebBackupBundleDto(
            type = "vault-encrypted-backup",
            pwMode = "custom",
            v = 1,
            salt = saltB64,
            iv = HesychCrypto.toBase64(iv),
            data = JsonPrimitive(HesychCrypto.toBase64(ct))
        )
        return json.encodeToString(bundle)
    }

    fun parseImportRows(
        rawJson: String,
        targetDek: SecretKey,
        customPassword: String? = null
    ): List<HesychItemEntity> {
        val bundle = json.decodeFromString<HesychWebBackupBundleDto>(rawJson)
        require(bundle.type == "vault-encrypted-backup") { "Bukan file backup Hesych yang valid" }

        if (bundle.pwMode == "custom") {
            val pw = customPassword?.takeIf { it.isNotEmpty() }
                ?: throw HesychBackupNeedsPasswordException(isMasterBackup = false)
            val salt = HesychCrypto.fromBase64(bundle.salt ?: error("Salt backup tidak ditemukan"))
            val iv = HesychCrypto.fromBase64(bundle.iv ?: error("IV backup tidak ditemukan"))
            val ctB64 = (bundle.data as? JsonPrimitive)?.contentOrNull ?: error("Data backup tidak valid")
            val ct = HesychCrypto.fromBase64(ctB64)
            val backupKey = HesychCrypto.derivePbkdf2Key(pw, salt)

            val plainPayload = runCatching {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, backupKey, GCMParameterSpec(HesychCrypto.GCM_TAG_BITS, iv))
                String(cipher.doFinal(ct), StandardCharsets.UTF_8)
            }.getOrElse {
                throw IllegalArgumentException("Password backup salah atau file rusak")
            }

            val inner = json.decodeFromString<HesychCustomInnerPayloadDto>(plainPayload)
            return inner.data.map { dto ->
                reencryptRow(dto, fromKey = backupKey, toKey = targetDek).toEntity()
            }
        }

        val dataArr = bundle.data?.jsonArray ?: error("Format data backup master tidak valid")
        val rows = dataArr.map { json.decodeFromJsonElement(HesychWebBackupRowDto.serializer(), it) }
        if (rows.isEmpty()) return emptyList()

        // Fast path: backup created with the exact same vault DEK
        val matchesCurrentDek = runCatching {
            HesychCrypto.decryptWith(targetDek, rows.first().title)
            true
        }.getOrDefault(false)
        if (matchesCurrentDek) {
            return rows.map { it.toEntity() }
        }

        // Cross-device / Hesych Web v3 (PBKDF2 salt) or v4 (dekEnv) master backup:
        val hasRecoveryMaterial = !bundle.dekEnv.isNullOrBlank() || !bundle.salt.isNullOrBlank()
        if (!hasRecoveryMaterial) {
            throw IllegalArgumentException(
                "Backup dibuat dengan kunci vault berbeda dan tidak memiliki salt/envelope."
            )
        }
        val pw = customPassword?.takeIf { it.isNotEmpty() }
            ?: throw HesychBackupNeedsPasswordException(isMasterBackup = true)

        val sourceKey = runCatching {
            if (!bundle.dekEnv.isNullOrBlank()) {
                val env = HesychCrypto.decodeEnvelope(bundle.dekEnv)
                    ?: error("Envelope DEK tidak valid")
                val rawDek = HesychCrypto.unwrapWithPassword(env, pw)
                val key = HesychCrypto.importAesKey(rawDek)
                rawDek.fill(0)
                key
            } else {
                val saltBytes = HesychCrypto.fromBase64(bundle.salt!!)
                HesychCrypto.derivePbkdf2Key(pw, saltBytes)
            }
        }.getOrElse {
            throw IllegalArgumentException("Master password backup salah atau file rusak")
        }

        runCatching {
            HesychCrypto.decryptWith(sourceKey, rows.first().title)
        }.getOrElse {
            throw IllegalArgumentException("Master password backup salah atau file rusak")
        }

        return rows.map { dto ->
            reencryptRow(dto, fromKey = sourceKey, toKey = targetDek).toEntity()
        }
    }

    fun bundleNeedsCustomPassword(rawJson: String): Boolean {
        val bundle = runCatching { json.decodeFromString<HesychWebBackupBundleDto>(rawJson) }.getOrNull()
        return bundle?.pwMode == "custom"
    }

    fun encodeEcosystemZipDto(
        items: List<HesychItemEntity>,
        history: List<HesychHistoryEntity>,
        meta: List<HesychMetaEntity>
    ): String {
        val dto = HesychEcosystemZipDto(
            version = 1,
            items = items.map { it.toWebDto() },
            history = history.map { HesychHistoryZipDto(it.hid, it.itemId, it.encPassword, it.changedAt) },
            meta = meta.associate { it.key to it.value }
        )
        return json.encodeToString(dto)
    }

    fun decodeEcosystemZipDto(rawJson: String): HesychEcosystemZipDto? = runCatching {
        json.decodeFromString<HesychEcosystemZipDto>(rawJson)
    }.getOrNull()

    fun HesychItemEntity.toWebDto(): HesychWebBackupRowDto {
        val parsedFields = runCatching {
            json.decodeFromString<List<HesychCustomFieldDto>>(customFieldsJson)
        }.getOrDefault(emptyList())
        return HesychWebBackupRowDto(
            id = id,
            title = title,
            username = username,
            password = password,
            notes = notes,
            color = color,
            favorite = favorite,
            totp_secret = totpSecret,
            custom_fields = parsedFields,
            category = category,
            tags = tags?.let { JsonPrimitive(it) },
            breachStatus = breachStatus?.let { JsonPrimitive(it) },
            breachCheckedAt = breachCheckedAt?.let { JsonPrimitive(it) },
            _metaV = metaVersion,
            updatedAt = updatedAt,
            createdAt = createdAt
        )
    }

    fun HesychWebBackupRowDto.toEntity(): HesychItemEntity {
        val now = System.currentTimeMillis()
        return HesychItemEntity(
            id = id,
            title = title,
            username = username,
            password = password,
            notes = notes,
            color = color,
            favorite = favorite,
            totpSecret = totp_secret,
            customFieldsJson = json.encodeToString(custom_fields),
            category = category,
            tags = (tags as? JsonPrimitive)?.contentOrNull,
            breachStatus = (breachStatus as? JsonPrimitive)?.contentOrNull,
            breachCheckedAt = (breachCheckedAt as? JsonPrimitive)?.contentOrNull,
            metaVersion = _metaV,
            createdAt = createdAt ?: now,
            updatedAt = updatedAt ?: now
        )
    }

    private fun reencryptRow(
        row: HesychWebBackupRowDto,
        fromKey: SecretKey,
        toKey: SecretKey
    ): HesychWebBackupRowDto {
        fun reenc(ct: String): String {
            if (ct.isBlank()) return ""
            val pt = HesychCrypto.decryptWith(fromKey, ct)
            return HesychCrypto.encryptWith(toKey, pt)
        }

        val plainCat = if (row._metaV == 6) {
            HesychCrypto.decryptMetaValue(fromKey, row.category, "other")
        } else {
            row.category ?: "other"
        }
        val tagsPrim = (row.tags as? JsonPrimitive)?.contentOrNull
        val plainTags = if (row._metaV == 6) {
            HesychCrypto.decryptMetaValue<List<String>>(fromKey, tagsPrim, emptyList())
        } else {
            runCatching { row.tags?.jsonArray?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } }.getOrNull() ?: emptyList()
        }
        val breachStatusPrim = (row.breachStatus as? JsonPrimitive)?.contentOrNull
        val plainBreachStatus = if (row._metaV == 6) {
            HesychCrypto.decryptMetaValue<Int?>(fromKey, breachStatusPrim, null)
        } else {
            breachStatusPrim?.toIntOrNull()
        }
        val breachAtPrim = row.breachCheckedAt as? JsonPrimitive
        val plainBreachAt = if (row._metaV == 6) {
            HesychCrypto.decryptMetaValue<Long?>(fromKey, breachAtPrim?.contentOrNull, null)
        } else {
            breachAtPrim?.longOrNull
        }

        val newFields = row.custom_fields.map { cf ->
            cf.copy(value = if (cf.value.isNotBlank()) reenc(cf.value) else "")
        }

        return row.copy(
            title = reenc(row.title),
            username = reenc(row.username),
            password = reenc(row.password),
            notes = reenc(row.notes),
            totp_secret = reenc(row.totp_secret),
            custom_fields = newFields,
            category = HesychCrypto.encryptMetaValue(toKey, plainCat),
            tags = HesychCrypto.encryptMetaValue(toKey, plainTags)?.let { JsonPrimitive(it) },
            breachStatus = HesychCrypto.encryptMetaValue(toKey, plainBreachStatus)?.let { JsonPrimitive(it) },
            breachCheckedAt = HesychCrypto.encryptMetaValue(toKey, plainBreachAt)?.let { JsonPrimitive(it) },
            _metaV = 6
        )
    }
}
