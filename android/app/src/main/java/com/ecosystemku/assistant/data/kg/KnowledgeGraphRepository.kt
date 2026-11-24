package com.ecosystemku.assistant.data.kg

import com.ecosystemku.assistant.data.local.ConversationSummaryEntity
import com.ecosystemku.assistant.data.local.KnowledgeGraphDao
import com.ecosystemku.assistant.data.local.UserMemoryEntity

/**
 * Mengelola operasi baca/tulis Personal Knowledge Graph di SQLite lokal,
 * termasuk traversal 1-2 lompatan untuk konteks prompt dan ekspor Digital Twin JSON.
 */
class KnowledgeGraphRepository(
    private val kgDao: KnowledgeGraphDao
) {
    suspend fun ensureSelfNode(profileName: String): Long {
        val label = profileName.trim().ifBlank { "Tuan" }
        return kgDao.upsertNode(
            nodeKey = "self",
            nodeType = "SELF",
            label = label,
            summary = "Pusat identitas dan pola berpikir pengguna",
            salienceBoost = 1.0f,
            sourceType = "manual"
        )
    }

    suspend fun recordTriples(
        profileName: String,
        triples: List<ExtractedTripleDto>,
        sourceType: String = "chat",
        weightBoost: Float = 0.7f
    ) {
        if (triples.isEmpty()) return
        val now = System.currentTimeMillis()
        ensureSelfNode(profileName)

        triples.forEach { t ->
            val fromLabelRaw = t.fromLabel.trim()
            val toLabelRaw = t.toLabel.trim()
            if (fromLabelRaw.isBlank() || toLabelRaw.isBlank()) return@forEach

            val fromType = KnowledgeGraphHelpers.normalizeNodeType(t.fromType)
            val toType = KnowledgeGraphHelpers.normalizeNodeType(t.toType)
            val fromKey = KnowledgeGraphHelpers.canonicalNodeKey(fromType, fromLabelRaw)
            val toKey = KnowledgeGraphHelpers.canonicalNodeKey(toType, toLabelRaw)
            if (fromKey == toKey) return@forEach

            val resolvedFromLabel = if (fromKey == "self") profileName.trim().ifBlank { "Tuan" } else fromLabelRaw.take(64)
            val resolvedToLabel = if (toKey == "self") profileName.trim().ifBlank { "Tuan" } else toLabelRaw.take(64)

            val fromId = kgDao.upsertNode(
                nodeKey = fromKey,
                nodeType = if (fromKey == "self") "SELF" else fromType,
                label = resolvedFromLabel,
                summary = "",
                salienceBoost = if (fromKey == "self") 1.0f else 0.15f,
                sourceType = sourceType,
                nowMs = now
            )
            val toId = kgDao.upsertNode(
                nodeKey = toKey,
                nodeType = if (toKey == "self") "SELF" else toType,
                label = resolvedToLabel,
                summary = "",
                salienceBoost = if (toKey == "self") 1.0f else 0.18f,
                sourceType = sourceType,
                nowMs = now
            )
            val replaceTarget = t.replacesLabel.trim()
            val supersededCount = if (replaceTarget.length >= 2) {
                supersedeMatchingEdges(
                    fromId = fromId,
                    newToId = toId,
                    newToLabel = resolvedToLabel,
                    replacesLabel = replaceTarget,
                    nowMs = now
                )
            } else {
                0
            }
            val cleanEvidence = t.evidence.trim()
            val finalEvidence = if (supersededCount > 0 && !cleanEvidence.contains(replaceTarget, ignoreCase = true)) {
                val suffix = "(evolusi dari $replaceTarget)"
                if (cleanEvidence.isBlank()) suffix else "$cleanEvidence • $suffix"
            } else {
                cleanEvidence
            }.take(240)

            kgDao.upsertEdge(
                fromNodeId = fromId,
                toNodeId = toId,
                relation = KnowledgeGraphHelpers.normalizeRelation(t.relation),
                evidence = finalEvidence,
                weightBoost = weightBoost,
                confidence = t.confidence.coerceIn(0.3f, 1.0f),
                nowMs = now
            )
        }
    }

    private suspend fun supersedeMatchingEdges(
        fromId: Long,
        newToId: Long,
        newToLabel: String,
        replacesLabel: String,
        nowMs: Long
    ): Int {
        val allNodes = kgDao.getAllNodes()
        val allEdges = kgDao.getAllEdges()
        val q = replacesLabel.lowercase()
        val matchedNodeIds = allNodes.filter { n ->
            n.id != newToId &&
                n.nodeType != "SELF" &&
                (n.label.contains(q, ignoreCase = true) || n.nodeKey.endsWith(q.replace(Regex("\\s+"), "_")))
        }.map { it.id }.toSet()
        if (matchedNodeIds.isEmpty()) return 0

        var updated = 0
        allEdges.forEach { edge ->
            val pointsToOld = edge.toNodeId in matchedNodeIds || edge.fromNodeId in matchedNodeIds
            if (pointsToOld && !(edge.fromNodeId == fromId && edge.toNodeId == newToId)) {
                val decayedWeight = (edge.weight * 0.3f).coerceIn(0.15f, 0.35f)
                val prefix = "[Historis → diganti '$newToLabel']"
                val nextEvidence = if (edge.evidence.startsWith("[Historis")) {
                    edge.evidence
                } else if (edge.evidence.isBlank()) {
                    prefix
                } else {
                    "$prefix ${edge.evidence}"
                }.take(240)
                kgDao.updateEdgeEvolution(edge.id, decayedWeight, nextEvidence, nowMs)
                updated++
            }
        }
        return updated
    }

    suspend fun markEdgeHistorical(edgeId: Long, note: String = "") {
        val edge = kgDao.getEdgeById(edgeId) ?: return
        val prefix = if (note.isNotBlank()) "[Historis → $note]" else "[Historis]"
        val nextEvidence = if (edge.evidence.startsWith("[Historis")) {
            edge.evidence
        } else if (edge.evidence.isBlank()) {
            prefix
        } else {
            "$prefix ${edge.evidence}"
        }.take(240)
        kgDao.updateEdgeEvolution(
            edgeId = edgeId,
            weight = (edge.weight * 0.25f).coerceIn(0.15f, 0.3f),
            evidence = nextEvidence,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun addManualEdge(
        profileName: String,
        fromType: String,
        fromLabel: String,
        relation: String,
        toType: String,
        toLabel: String,
        evidence: String,
        replacesLabel: String = ""
    ): Boolean {
        val cleanFrom = fromLabel.trim()
        val cleanTo = toLabel.trim()
        if (cleanFrom.isBlank() || cleanTo.isBlank()) return false
        val fromKey = KnowledgeGraphHelpers.canonicalNodeKey(KnowledgeGraphHelpers.normalizeNodeType(fromType), cleanFrom)
        val toKey = KnowledgeGraphHelpers.canonicalNodeKey(KnowledgeGraphHelpers.normalizeNodeType(toType), cleanTo)
        if (fromKey == toKey) return false

        recordTriples(
            profileName = profileName,
            triples = listOf(
                ExtractedTripleDto(
                    fromType = fromType,
                    fromLabel = cleanFrom,
                    relation = relation,
                    toType = toType,
                    toLabel = cleanTo,
                    evidence = evidence,
                    confidence = 1.0f,
                    replacesLabel = replacesLabel.trim()
                )
            ),
            sourceType = "manual",
            weightBoost = 1.0f
        )
        return true
    }

    suspend fun updateNodeDetails(nodeId: Long, label: String, summary: String) {
        val cleanLabel = label.trim().take(64)
        if (cleanLabel.isEmpty()) return
        kgDao.updateNodeDetails(nodeId, cleanLabel, summary.trim().take(280), System.currentTimeMillis())
    }

    suspend fun deleteNode(nodeId: Long) {
        kgDao.deleteNodeAndEdges(nodeId)
    }

    suspend fun deleteEdge(edgeId: Long) {
        kgDao.deleteEdgeById(edgeId)
    }

    suspend fun queryContextForMessage(userMessage: String, maxEdges: Int = 14): List<KgTripleSummary> {
        val nodes = kgDao.getAllNodes()
        val edges = kgDao.getAllEdges()
        return KnowledgeGraphHelpers.selectRelevantTriples(nodes, edges, userMessage, maxEdges)
    }

    suspend fun searchGraph(query: String): String {
        val nodes = kgDao.getAllNodes()
        val edges = kgDao.getAllEdges()
        if (nodes.isEmpty() || edges.isEmpty()) {
            return "Knowledge Graph masih kosong (belum ada koneksi pola pikir/perilaku tersimpan)."
        }
        val hits = KnowledgeGraphHelpers.searchTriples(nodes, edges, query, maxResults = 18)
        if (hits.isEmpty()) {
            return "Tidak ditemukan koneksi Knowledge Graph yang cocok untuk \"$query\"."
        }
        return hits.joinToString("\n") { (from, edge, to) ->
            val ev = if (edge.evidence.isNotBlank()) " — bukti: ${edge.evidence}" else ""
            "- [${from.nodeType}] ${from.label} --(${edge.relation})--> [${to.nodeType}] ${to.label} (kekuatan ${"%.1f".format(edge.weight)})$ev"
        }
    }

    suspend fun exportDigitalTwinJson(
        profileName: String,
        memories: List<UserMemoryEntity>,
        summaries: List<ConversationSummaryEntity>
    ): String {
        val nodes = kgDao.getAllNodes()
        val edges = kgDao.getAllEdges()
        return KnowledgeGraphHelpers.buildDigitalTwinJson(profileName, nodes, edges, memories, summaries)
    }
}
