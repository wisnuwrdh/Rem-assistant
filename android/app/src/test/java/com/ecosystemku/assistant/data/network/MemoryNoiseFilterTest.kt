package com.ecosystemku.assistant.data.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryNoiseFilterTest {

    @Test
    fun isNoisyFactBlocksTransientBalancesTransactionsAndLinks() {
        assertTrue(
            MemoryNoiseFilter.isNoisyFact(
                "catatan",
                "kondisi_keuangan_hari_ini",
                "Hari ini merasa uangnya aman dengan Dana Cash Rp526.500 dan aset bersih Rp628.029"
            )
        )
        assertTrue(
            MemoryNoiseFilter.isNoisyFact(
                "catatan",
                "status_keuangan_hari_ini",
                "Hari ini merasa uangnya aman (Dana Cash Rp526.500, aset bersih Rp628.029)"
            )
        )
        assertTrue(
            MemoryNoiseFilter.isNoisyFact(
                "catatan",
                "pemasukan_hasil_10rb",
                "Mendapat pemasukan Rp10.000 kategori Hasil ke akun Dana Cash"
            )
        )
        assertTrue(
            MemoryNoiseFilter.isNoisyFact(
                "catatan",
                "link_agent_reach",
                "Menyimpan link GitHub https://github.com/Panniantong/agent-reach di LinkKu dengan judul agent-reach"
            )
        )
    }

    @Test
    fun isNoisyFactPreservesGenuineLongTermPersonalFacts() {
        assertFalse(MemoryNoiseFilter.isNoisyFact("identitas", "nama_panggilan", "Wisnu"))
        assertFalse(MemoryNoiseFilter.isNoisyFact("preferensi", "minuman_kopi", "Suka kopi hitam tanpa gula"))
        assertFalse(MemoryNoiseFilter.isNoisyFact("kebiasaan", "jam_tidur", "Tidur sekitar jam 11 malam dan journaling sebelumnya"))
        assertFalse(MemoryNoiseFilter.isNoisyFact("tujuan", "dana_darurat", "Menjaga patokan tabungan minimal 3x harga barang"))
        assertFalse(MemoryNoiseFilter.isNoisyFact("catatan", "prinsip_belanja", "Menahan pembelian impulsif dan mempertimbangkan ulang di akhir"))
    }

    @Test
    fun isNoisySummaryFiltersRoutineCrudTurnsAndKeepsReflectiveSummaries() {
        assertTrue(
            MemoryNoiseFilter.isNoisySummary(
                "User mengonfirmasi pekerjaan hari ini sudah selesai dan menyapa malam Rem."
            )
        )
        assertTrue(
            MemoryNoiseFilter.isNoisySummary(
                "User menambahkan pemasukan Rp10.000 dan menyatakan kondisi keuangannya hari ini aman."
            )
        )
        assertTrue(
            MemoryNoiseFilter.isNoisySummary(
                "User membatalkan pengeluaran Rp10.000 lalu mencatat pemasukan hasil Rp10.000 ke Dana Cash."
            )
        )
        assertFalse(
            MemoryNoiseFilter.isNoisySummary(
                "Rem memulai Sesi Kenali Aku dengan menanyakan alasan User mempertahankan patokan tabungan 3x harga barang."
            )
        )
    }

    @Test
    fun isNoisyTripleBlocksTransientTransactionsAndUrls() {
        assertTrue(
            MemoryNoiseFilter.isNoisyTriple(
                "Wisnu",
                "Pemasukan Rp10.000",
                "Mencatat pemasukan Rp10.000 ke Dana Cash hari ini"
            )
        )
        assertTrue(
            MemoryNoiseFilter.isNoisyTriple(
                "Wisnu",
                "https://github.com/example",
                "Menyimpan bookmark"
            )
        )
        assertFalse(
            MemoryNoiseFilter.isNoisyTriple(
                "Kehati-hatian finansial",
                "Patokan tabungan 3x harga barang",
                "Menjaga cadangan sebelum membeli barang baru"
            )
        )
    }
}
