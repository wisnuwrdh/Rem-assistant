package com.ecosystemku.assistant.data.hesych

import com.ecosystemku.assistant.data.local.HesychVaultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HesychHealthTest {

    @Test
    fun emptyVaultScores100Great() {
        val dek = HesychCrypto.importAesKey(ByteArray(32) { 1 })
        val report = HesychHealth.scanVaultHealth(emptyList(), dek)
        assertEquals(100, report.score)
        assertEquals(HesychHealthLevel.GREAT, report.level)
    }

    @Test
    fun scanDetectsBreachedWeakOldAndDuplicatePasswords() {
        val dek = HesychCrypto.importAesKey(ByteArray(32) { 7 })
        val now = 1_800_000_000_000L
        val oldStamp = now - (100L * 86_400_000L)

        val items = listOf(
            HesychVaultItem(
                id = 1L,
                title = "Bank",
                username = "u1",
                password = HesychCrypto.encryptWith(dek, "Str0ng!Pass#99"),
                breachStatus = 1,
                createdAt = now,
                updatedAt = now
            ),
            HesychVaultItem(
                id = 2L,
                title = "Social A",
                username = "u2",
                password = HesychCrypto.encryptWith(dek, "weak123"),
                breachStatus = 2,
                createdAt = oldStamp,
                updatedAt = oldStamp
            ),
            HesychVaultItem(
                id = 3L,
                title = "Social B",
                username = "u3",
                password = HesychCrypto.encryptWith(dek, "weak123"),
                breachStatus = 1,
                createdAt = now,
                updatedAt = now
            )
        )

        val report = HesychHealth.scanVaultHealth(items, dek, now)
        assertEquals(3, report.total)
        assertEquals(3, report.checked)
        assertEquals(1, report.breached)
        assertEquals(2, report.weak)
        assertEquals(1, report.oldCount)
        assertEquals(1, report.dupExtra)
        assertEquals(listOf(2L), report.breachedIds)
        assertEquals(listOf(2L, 3L), report.weakIds)
        assertEquals(listOf(2L), report.oldIds)
        assertEquals(listOf(2L, 3L), report.dupIds)
        // Penalty: 20 (breach) + 20 (2 weak) + 5 (old) + 10 (1 dupExtra) = 55 -> score 45
        assertEquals(45, report.score)
        assertEquals(HesychHealthLevel.POOR, report.level)
        assertTrue(report.score < 50)
    }
}
