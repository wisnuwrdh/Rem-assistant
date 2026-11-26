package com.ecosystemku.assistant.data.skills

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemSkillRegistryTest {

    @Test
    fun stripFrontmatterRemovesLeadingYamlBlockAndPreservesBody() {
        val raw = """
            ---
            name: digital-twin-interviewer
            description: Use when running Sesi Kenali Aku.
            ---

            # Digital Twin Interviewer

            ## Peran
            Gali aturan pengambilan keputusan pengguna.
        """.trimIndent()

        val stripped = RemSkillSelector.stripFrontmatter(raw)
        assertTrue(stripped.startsWith("# Digital Twin Interviewer"))
        assertTrue(!stripped.contains("name: digital-twin-interviewer"))
        assertTrue(stripped.contains("Gali aturan pengambilan keputusan pengguna."))
    }

    @Test
    fun stripFrontmatterLeavesMarkdownWithoutYamlUntouched() {
        val raw = "# Judul Biasa\nIsi panduan singkat."
        assertEquals(raw, RemSkillSelector.stripFrontmatter(raw))
    }

    @Test
    fun selectSkillIdPrioritizesKenaliAkuSession() {
        val id = RemSkillSelector.selectSkillId(
            userMessage = "Aku bingung soal kerja larut vs tidur cukup.",
            sessionTitle = "Sesi Kenali Aku",
            isKenaliAku = true
        )
        assertEquals("digital-twin-interviewer", id)
    }

    @Test
    fun selectSkillIdDetectsWeeklyLifeReviewBeforeSingleModuleSkills() {
        val id = RemSkillSelector.selectSkillId(
            userMessage = "Rem, tolong review minggu ini lintas semua modulku termasuk keuangan dan jurnal.",
            sessionTitle = "Obrolan Baru",
            isKenaliAku = false
        )
        assertEquals("weekly-life-review", id)
    }

    @Test
    fun selectSkillIdDetectsFinancialAdvisorOnlyForAnalysisOrWishlistDecision() {
        val adviceId = RemSkillSelector.selectSkillId(
            userMessage = "Cek keuangan buku Personal dong, kok uang cepat habis dan boleh beli wishlist keyboard gak?",
            sessionTitle = "Obrolan Baru",
            isKenaliAku = false
        )
        assertEquals("cucimoney-financial-advisor", adviceId)

        val simpleLogId = RemSkillSelector.selectSkillId(
            userMessage = "Catat beli kopi susu 25000 pakai BCA",
            sessionTitle = "Obrolan Baru",
            isKenaliAku = false
        )
        assertNull(simpleLogId)
    }

    @Test
    fun selectSkillIdDetectsKronikReflectionWithoutTriggeringOnSimpleJournalAdd() {
        val reflectId = RemSkillSelector.selectSkillId(
            userMessage = "Habis baca ulang Kronik kemarin aku overthinking dan merasa gagal total.",
            sessionTitle = "Obrolan Baru",
            isKenaliAku = false
        )
        assertEquals("kronik-reflection-therapist", reflectId)

        val simpleAddId = RemSkillSelector.selectSkillId(
            userMessage = "Catat ke Kronik hari ini lancar kerja dari pagi sampai sore",
            sessionTitle = "Obrolan Baru",
            isKenaliAku = false
        )
        assertNull(simpleAddId)
    }

    @Test
    fun selectSkillIdDetectsIdekuStrategistOnBacklogOrNewIdeaEvaluation() {
        val strategyId = RemSkillSelector.selectSkillId(
            userMessage = "Ideku menumpuk banget di IdeKu, bantu pilah harus fokus ke mana dan next step terkecilnya.",
            sessionTitle = "Obrolan Baru",
            isKenaliAku = false
        )
        assertEquals("ideku-execution-strategist", strategyId)

        val newIdeaId = RemSkillSelector.selectSkillId(
            userMessage = "Aku punya ide baru bikin tracker tidur pakai audio, layak gak?",
            sessionTitle = "Obrolan Baru",
            isKenaliAku = false
        )
        assertEquals("ideku-execution-strategist", newIdeaId)
    }

    @Test
    fun selectSkillIdReturnsNullForCasualChatAndDeviceCommands() {
        assertNull(
            RemSkillSelector.selectSkillId(
                userMessage = "Selamat malam Rem, hari ini dingin ya.",
                sessionTitle = "Obrolan Baru",
                isKenaliAku = false
            )
        )
        assertNull(
            RemSkillSelector.selectSkillId(
                userMessage = "Pasang alarm jam 05:30 besok pagi",
                sessionTitle = "Obrolan Baru",
                isKenaliAku = false
            )
        )
    }
}
