package com.ecosystemku.assistant.data.hesych

import com.ecosystemku.assistant.data.local.HesychVaultItem
import javax.crypto.SecretKey

enum class HesychHealthLevel(val label: String) {
    GREAT("Sangat Baik"),
    GOOD("Baik"),
    FAIR("Cukup"),
    POOR("Perlu Perbaikan")
}

data class HesychHealthReport(
    val score: Int,
    val level: HesychHealthLevel,
    val total: Int,
    val checked: Int,
    val breached: Int,
    val weak: Int,
    val oldCount: Int,
    val dupExtra: Int,
    val breachedIds: List<Long>,
    val weakIds: List<Long>,
    val oldIds: List<Long>,
    val dupIds: List<Long>
)

/**
 * Pure Kotlin Vault Health scanner matching Hesych Web (`lib/health.ts`).
 */
object HesychHealth {
    const val EXPIRY_DAYS = 90L
    const val OLD_PASSWORD_MS = EXPIRY_DAYS * 86_400_000L

    fun levelForScore(score: Int): HesychHealthLevel = when {
        score >= 85 -> HesychHealthLevel.GREAT
        score >= 70 -> HesychHealthLevel.GOOD
        score >= 50 -> HesychHealthLevel.FAIR
        else -> HesychHealthLevel.POOR
    }

    fun isPasswordOld(item: HesychVaultItem, nowMs: Long = System.currentTimeMillis()): Boolean {
        val stamp = if (item.updatedAt > 0L) item.updatedAt else item.createdAt
        if (stamp <= 0L) return false
        return (nowMs - stamp) > OLD_PASSWORD_MS
    }

    fun scanVaultHealth(
        items: List<HesychVaultItem>,
        dek: SecretKey,
        nowMs: Long = System.currentTimeMillis()
    ): HesychHealthReport {
        if (items.isEmpty()) {
            return HesychHealthReport(
                score = 100,
                level = HesychHealthLevel.GREAT,
                total = 0,
                checked = 0,
                breached = 0,
                weak = 0,
                oldCount = 0,
                dupExtra = 0,
                breachedIds = emptyList(),
                weakIds = emptyList(),
                oldIds = emptyList(),
                dupIds = emptyList()
            )
        }

        var checked = 0
        var breached = 0
        var weak = 0
        var oldCount = 0
        var failed = 0

        val breachedIds = mutableListOf<Long>()
        val weakIds = mutableListOf<Long>()
        val oldIds = mutableListOf<Long>()
        val byPassword = linkedMapOf<String, MutableList<Long>>()

        for (item in items) {
            val plainPw = runCatching { HesychCrypto.decryptWith(dek, item.password) }.getOrNull()
            if (plainPw == null) {
                failed++
                continue
            }

            if (item.breachStatus == 2) {
                breached++
                breachedIds += item.id
            }
            if (item.breachStatus != null) {
                checked++
            }
            if (HesychPasswordGenerator.scorePassword(plainPw) <= 2) {
                weak++
                weakIds += item.id
            }
            if (isPasswordOld(item, nowMs)) {
                oldCount++
                oldIds += item.id
            }
            byPassword.getOrPut(plainPw) { mutableListOf() }.add(item.id)
        }

        var dupExtra = 0
        val dupIds = mutableListOf<Long>()
        for (ids in byPassword.values) {
            if (ids.size > 1) {
                dupExtra += ids.size - 1
                dupIds.addAll(ids)
            }
        }

        var penalty = 0
        penalty += minOf(breached * 20, 70)
        penalty += minOf(weak * 10, 30)
        penalty += minOf(oldCount * 5, 20)
        penalty += minOf(failed, 10)
        val unchecked = (items.size - checked - failed).coerceAtLeast(0)
        penalty += minOf(unchecked, 10)
        penalty += minOf(dupExtra * 10, 30)

        val finalScore = (100 - penalty).coerceIn(0, 100)
        return HesychHealthReport(
            score = finalScore,
            level = levelForScore(finalScore),
            total = items.size,
            checked = checked,
            breached = breached,
            weak = weak,
            oldCount = oldCount,
            dupExtra = dupExtra,
            breachedIds = breachedIds,
            weakIds = weakIds,
            oldIds = oldIds,
            dupIds = dupIds
        )
    }
}
