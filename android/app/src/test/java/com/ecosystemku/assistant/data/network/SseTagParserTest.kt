package com.ecosystemku.assistant.data.network

import org.junit.Assert.assertFalse
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
}
