package com.ecosystemku.assistant.ui.apps.linkku

import android.graphics.Bitmap
import android.util.Base64
import com.ecosystemku.assistant.data.local.LinkkuBackupCatDto
import com.ecosystemku.assistant.data.local.LinkkuBackupLinkDto
import com.ecosystemku.assistant.data.local.LinkkuBackupWrapperDto
import com.ecosystemku.assistant.data.local.LinkkuCategoryEntity
import com.ecosystemku.assistant.data.local.LinkkuEncryptedPrivatePayloadDto
import com.ecosystemku.assistant.data.local.LinkkuLinkEntity
import com.ecosystemku.assistant.data.network.LinkkuMetaResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class LinkkuUiState(
    val links: List<LinkkuLinkEntity> = emptyList(),
    val categories: List<LinkkuCategoryEntity> = emptyList(),
    val extraTagsNormal: List<String> = emptyList(),
    val extraTagsPriv: List<String> = emptyList(),
    val hasPin: Boolean = false
)

sealed class LinkkuImportResult {
    data class Success(val importedCount: Int) : LinkkuImportResult()
    data class NeedsPin(val rawJson: String) : LinkkuImportResult()
    data class Error(val message: String) : LinkkuImportResult()
}

data class LinkkuActions(
    val onSaveLink: suspend (
        editId: String?,
        url: String,
        title: String,
        note: String,
        catId: String,
        tags: List<String>,
        isPrivate: Boolean
    ) -> Unit,
    val onDeleteLink: (id: String) -> Unit = {},
    val onAddCategory: suspend (name: String, isPrivate: Boolean) -> Unit = { _, _ -> },
    val onDeleteCategory: (id: String, isPrivate: Boolean) -> Unit = { _, _ -> },
    val onAddExtraTag: (tag: String, isPrivate: Boolean) -> Unit = { _, _ -> },
    val onDeleteTagGlobally: (tag: String, isPrivate: Boolean) -> Unit = { _, _ -> },
    val onFetchMeta: suspend (url: String) -> LinkkuMetaResult = { LinkkuMetaResult() },
    val onLoadBitmap: suspend (url: String) -> Bitmap? = { null },
    val onVerifyPin: (pin: String) -> Boolean = { false },
    val onSetPin: (newPin: String) -> Unit = {},
    val onExportJson: suspend () -> String = { "{}" },
    val onImportJson: suspend (raw: String, rawPin: String?) -> LinkkuImportResult = { _, _ ->
        LinkkuImportResult.Error("Tidak tersedia")
    },
    val onResetLinkku: () -> Unit = {}
)

object LinkkuCrypto {
    private const val SALT = "linkku-salt-v1"
    private const val ITERATIONS = 100_000

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun deriveAesKey(passphrase: String): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            passphrase.toCharArray(),
            SALT.toByteArray(Charsets.UTF_8),
            ITERATIONS,
            256
        )
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    fun encryptPrivatePayload(plainJson: String, pinHash: String): String {
        val key = deriveAesKey(pinHash)
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptPrivatePayload(b64: String, pinHash: String): String {
        val key = deriveAesKey(pinHash)
        val combined = Base64.decode(b64, Base64.DEFAULT)
        require(combined.size > 12) { "Ciphertext invalid" }
        val iv = combined.copyOfRange(0, 12)
        val data = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }
}

object LinkkuHelpers {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    val DEFAULT_CATEGORIES = listOf(
        LinkkuCategoryEntity(id = "yt", name = "YouTube", isPrivate = false, sortOrder = 0),
        LinkkuCategoryEntity(id = "tools", name = "Tools", isPrivate = false, sortOrder = 1),
        LinkkuCategoryEntity(id = "reading", name = "Reading", isPrivate = false, sortOrder = 2),
        LinkkuCategoryEntity(id = "project", name = "Project", isPrivate = false, sortOrder = 3)
    )

    fun generateId(): String {
        val rand = (100_000..999_999).random().toString(36)
        val ts = System.currentTimeMillis().toString(36)
        return "$rand$ts"
    }

    fun extractDomain(rawUrl: String): String {
        return runCatching {
            val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
            val host = URI(normalized).host ?: return rawUrl
            host.removePrefix("www.")
        }.getOrDefault(rawUrl)
    }

    fun youtubeThumb(url: String): String {
        val m = Regex("""(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/shorts/)([a-zA-Z0-9_-]{11})""").find(url)
        val id = m?.groupValues?.getOrNull(1) ?: return ""
        return "https://img.youtube.com/vi/$id/mqdefault.jpg"
    }

    fun domainHue(domain: String): Float {
        var hue = 0
        for (ch in domain) {
            hue = (hue * 31 + ch.code) % 360
        }
        return hue.toFloat().coerceIn(0f, 359f)
    }

    fun formatRelativeTime(ts: Long): String {
        val diff = (System.currentTimeMillis() - ts).coerceAtLeast(0L)
        return when {
            diff < 60_000L -> "Baru"
            diff < 3_600_000L -> "${diff / 60_000L} mnt"
            diff < 86_400_000L -> "${diff / 3_600_000L} jam"
            diff < 604_800_000L -> "${diff / 86_400_000L} hari"
            else -> SimpleDateFormat("d MMM", Locale("in", "ID")).format(Date(ts))
        }
    }

    fun parseTags(tagsJson: String): List<String> {
        if (tagsJson.isBlank() || tagsJson == "[]") return emptyList()
        return runCatching {
            json.decodeFromString<List<String>>(tagsJson)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }.getOrDefault(emptyList())
    }

    fun encodeTags(tags: List<String>): String {
        return runCatching {
            json.encodeToString(tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct())
        }.getOrDefault("[]")
    }

    fun buildExportJson(
        links: List<LinkkuLinkEntity>,
        categories: List<LinkkuCategoryEntity>,
        pinHash: String?
    ): String {
        val pubLinks = links.filter { !it.isPrivate }.map { it.toBackupDto() }
        val privLinks = links.filter { it.isPrivate }.map { it.toBackupDto() }
        val pubCats = categories.filter { !it.isPrivate }.map { LinkkuBackupCatDto(id = it.id, n = it.name) }
        val privCats = categories.filter { it.isPrivate }.map { LinkkuBackupCatDto(id = it.id, n = it.name) }

        return if (privLinks.isNotEmpty() && !pinHash.isNullOrBlank()) {
            val privJson = json.encodeToString(LinkkuEncryptedPrivatePayloadDto(links = privLinks, cats = privCats))
            val encrypted = LinkkuCrypto.encryptPrivatePayload(privJson, pinHash)
            json.encodeToString(
                LinkkuBackupWrapperDto(
                    L = pubLinks,
                    C = pubCats,
                    ep = encrypted,
                    ts = System.currentTimeMillis(),
                    v = 5
                )
            )
        } else {
            json.encodeToString(
                LinkkuBackupWrapperDto(
                    L = pubLinks,
                    C = pubCats,
                    PC = privCats,
                    ts = System.currentTimeMillis(),
                    v = 5
                )
            )
        }
    }

    fun LinkkuLinkEntity.toBackupDto() = LinkkuBackupLinkDto(
        id = id,
        url = url,
        ti = title,
        note = note,
        cat = cat,
        pcat = pcat,
        tags = parseTags(tagsJson),
        p = isPrivate,
        thumb = thumb,
        ts = ts
    )

    fun LinkkuBackupLinkDto.toEntity() = LinkkuLinkEntity(
        id = id.ifBlank { generateId() },
        url = url,
        title = ti,
        note = note,
        cat = if (p) "" else cat,
        pcat = if (p) pcat else "",
        tagsJson = encodeTags(tags),
        isPrivate = p,
        thumb = thumb.ifBlank { youtubeThumb(url) },
        ts = if (ts > 0L) ts else System.currentTimeMillis()
    )
}
