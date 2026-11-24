package com.ecosystemku.assistant.data.kg

import com.ecosystemku.assistant.data.local.CuciCategoryEntity
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.IdekuIdeaEntity
import com.ecosystemku.assistant.data.local.KronikEntryEntity
import com.ecosystemku.assistant.data.local.LinkkuCategoryEntity
import com.ecosystemku.assistant.data.local.LinkkuLinkEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BehavioralSignalSyncTest {

    @Test
    fun extractsCrossModuleSignalsAndExcludesPrivateLinks() {
        val habits = listOf(
            DailyOsHabitEntity(
                id = "h1",
                name = "Olahraga Pagi",
                historyJson = """
                    {
                      "2026-11-24":[{"id":"main"}],
                      "2026-11-23":[{"id":"main"}],
                      "2026-11-22":[{"id":"main"}]
                    }
                """.trimIndent()
            ),
            DailyOsHabitEntity(
                id = "h2",
                name = "Membaca Buku",
                historyJson = "{}"
            )
        )
        val journals = listOf(
            KronikEntryEntity(id = "k1", date = "2026-11-20", moodStatus = "Lelah"),
            KronikEntryEntity(id = "k2", date = "2026-11-21", moodStatus = "Lelah")
        )
        val ideas = listOf(
            IdekuIdeaEntity(id = 1L, title = "Robot Kembaran Digital", status = "Proses", type = "Idea")
        )
        val cats = listOf(CuciCategoryEntity(id = 10L, name = "Makan"))
        val trxs = listOf(
            CuciTransactionEntity(id = 1L, bookId = 1L, title = "Kopi", amount = 50_000L, direction = "out", categoryId = 10L, accountId = 1L, date = "2026-11-24")
        )
        val linkCats = listOf(
            LinkkuCategoryEntity(id = "c_ai", name = "AI & Robotik", isPrivate = false),
            LinkkuCategoryEntity(id = "c_priv", name = "Rahasia Privat", isPrivate = true)
        )
        val links = listOf(
            LinkkuLinkEntity(id = "l1", url = "https://a.com", cat = "c_ai", isPrivate = false),
            LinkkuLinkEntity(id = "l2", url = "https://b.com", cat = "c_ai", isPrivate = false),
            LinkkuLinkEntity(id = "l3", url = "https://priv1.com", cat = "c_priv", isPrivate = true),
            LinkkuLinkEntity(id = "l4", url = "https://priv2.com", cat = "c_priv", isPrivate = true)
        )

        val triples = BehavioralSignalExtractor.extractTriples(
            profileName = "Wisnu",
            habits = habits,
            kronikEntries = journals,
            ideas = ideas,
            transactions = trxs,
            cuciCategories = cats,
            links = links,
            linkkuCategories = linkCats,
            todayStr = "2026-11-24"
        )

        assertTrue(triples.any { it.relation == "KONSISTEN_PADA" && it.toLabel == "Olahraga Pagi" })
        assertTrue(triples.any { it.relation == "MENURUNKAN" && it.fromLabel == "Mood Lelah" && it.toLabel == "Membaca Buku" })
        assertTrue(triples.any { it.relation == "SEDANG_MEMBANGUN" && it.toLabel == "Robot Kembaran Digital" })
        assertTrue(triples.any { it.relation == "ALOKASI_UTAMA" && it.toLabel == "Pengeluaran Makan" })
        assertTrue(triples.any { it.relation == "MENGUTAMAKAN" && it.toLabel == "AI & Robotik" })
        assertFalse(triples.any { it.toLabel.contains("Rahasia Privat") })
    }
}
