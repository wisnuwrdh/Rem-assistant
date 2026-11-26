package com.ecosystemku.assistant.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseTagParserTest {

    @Test
    fun discussionAboutJournalDoesNotTriggerOpenKronik() {
        assertFalse(SseTagParser.shouldAllowOpenApp("Ayo bahas jurnal aku kemarin", "kronik"))
        assertFalse(SseTagParser.shouldAllowOpenApp("Tolong bacakan jurnal terbaruku", "kronik"))
        assertFalse(SseTagParser.shouldAllowOpenApp("Catat di jurnal kalau hari ini capek banget", "kronik"))
        assertFalse(SseTagParser.shouldAllowOpenApp("Jangan buka kronik, bahas di sini aja", "kronik"))
        assertFalse(SseTagParser.shouldAllowOpenApp("Lihat jurnal aku minggu lalu dong", "kronik"))
    }

    @Test
    fun discussionAboutOtherModulesDoesNotTriggerOpenApp() {
        assertFalse(SseTagParser.shouldAllowOpenApp("Analisis pengeluaran keuanganku bulan ini", "cucimoney"))
        assertFalse(SseTagParser.shouldAllowOpenApp("Apa saja daftar wishlist atau ideku?", "ideku"))
        assertFalse(SseTagParser.shouldAllowOpenApp("Cek streak habit dan to-do hari ini", "dailyos"))
        assertFalse(SseTagParser.shouldAllowOpenApp("Simpan link ini https://example.com ke bookmark", "linkku"))
        assertFalse(SseTagParser.shouldAllowOpenApp("Gk usah buka dompet cucimoney", "cucimoney"))
    }

    @Test
    fun explicitOpenCommandsAreAllowedForAllFiveModules() {
        assertTrue(SseTagParser.shouldAllowOpenApp("Buka Kronik sekarang", "kronik"))
        assertTrue(SseTagParser.shouldAllowOpenApp("Bukain halaman jurnal dong", "kronik"))
        assertTrue(SseTagParser.shouldAllowOpenApp("Open CuciMoney+", "cucimoney"))
        assertTrue(SseTagParser.shouldAllowOpenApp("Masuk ke menu keuangan", "cucimoney"))
        assertTrue(SseTagParser.shouldAllowOpenApp("Tampilkan halaman IdeKu", "ideku"))
        assertTrue(SseTagParser.shouldAllowOpenApp("Pindah ke Daily OS", "dailyos"))
        assertTrue(SseTagParser.shouldAllowOpenApp("Buka LinkKu", "linkku"))
    }

    @Test
    fun kronikEditDeleteAndArchiveTagsAreParsedCorrectly() {
        val editParsed = SseTagParser.parseFinal(
            "Siap, jurnal diperbarui. [KRONIK:EDIT|abc123|Judul Baru|Isi baru nih|Fokus|2026-11-20] [EXPR:smile]"
        )
        assertEquals("Siap, jurnal diperbarui.", editParsed.cleanText)
        assertEquals(1, editParsed.kronikActions.size)
        val edit = editParsed.kronikActions.first() as SseTagParser.KronikChatAction.EditEntry
        assertEquals("abc123", edit.target)
        assertEquals("Judul Baru", edit.title)
        assertEquals("Isi baru nih", edit.content)
        assertEquals("Fokus", edit.mood)
        assertEquals("2026-11-20", edit.date)

        val clearMoodParsed = SseTagParser.parseFinal(
            "Mood dihapus. [KRONIK:EDIT|abc123|||-|] [EXPR:none]"
        )
        val clearMood = clearMoodParsed.kronikActions.first() as SseTagParser.KronikChatAction.EditEntry
        assertEquals("__CLEAR__", clearMood.mood)

        val delParsed = SseTagParser.parseFinal(
            "Oke dihapus. [KRONIK:DEL|abc123] [EXPR:none]"
        )
        val del = delParsed.kronikActions.first() as SseTagParser.KronikChatAction.DeleteEntry
        assertEquals("abc123", del.target)
        assertFalse(del.archiveOnly)

        val archParsed = SseTagParser.parseFinal(
            "Diarsipkan ya. [KRONIK:ARCHIVE|abc123] [EXPR:none]"
        )
        val arch = archParsed.kronikActions.first() as SseTagParser.KronikChatAction.DeleteEntry
        assertEquals("abc123", arch.target)
        assertTrue(arch.archiveOnly)

        val invalidEdit = SseTagParser.parseFinal("[KRONIK:EDIT|abc123||||]")
        assertNull(invalidEdit.kronikActions.firstOrNull())
    }

    @Test
    fun idekuSearchQueryParsesNaturalAndStructuredFilters() {
        val natural = SseTagParser.parseIdekuSearchQuery("Idea Belum")
        assertEquals("Idea", natural.type)
        assertEquals("Belum", natural.status)
        assertEquals("", natural.query)

        val naturalWish = SseTagParser.parseIdekuSearchQuery("daftar wishlist")
        assertEquals("Wishlist", naturalWish.type)
        assertEquals("", naturalWish.query)

        val structured = SseTagParser.parseIdekuSearchQuery("type=Idea;status=Belum;pri=Penting;cat=Bisnis;q=saas")
        assertEquals("Idea", structured.type)
        assertEquals("Belum", structured.status)
        assertEquals("Penting", structured.priority)
        assertEquals("Bisnis", structured.category)
        assertEquals("saas", structured.query)
    }

    @Test
    fun kgUpdateArchiveAndMergeTagsAreParsedAndStripped() {
        val raw = "Oke dicatat. [KG:UPDATE|Begadang|Tidur teratur|Tidur jam 10|KEBIASAAN|konsisten] " +
            "[KG:ARCHIVE|Panik deadline|sudah diatasi] " +
            "[KG:MERGE|Stres uang|Tekanan finansial|sama arti] [EXPR:smile]"
        assertEquals("Oke dicatat.", SseTagParser.visibleTextDuringStream(raw))
        val parsed = SseTagParser.parseFinal(raw)
        assertEquals("Oke dicatat.", parsed.cleanText)
        assertEquals(3, parsed.kgActions.size)

        val upd = parsed.kgActions[0] as SseTagParser.KgChatAction.UpdateNode
        assertEquals("Begadang", upd.target)
        assertEquals("Tidur teratur", upd.newLabel)
        assertEquals("Tidur jam 10", upd.newSummary)
        assertEquals("KEBIASAAN", upd.newType)
        assertEquals("konsisten", upd.reason)

        val arch = parsed.kgActions[1] as SseTagParser.KgChatAction.ArchiveNode
        assertEquals("Panik deadline", arch.target)
        assertEquals("sudah diatasi", arch.reason)

        val merge = parsed.kgActions[2] as SseTagParser.KgChatAction.MergeNodes
        assertEquals("Stres uang", merge.sourceTarget)
        assertEquals("Tekanan finansial", merge.destinationTarget)
        assertEquals("sama arti", merge.reason)
    }

    @Test
    fun kronikMergeAndIdekuArchiveMergeTagsAreParsedCorrectly() {
        val parsed = SseTagParser.parseFinal(
            "Siap digabung. [KRONIK:MERGE|Catatan Lama|Catatan Utama] " +
                "[IDEKU:ARCHIVE|Drone Mini] [IDEKU:MERGE|App Catatan v1|App Catatan] [EXPR:happy]"
        )
        assertEquals("Siap digabung.", parsed.cleanText)

        val kronikMerge = parsed.kronikActions.first() as SseTagParser.KronikChatAction.MergeEntries
        assertEquals("Catatan Lama", kronikMerge.sourceTarget)
        assertEquals("Catatan Utama", kronikMerge.destinationTarget)

        assertEquals(2, parsed.idekuActions.size)
        val idekuArchive = parsed.idekuActions[0] as SseTagParser.IdekuChatAction.ArchiveItem
        assertEquals("Drone Mini", idekuArchive.target)
        val idekuMerge = parsed.idekuActions[1] as SseTagParser.IdekuChatAction.MergeItems
        assertEquals("App Catatan v1", idekuMerge.sourceTarget)
        assertEquals("App Catatan", idekuMerge.destinationTarget)
    }
}
