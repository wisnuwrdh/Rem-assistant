package com.ecosystemku.assistant.data.kg

import com.ecosystemku.assistant.data.local.KgEdgeEntity
import com.ecosystemku.assistant.data.local.KgNodeEntity
import com.ecosystemku.assistant.data.local.UserMemoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityBenchmarkTest {

    @Test
    fun benchmarkDefines25UniqueScenariosAcross5Domains() {
        assertEquals(25, SimilarityBenchmark.SCENARIOS.size)
        val ids = SimilarityBenchmark.SCENARIOS.map { it.id }.toSet()
        assertEquals(25, ids.size)
        val domains = SimilarityBenchmark.SCENARIOS.map { it.domain }.toSet()
        assertEquals(5, domains.size)
    }

    @Test
    fun offlineCoverageIncreasesSignificantlyWithActiveNonSelfGraphAndMemories() {
        val emptyReport = SimilarityBenchmark.evaluateOfflineCoverage(
            nodes = emptyList(),
            edges = emptyList(),
            memories = emptyList()
        )
        assertEquals(25, emptyReport.totalScenarios)
        assertEquals("offline", emptyReport.mode)
        assertEquals(0, emptyReport.overallScorePercent)

        val self = KgNodeEntity(id = 1L, nodeKey = "self", nodeType = "SELF", label = "Wisnu", status = "active")
        val nilai = KgNodeEntity(
            id = 2L,
            nodeKey = "nilai:integritas_dan_keluarga",
            nodeType = "NILAI",
            label = "Menjaga keluarga dan prinsip jujur",
            salience = 1.0f,
            confidence = 0.95f,
            status = "active"
        )
        val habit = KgNodeEntity(
            id = 3L,
            nodeKey = "kebiasaan:pomodoro_catat_keuangan",
            nodeType = "KEBIASAAN",
            label = "Catat pengeluaran dan fokus pomodoro",
            salience = 0.9f,
            confidence = 0.9f,
            status = "active"
        )
        val mindset = KgNodeEntity(
            id = 4L,
            nodeKey = "pola_pikir:sistematis_evaluasi",
            nodeType = "POLA_PIKIR",
            label = "Analisis sistematis dan terbuka koreksi diri",
            salience = 0.85f,
            confidence = 0.85f,
            status = "active"
        )
        val goal = KgNodeEntity(
            id = 5L,
            nodeKey = "tujuan:dana_aman_proyek_mandiri",
            nodeType = "TUJUAN",
            label = "Tabungan dana aman dan proyek mandiri",
            salience = 0.9f,
            confidence = 0.9f,
            status = "active"
        )

        val edges = listOf(
            KgEdgeEntity(id = 10L, fromNodeId = 2L, toNodeId = 5L, relation = "MEMOTIVASI", status = "active"),
            KgEdgeEntity(id = 11L, fromNodeId = 3L, toNodeId = 5L, relation = "MENDUKUNG", status = "active"),
            KgEdgeEntity(id = 12L, fromNodeId = 4L, toNodeId = 3L, relation = "MENYEBABKAN", status = "active")
        )
        val memories = listOf(
            UserMemoryEntity(
                id = 1L,
                category = "kebiasaan",
                factKey = "kerja_fokus",
                factValue = "Suka kerja mandiri pakai timer dan mencatat bujet",
                status = "active"
            )
        )

        val richReport = SimilarityBenchmark.evaluateOfflineCoverage(
            nodes = listOf(self, nilai, habit, mindset, goal),
            edges = edges,
            memories = memories
        )

        assertTrue(richReport.overallScorePercent > 40)
        assertTrue(richReport.domainScores.containsKey("Keuangan"))
        assertTrue(richReport.results.any { it.matchedSignals.isNotEmpty() })
    }
}
