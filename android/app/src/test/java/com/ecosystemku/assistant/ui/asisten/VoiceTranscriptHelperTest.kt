package com.ecosystemku.assistant.ui.asisten

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTranscriptHelperTest {

    @Test
    fun pickBestCandidatePrefersNearbyConfidenceEcosystemMatchAndNormalizesTerms() {
        val chosen = VoiceTranscriptHelper.pickBestCandidate(
            candidates = listOf("catat ke dari os", "catat ke daily os"),
            confidenceScores = floatArrayOf(0.88f, 0.82f)
        )
        assertEquals("catat ke Daily OS", chosen)

        // Do not override top result if ecosystem candidate has much lower confidence
        val keepTop = VoiceTranscriptHelper.pickBestCandidate(
            candidates = listOf("beli makan siang", "beli daily os"),
            confidenceScores = floatArrayOf(0.95f, 0.40f)
        )
        assertEquals("beli makan siang", keepTop)
    }

    @Test
    fun normalizeAppTermsFixesCommonIndonesianSttMishearings() {
        assertEquals(
            "Catat pemasukan ke CuciMoney dan Daily OS serta LinkKu",
            VoiceTranscriptHelper.normalizeAppTerms("Catat pemasukan ke cuci mani dan deili os serta link ku")
        )
        assertEquals(
            "Simpan ke IdeKu",
            VoiceTranscriptHelper.normalizeAppTerms("Simpan ke ide ku")
        )
    }

    @Test
    fun joinSegmentsConcatenatesCleanly() {
        assertEquals("Halo Rem", VoiceTranscriptHelper.joinSegments("", "Halo Rem"))
        assertEquals("Halo Rem", VoiceTranscriptHelper.joinSegments("Halo Rem", ""))
        assertEquals("Halo Rem tolong catat", VoiceTranscriptHelper.joinSegments("Halo Rem", "tolong catat"))
    }
}
