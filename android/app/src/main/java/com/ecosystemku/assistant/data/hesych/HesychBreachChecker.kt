package com.ecosystemku.assistant.data.hesych

import com.ecosystemku.assistant.data.local.HesychDao
import com.ecosystemku.assistant.data.local.HesychVaultItem
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class HesychBreachSummary(
    val checked: Int,
    val breached: Int,
    val safe: Int,
    val failed: Int
)

/**
 * Have I Been Pwned (HIBP) k-anonymity range checker.
 * Only the first 5 hex characters of the SHA-1 hash ever leave the device.
 */
object HesychBreachChecker {
    private const val HIBP_API = "https://api.pwnedpasswords.com/range/"

    fun sha1Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { b -> String.format(Locale.US, "%02X", b) }
    }

    fun parseRangeResponse(body: String, suffix: String): Int {
        val target = suffix.uppercase(Locale.US)
        for (line in body.lineSequence()) {
            val idx = line.indexOf(':')
            if (idx <= 0) continue
            if (line.substring(0, idx).trim().uppercase(Locale.US) == target) {
                return line.substring(idx + 1).trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
            }
        }
        return 0
    }

    suspend fun checkPasswordBreach(
        httpClient: OkHttpClient,
        plainPassword: String
    ): Int = withContext(Dispatchers.IO) {
        val hash = sha1Hex(plainPassword)
        val prefix = hash.substring(0, 5)
        val suffix = hash.substring(5)
        val req = Request.Builder()
            .url("$HIBP_API$prefix")
            .header("Add-Padding", "true")
            .get()
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("HIBP HTTP ${resp.code}")
            }
            val body = resp.body?.string().orEmpty()
            parseRangeResponse(body, suffix)
        }
    }

    suspend fun checkAllItems(
        httpClient: OkHttpClient,
        dao: HesychDao,
        dek: SecretKey,
        items: List<HesychVaultItem>,
        onProgress: (checkedCount: Int, total: Int) -> Unit = { _, _ -> }
    ): HesychBreachSummary {
        var checked = 0
        var breached = 0
        var safe = 0
        var failed = 0

        items.forEachIndexed { index, item ->
            runCatching {
                val plainPw = HesychCrypto.decryptWith(dek, item.password)
                val count = checkPasswordBreach(httpClient, plainPw)
                val status = if (count > 0) 2 else 1
                val now = System.currentTimeMillis()
                val row = withContext(Dispatchers.IO) { dao.getItemById(item.id) }
                if (row != null) {
                    val updated = row.copy(
                        breachStatus = HesychCrypto.encryptMetaValue(dek, status),
                        breachCheckedAt = HesychCrypto.encryptMetaValue(dek, now)
                    )
                    withContext(Dispatchers.IO) { dao.upsertItem(updated) }
                }
                checked++
                if (count > 0) breached++ else safe++
            }.onFailure {
                failed++
            }
            onProgress(index + 1, items.size)
        }

        return HesychBreachSummary(
            checked = checked,
            breached = breached,
            safe = safe,
            failed = failed
        )
    }
}
