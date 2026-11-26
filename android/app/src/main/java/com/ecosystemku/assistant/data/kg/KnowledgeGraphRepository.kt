package com.ecosystemku.assistant.data.kg

import com.ecosystemku.assistant.data.local.ConversationSummaryEntity
import com.ecosystemku.assistant.data.local.KgNodeEntity
import com.ecosystemku.assistant.data.local.KnowledgeGraphDao
import com.ecosystemku.assistant.data.local.UserMemoryEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        weightBoost: Float = 0.7f,
        sourceRef: String = ""
    ) {
        if (triples.isEmpty()) return
        val now = System.currentTimeMillis()
        ensureSelfNode(profileName)
        val isManual = sourceType == "manual"
        val existingNodes = kgDao.getAllNodes().toMutableList()

        triples.forEach { t ->
            val fromLabelRaw = t.fromLabel.trim()
            val toLabelRaw = t.toLabel.trim()
            if (fromLabelRaw.isBlank() || toLabelRaw.isBlank()) return@forEach

            val fromType = KnowledgeGraphHelpers.normalizeNodeType(t.fromType)
            val toType = KnowledgeGraphHelpers.normalizeNodeType(t.toType)
            val rawFromKey = KnowledgeGraphHelpers.canonicalNodeKey(fromType, fromLabelRaw)
            val rawToKey = KnowledgeGraphHelpers.canonicalNodeKey(toType, toLabelRaw)

            val simFrom = if (rawFromKey == "self") null else KnowledgeGraphHelpers.findSimilarActiveNode(existingNodes, fromType, fromLabelRaw)
            val simTo = if (rawToKey == "self") null else KnowledgeGraphHelpers.findSimilarActiveNode(existingNodes, toType, toLabelRaw)

            val fromKey = simFrom?.nodeKey ?: rawFromKey
            val toKey = simTo?.nodeKey ?: rawToKey
            if (fromKey == toKey) return@forEach

            val resolvedFromType = simFrom?.nodeType ?: if (fromKey == "self") "SELF" else fromType
            val resolvedToType = simTo?.nodeType ?: if (toKey == "self") "SELF" else toType
            val resolvedFromLabel = when {
                fromKey == "self" -> profileName.trim().ifBlank { "Tuan" }
                simFrom != null -> simFrom.label
                else -> fromLabelRaw.take(64)
            }
            val resolvedToLabel = when {
                toKey == "self" -> profileName.trim().ifBlank { "Tuan" }
                simTo != null -> simTo.label
                else -> toLabelRaw.take(64)
            }
            val safeConf = t.confidence.coerceIn(0.3f, 1.0f)

            val fromId = kgDao.upsertNode(
                nodeKey = fromKey,
                nodeType = resolvedFromType,
                label = resolvedFromLabel,
                summary = "",
                salienceBoost = if (fromKey == "self") 1.0f else 0.15f,
                sourceType = sourceType,
                nowMs = now,
                confidence = safeConf,
                sourceRef = sourceRef,
                confirmNode = isManual
            )
            if (simFrom == null && existingNodes.none { it.nodeKey == fromKey }) {
                kgDao.getNodeById(fromId)?.let { existingNodes.add(it) }
            }

            val toId = kgDao.upsertNode(
                nodeKey = toKey,
                nodeType = resolvedToType,
                label = resolvedToLabel,
                summary = "",
                salienceBoost = if (toKey == "self") 1.0f else 0.18f,
                sourceType = sourceType,
                nowMs = now,
                confidence = safeConf,
                sourceRef = sourceRef,
                confirmNode = isManual
            )
            if (simTo == null && existingNodes.none { it.nodeKey == toKey }) {
                kgDao.getNodeById(toId)?.let { existingNodes.add(it) }
            }

            val replaceTarget = t.replacesLabel.trim()
            val supersededCount = if (replaceTarget.length >= 2) {
                supersedeMatchingEdges(
                    fromId = fromId,
                    newToId = toId,
                    newToLabel = resolvedToLabel,
                    replacesLabel = replaceTarget,
                    nowMs = now,
                    sourceType = sourceType,
                    sourceRef = sourceRef
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
                confidence = safeConf,
                nowMs = now,
                sourceType = sourceType,
                sourceRef = sourceRef,
                status = "active",
                confirmEdge = isManual
            )
        }
    }

    private suspend fun supersedeMatchingEdges(
        fromId: Long,
        newToId: Long,
        newToLabel: String,
        replacesLabel: String,
        nowMs: Long,
        sourceType: String = "chat",
        sourceRef: String = ""
    ): Int {
        val allNodes = kgDao.getAllNodes()
        val allEdges = kgDao.getAllEdges()
        val q = replacesLabel.lowercase()
        val matchedNodeIds = allNodes.filter { n ->
            n.id != newToId &&
                n.nodeType != "SELF" &&
                KnowledgeGraphHelpers.isActiveNode(n) &&
                (n.label.contains(q, ignoreCase = true) || n.nodeKey.endsWith(q.replace(Regex("\\s+"), "_")))
        }.map { it.id }.toSet()
        if (matchedNodeIds.isEmpty()) return 0

        var updated = 0
        val updatedEdgeIds = mutableSetOf<Long>()
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
                updatedEdgeIds.add(edge.id)
                updated++
            }
        }

        matchedNodeIds.forEach { oldNodeId ->
            kgDao.upsertEdge(
                fromNodeId = newToId,
                toNodeId = oldNodeId,
                relation = "MENGGANTIKAN",
                evidence = "Menggantikan pola sebelumnya",
                weightBoost = 0.6f,
                confidence = 0.9f,
                nowMs = nowMs,
                sourceType = sourceType,
                sourceRef = sourceRef,
                status = "historical",
                confirmEdge = false
            )
            val stillHasActiveEdge = allEdges.any { e ->
                e.id !in updatedEdgeIds &&
                    !KnowledgeGraphHelpers.isHistoricalEdge(e) &&
                    (e.fromNodeId == oldNodeId || e.toNodeId == oldNodeId)
            }
            if (!stillHasActiveEdge) {
                kgDao.updateNodeStatus(oldNodeId, "deprecated", nowMs)
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

    suspend fun deprecateNode(nodeId: Long, note: String = "") {
        val node = kgDao.getNodeById(nodeId) ?: return
        if (node.nodeType == "SELF") return
        val now = System.currentTimeMillis()
        kgDao.updateNodeStatus(nodeId, "deprecated", now)
        val edges = kgDao.getAllEdges().filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }
        edges.forEach { edge ->
            if (!KnowledgeGraphHelpers.isHistoricalEdge(edge)) {
                markEdgeHistorical(edge.id, note.ifBlank { "simpul usang" })
            }
        }
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
            weightBoost = 1.0f,
            sourceRef = "manual_ui"
        )
        return true
    }

    suspend fun updateNodeDetails(nodeId: Long, label: String, summary: String) {
        val cleanLabel = label.trim().take(64)
        if (cleanLabel.isEmpty()) return
        kgDao.updateNodeDetails(nodeId, cleanLabel, summary.trim().take(280), System.currentTimeMillis())
    }

    suspend fun validateNodeCorrect(nodeId: Long) {
        val node = kgDao.getNodeById(nodeId) ?: return
        val now = System.currentTimeMillis()
        kgDao.insertNode(
            node.copy(
                status = "active",
                validationState = "confirmed",
                confidence = 1.0f,
                salience = (node.salience + 0.1f).coerceIn(0.3f, 1.0f),
                lastConfirmedAt = now,
                lastSeenAt = now
            )
        )
    }

    suspend fun validateNodeWrong(nodeId: Long) {
        val node = kgDao.getNodeById(nodeId) ?: return
        if (node.nodeType == "SELF") return
        val now = System.currentTimeMillis()
        kgDao.updateNodeValidation(
            nodeId = nodeId,
            status = "deprecated",
            validationState = "rejected",
            confirmedAt = now
        )
        val edges = kgDao.getAllEdges().filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }
        edges.forEach { edge ->
            if (!KnowledgeGraphHelpers.isHistoricalEdge(edge)) {
                markEdgeHistorical(edge.id, "ditolak manual")
            }
        }
    }

    suspend fun replaceNodeWithNew(
        profileName: String,
        oldNodeId: Long,
        newType: String,
        newLabel: String,
        relationToSelf: String,
        evidence: String
    ): Boolean {
        val oldNode = kgDao.getNodeById(oldNodeId) ?: return false
        val cleanNew = newLabel.trim().take(64)
        if (cleanNew.isEmpty()) return false
        val normType = KnowledgeGraphHelpers.normalizeNodeType(newType.ifBlank { oldNode.nodeType })
        val now = System.currentTimeMillis()
        val selfId = ensureSelfNode(profileName)
        val newKey = KnowledgeGraphHelpers.canonicalNodeKey(normType, cleanNew)
        val newId = kgDao.upsertNode(
            nodeKey = newKey,
            nodeType = normType,
            label = cleanNew,
            summary = "",
            salienceBoost = 0.25f,
            sourceType = "manual",
            nowMs = now,
            confidence = 1.0f,
            sourceRef = "manual_replace:${oldNode.nodeKey}",
            confirmNode = true
        )
        if (newId != oldNode.id) {
            kgDao.upsertEdge(
                fromNodeId = selfId,
                toNodeId = newId,
                relation = KnowledgeGraphHelpers.normalizeRelation(relationToSelf),
                evidence = evidence.ifBlank { "Menggantikan ${oldNode.label}" }.take(240),
                weightBoost = 1.0f,
                confidence = 1.0f,
                nowMs = now,
                sourceType = "manual",
                sourceRef = "manual_replace",
                status = "active",
                confirmEdge = true
            )
            kgDao.upsertEdge(
                fromNodeId = newId,
                toNodeId = oldNode.id,
                relation = "MENGGANTIKAN",
                evidence = evidence.ifBlank { "Evolusi dari ${oldNode.label}" }.take(240),
                weightBoost = 0.7f,
                confidence = 1.0f,
                nowMs = now,
                sourceType = "manual",
                sourceRef = "manual_replace",
                status = "historical",
                confirmEdge = true
            )
            deprecateNode(oldNode.id, "diganti '$cleanNew'")
        }
        return true
    }

    suspend fun resolveNodeByTarget(target: String, includeDeprecated: Boolean = false): KgNodeEntity? {
        val clean = target.trim()
        if (clean.isEmpty()) return null
        val all = kgDao.getAllNodes().let { list ->
            if (includeDeprecated) list else list.filter { KnowledgeGraphHelpers.isActiveNode(it) }
        }
        all.firstOrNull { it.nodeKey.equals(clean, ignoreCase = true) }?.let { return it }
        clean.toLongOrNull()?.let { idNum ->
            all.firstOrNull { it.id == idNum }?.let { return it }
        }
        val exact = all.filter { it.label.equals(clean, ignoreCase = true) }
        if (exact.size == 1) return exact.first()
        val partial = all.filter { it.label.contains(clean, ignoreCase = true) }
        return if (partial.size == 1) partial.first() else null
    }

    suspend fun updateNodeByTag(
        target: String,
        newLabel: String = "",
        newSummary: String = "",
        newType: String = "",
        reason: String = ""
    ): Boolean {
        val node = resolveNodeByTarget(target) ?: return false
        if (node.nodeType == "SELF") return false
        val now = System.currentTimeMillis()
        val resolvedType = if (newType.isNotBlank()) KnowledgeGraphHelpers.normalizeNodeType(newType) else node.nodeType
        val resolvedLabel = newLabel.trim().take(64).ifBlank { node.label }
        val resolvedSummary = when {
            newSummary.isNotBlank() -> newSummary.trim().take(280)
            reason.isNotBlank() && node.summary.isBlank() -> reason.trim().take(280)
            else -> node.summary
        }
        kgDao.insertNode(
            node.copy(
                nodeType = resolvedType,
                label = resolvedLabel,
                summary = resolvedSummary,
                lastConfirmedAt = now,
                lastSeenAt = now
            )
        )
        return true
    }

    suspend fun mergeNodes(
        sourceTarget: String,
        destinationTarget: String,
        reason: String = ""
    ): Boolean {
        val src = resolveNodeByTarget(sourceTarget) ?: return false
        val dst = resolveNodeByTarget(destinationTarget) ?: return false
        if (src.id == dst.id || src.nodeType == "SELF") return false
        val now = System.currentTimeMillis()
        val allEdges = kgDao.getAllEdges()
        allEdges.filter { it.fromNodeId == src.id || it.toNodeId == src.id }.forEach { edge ->
            if (edge.relation == "MENGGANTIKAN") return@forEach
            val newFrom = if (edge.fromNodeId == src.id) dst.id else edge.fromNodeId
            val newTo = if (edge.toNodeId == src.id) dst.id else edge.toNodeId
            if (newFrom != newTo && !KnowledgeGraphHelpers.isHistoricalEdge(edge)) {
                kgDao.upsertEdge(
                    fromNodeId = newFrom,
                    toNodeId = newTo,
                    relation = edge.relation,
                    evidence = edge.evidence,
                    weightBoost = (edge.weight * 0.5f).coerceIn(0.4f, 1.5f),
                    confidence = edge.confidence,
                    nowMs = now,
                    sourceType = edge.sourceType,
                    sourceRef = edge.sourceRef.ifBlank { "merge:${src.nodeKey}" },
                    status = "active",
                    confirmEdge = false
                )
            }
            if (!KnowledgeGraphHelpers.isHistoricalEdge(edge)) {
                markEdgeHistorical(edge.id, "digabung ke '${dst.label}'")
            }
        }
        val mergedSummary = listOf(dst.summary, src.summary)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" • ")
            .take(280)
        kgDao.insertNode(
            dst.copy(
                summary = mergedSummary,
                confidence = maxOf(dst.confidence, src.confidence).coerceIn(0.3f, 1.0f),
                salience = maxOf(dst.salience, src.salience).coerceIn(0.2f, 1.0f),
                lastConfirmedAt = now,
                lastSeenAt = now
            )
        )
        kgDao.upsertEdge(
            fromNodeId = dst.id,
            toNodeId = src.id,
            relation = "MENGGANTIKAN",
            evidence = reason.ifBlank { "Digabung dengan '${src.label}'" }.take(240),
            weightBoost = 0.6f,
            confidence = 0.95f,
            nowMs = now,
            sourceType = "manual",
            sourceRef = "kg_merge",
            status = "historical",
            confirmEdge = true
        )
        kgDao.updateNodeStatus(src.id, "deprecated", now)
        return true
    }

    /**
     * Memadatkan simpul berstatus 'deprecated' yang usianya >= olderThanDays menjadi
     * ringkasan riwayat per tipe tanpa pernah menyentuh simpul inti SELF dan NILAI.
     */
    suspend fun compactOldDeprecatedNodes(olderThanDays: Int = 90): Int {
        val now = System.currentTimeMillis()
        val cutoffMs = now - olderThanDays.coerceAtLeast(1) * 86_400_000L
        val allNodes = kgDao.getAllNodes()
        val candidates = allNodes.filter { n ->
            n.status == "deprecated" &&
                n.nodeType !in setOf("SELF", "NILAI") &&
                n.lastSeenAt < cutoffMs
        }
        if (candidates.size < 3) return 0

        val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val zone = ZoneId.systemDefault()
        var compactedCount = 0
        candidates.groupBy { it.nodeType }.forEach { (type, group) ->
            if (group.size < 2) return@forEach
            val summaryKey = "konteks:riwayat_padat_${type.lowercase()}"
            val itemsDesc = group.take(15).joinToString("; ") { n ->
                val d = runCatching {
                    Instant.ofEpochMilli(n.lastSeenAt).atZone(zone).toLocalDate().format(dateFmt)
                }.getOrDefault("")
                if (d.isNotBlank()) "${n.label} ($d)" else n.label
            }
            val existingSummary = kgDao.getNodeByKey(summaryKey)
            val combinedSummary = if (existingSummary != null && existingSummary.summary.isNotBlank()) {
                "${existingSummary.summary}; $itemsDesc"
            } else {
                "Riwayat $type lama: $itemsDesc"
            }.take(300)
            kgDao.insertNode(
                KgNodeEntity(
                    id = existingSummary?.id ?: 0L,
                    nodeKey = summaryKey,
                    nodeType = "KONTEKS",
                    label = "Riwayat $type Lama",
                    summary = combinedSummary,
                    salience = 0.25f,
                    sourceType = "system_compact",
                    firstSeenAt = existingSummary?.firstSeenAt ?: now,
                    lastSeenAt = now,
                    status = "compacted",
                    confidence = 0.8f,
                    lastConfirmedAt = now,
                    sourceRef = "compaction",
                    validationState = "confirmed"
                )
            )
            group.forEach { n ->
                kgDao.updateNodeStatus(n.id, "compacted", now)
                compactedCount++
            }
        }
        return compactedCount
    }

    suspend fun getActiveNodeHints(limit: Int = 35): List<KgNodeEntity> {
        return kgDao.getAllNodes()
            .filter { KnowledgeGraphHelpers.isActiveNode(it) && it.nodeType != "SELF" }
            .take(limit)
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
            val histTag = if (KnowledgeGraphHelpers.isHistoricalEdge(edge) || !KnowledgeGraphHelpers.isActiveNode(from) || !KnowledgeGraphHelpers.isActiveNode(to)) "[HISTORIS] " else ""
            "- $histTag[${from.nodeType}] ${from.label} --(${edge.relation})--> [${to.nodeType}] ${to.label} (kekuatan ${"%.1f".format(edge.weight)})$ev"
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
