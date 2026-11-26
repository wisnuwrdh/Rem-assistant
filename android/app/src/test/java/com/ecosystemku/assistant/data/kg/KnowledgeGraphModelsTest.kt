package com.ecosystemku.assistant.data.kg

import com.ecosystemku.assistant.data.local.KgEdgeEntity
import com.ecosystemku.assistant.data.local.KgNodeEntity
import com.ecosystemku.assistant.data.local.UserMemoryEntity
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeGraphModelsTest {

    @Test
    fun canonicalNodeKeyNormalizesSelfAndDuplicates() {
        assertEquals("self", KnowledgeGraphHelpers.canonicalNodeKey("SELF", "Tuan"))
        assertEquals("self", KnowledgeGraphHelpers.canonicalNodeKey("POLA_PIKIR", "Aku"))
        assertEquals(
            "nilai:kemandirian_data",
            KnowledgeGraphHelpers.canonicalNodeKey("nilai", "  Kemandirian   Data! ")
        )
        assertEquals("TERKAIT", KnowledgeGraphHelpers.normalizeRelation("relasi_asing"))
    }

    @Test
    fun selectRelevantTriplesPrioritizesTwoHopQueryMatches() {
        val nSelf = KgNodeEntity(1L, "self", "SELF", "Wisnu", salience = 1.0f)
        val nSleep = KgNodeEntity(2L, "kebiasaan:begadang", "KEBIASAAN", "Begadang", salience = 0.5f)
        val nMood = KgNodeEntity(3L, "emosi_pemicu:lelah", "EMOSI_PEMICU", "Mood Lelah", salience = 0.5f)
        val nSport = KgNodeEntity(4L, "kebiasaan:olahraga", "KEBIASAAN", "Olahraga Pagi", salience = 0.5f)
        val nCoffee = KgNodeEntity(5L, "minat:kopi", "MINAT", "Kopi Hitam", salience = 0.9f)

        val eCoffee = KgEdgeEntity(1L, 1L, 5L, "MENGUTAMAKAN", weight = 2.0f, confidence = 0.9f)
        val eSleepMood = KgEdgeEntity(2L, 2L, 3L, "MEMICU", weight = 1.0f, confidence = 0.8f)
        val eMoodSport = KgEdgeEntity(3L, 3L, 4L, "MENURUNKAN", weight = 1.0f, confidence = 0.8f)

        val selected = KnowledgeGraphHelpers.selectRelevantTriples(
            nodes = listOf(nSelf, nSleep, nMood, nSport, nCoffee),
            edges = listOf(eCoffee, eSleepMood, eMoodSport),
            userMessage = "Kenapa olahraga pagi ku sering bolong ya?",
            maxEdges = 2
        )

        val rels = selected.map { it.edge.relation }
        assertTrue("MENURUNKAN" in rels)
        assertTrue("MEMICU" in rels)
    }

    @Test
    fun buildDigitalTwinJsonContainsPoliciesAndCanonicalKeys() {
        val nSelf = KgNodeEntity(1L, "self", "SELF", "Wisnu", salience = 1.0f)
        val nVal = KgNodeEntity(2L, "nilai:efisiensi", "NILAI", "Efisiensi Token", salience = 0.9f)
        val edge = KgEdgeEntity(1L, 1L, 2L, "MENGUTAMAKAN", evidence = "Hapus preview web", weight = 2.5f)
        val mem = UserMemoryEntity(1L, "identitas", "nama", "Wisnu")

        val raw = KnowledgeGraphHelpers.buildDigitalTwinJson(
            profileName = "Wisnu",
            nodes = listOf(nSelf, nVal),
            edges = listOf(edge),
            memories = listOf(mem),
            summaries = emptyList()
        )

        assertTrue(raw.contains("rem-digital-twin-kg-v1"))
        assertTrue(raw.contains("nilai:efisiensi"))
        assertTrue(raw.contains("Hapus preview web"))
        assertTrue(raw.contains("\"decisionPolicyCount\": 1"))
    }

    @Test
    fun buildBackupDtoRoundTripPreservesKeys() {
        val nodes = listOf(
            KgNodeEntity(id = 10L, nodeKey = "self", nodeType = "SELF", label = "Tuan", salience = 1.0f),
            KgNodeEntity(id = 20L, nodeKey = "tujuan:ai_kembaran", nodeType = "TUJUAN", label = "AI Kembaran", salience = 0.8f)
        )
        val edges = listOf(
            KgEdgeEntity(
                id = 5L,
                fromNodeId = 10L,
                toNodeId = 20L,
                relation = "SEDANG_MEMBANGUN",
                evidence = "Ingin mengumpulkan data pola pikir untuk masa depan",
                weight = 1.8f,
                confidence = 0.9f
            )
        )

        val encoded = KnowledgeGraphHelpers.json.encodeToString(
            KnowledgeGraphHelpers.buildBackupDto(nodes, edges)
        )
        val decoded = KnowledgeGraphHelpers.json.decodeFromString<KgBackupWrapperDto>(encoded)

        assertEquals(2, decoded.nodes.size)
        assertEquals(1, decoded.edges.size)
        assertEquals("self", decoded.edges.first().fromKey)
        assertEquals("tujuan:ai_kembaran", decoded.edges.first().toKey)
        assertEquals("SEDANG_MEMBANGUN", decoded.edges.first().relation)
        assertEquals(1.8f, decoded.edges.first().weight, 0.01f)
    }

    @Test
    fun historicalEdgeDownRankedAndExcludedFromActivePolicies() {
        val self = KgNodeEntity(id = 1L, nodeKey = "self", nodeType = "SELF", label = "Tuan", salience = 1.0f)
        val oldHabit = KgNodeEntity(id = 2L, nodeKey = "kebiasaan:begadang", nodeType = "KEBIASAAN", label = "Begadang", salience = 0.6f)
        val newHabit = KgNodeEntity(id = 3L, nodeKey = "kebiasaan:tidur_jam_10", nodeType = "KEBIASAAN", label = "Tidur jam 10 malam", salience = 0.8f)

        val oldEdge = KgEdgeEntity(
            id = 11L,
            fromNodeId = 1L,
            toNodeId = 2L,
            relation = "CENDERUNG_SAAT",
            evidence = "[Historis → diganti 'Tidur jam 10 malam'] Dulu kerja larut",
            weight = 0.25f,
            confidence = 0.8f
        )
        val newEdge = KgEdgeEntity(
            id = 12L,
            fromNodeId = 1L,
            toNodeId = 3L,
            relation = "CENDERUNG_SAAT",
            evidence = "Jadwal tidur baru • (evolusi dari Begadang)",
            weight = 1.4f,
            confidence = 0.9f
        )

        assertTrue(KnowledgeGraphHelpers.isHistoricalEdge(oldEdge))
        assertTrue(!KnowledgeGraphHelpers.isHistoricalEdge(newEdge))

        val ranked = KnowledgeGraphHelpers.selectRelevantTriples(
            nodes = listOf(self, oldHabit, newHabit),
            edges = listOf(oldEdge, newEdge),
            userMessage = "kebiasaan tidurku belakangan ini",
            maxEdges = 2
        )
        assertEquals(1, ranked.size)
        assertEquals(3L, ranked.first().toNode.id)

        val exported = KnowledgeGraphHelpers.buildDigitalTwinJson(
            profileName = "Tuan",
            nodes = listOf(self, oldHabit, newHabit),
            edges = listOf(oldEdge, newEdge),
            memories = emptyList(),
            summaries = emptyList()
        )
        assertTrue(exported.contains("\"isHistorical\": true"))
        assertTrue(exported.contains("\"decisionPolicyCount\": 1"))
    }

    @Test
    fun deprecatedNodesAndMenggantikanEdgesAreExcludedFromActivePromptAndPolicies() {
        val self = KgNodeEntity(id = 1L, nodeKey = "self", nodeType = "SELF", label = "Wisnu", salience = 1.0f, status = "active")
        val oldNode = KgNodeEntity(
            id = 2L,
            nodeKey = "kebiasaan:begadang",
            nodeType = "KEBIASAAN",
            label = "Begadang",
            status = "deprecated",
            sourceRef = "chat:session=1:msg=2",
            confidence = 0.7f
        )
        val newNode = KgNodeEntity(
            id = 3L,
            nodeKey = "kebiasaan:tidur_jam_10",
            nodeType = "KEBIASAAN",
            label = "Tidur jam 10",
            status = "active",
            sourceRef = "chat:session=2:msg=9",
            lastConfirmedAt = 123456L,
            confidence = 0.95f,
            validationState = "confirmed"
        )
        val activeEdge = KgEdgeEntity(
            id = 10L,
            fromNodeId = 1L,
            toNodeId = 3L,
            relation = "KONSISTEN_PADA",
            status = "active",
            weight = 1.2f,
            confidence = 0.9f
        )
        val replaceEdge = KgEdgeEntity(
            id = 11L,
            fromNodeId = 3L,
            toNodeId = 2L,
            relation = "MENGGANTIKAN",
            status = "historical",
            weight = 0.6f,
            confidence = 0.9f
        )

        val selected = KnowledgeGraphHelpers.selectRelevantTriples(
            nodes = listOf(self, oldNode, newNode),
            edges = listOf(activeEdge, replaceEdge),
            userMessage = "begadang tidur",
            maxEdges = 5
        )
        assertEquals(1, selected.size)
        assertEquals(3L, selected.first().toNode.id)

        val backupRaw = KnowledgeGraphHelpers.json.encodeToString(
            KnowledgeGraphHelpers.buildBackupDto(listOf(self, oldNode, newNode), listOf(activeEdge, replaceEdge))
        )
        val decoded = KnowledgeGraphHelpers.json.decodeFromString<KgBackupWrapperDto>(backupRaw)
        assertEquals(3, decoded.nodes.size)
        assertEquals("deprecated", decoded.nodes.first { it.nodeKey == "kebiasaan:begadang" }.status)
        assertEquals("confirmed", decoded.nodes.first { it.nodeKey == "kebiasaan:tidur_jam_10" }.validationState)
        assertEquals("MENGGANTIKAN", decoded.edges.first { it.relation == "MENGGANTIKAN" }.relation)
    }

    @Test
    fun semanticDuplicateDetectionReusesSimilarActiveNodeAcrossSynonyms() {
        val existing = KgNodeEntity(
            id = 5L,
            nodeKey = "emosi_pemicu:tekanan_keuangan",
            nodeType = "EMOSI_PEMICU",
            label = "Tekanan keuangan",
            status = "active"
        )
        val deprecated = KgNodeEntity(
            id = 6L,
            nodeKey = "emosi_pemicu:stres_uang_lama",
            nodeType = "EMOSI_PEMICU",
            label = "Beban finansial lama",
            status = "deprecated"
        )

        val matched = KnowledgeGraphHelpers.findSimilarActiveNode(
            nodes = listOf(existing, deprecated),
            candidateType = "EMOSI_PEMICU",
            candidateLabel = "Stres finansial"
        )
        assertEquals(5L, matched?.id)

        val unrelated = KnowledgeGraphHelpers.findSimilarActiveNode(
            nodes = listOf(existing, deprecated),
            candidateType = "MINAT",
            candidateLabel = "Olahraga lari pagi"
        )
        assertEquals(null, unrelated)
    }

    @Test
    fun decisionPolicyRelationsIncludeCausalConflictMotivationAndExampleTypes() {
        val expected = setOf("MENYEBABKAN", "BERTENTANGAN", "MEMOTIVASI", "CONTOH_DARI")
        assertTrue(KnowledgeGraphHelpers.VALID_RELATIONS.containsAll(expected))
        assertTrue(KnowledgeGraphHelpers.DECISION_POLICY_RELATIONS.containsAll(expected))
    }
}
