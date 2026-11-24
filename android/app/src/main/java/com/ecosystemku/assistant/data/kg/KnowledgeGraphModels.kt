package com.ecosystemku.assistant.data.kg

import com.ecosystemku.assistant.data.local.ConversationSummaryEntity
import com.ecosystemku.assistant.data.local.KgEdgeEntity
import com.ecosystemku.assistant.data.local.KgNodeEntity
import com.ecosystemku.assistant.data.local.UserMemoryEntity
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ExtractedTripleDto(
    val fromType: String = "SELF",
    val fromLabel: String = "Tuan",
    val relation: String = "TERKAIT",
    val toType: String = "POLA_PIKIR",
    val toLabel: String = "",
    val evidence: String = "",
    val confidence: Float = 0.8f,
    val replacesLabel: String = ""
)

data class KgTripleSummary(
    val fromNode: KgNodeEntity,
    val edge: KgEdgeEntity,
    val toNode: KgNodeEntity
)

@Serializable
data class DigitalTwinNodeDto(
    val nodeKey: String,
    val type: String,
    val label: String,
    val summary: String,
    val salience: Float,
    val sourceType: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long
)

@Serializable
data class DigitalTwinEdgeDto(
    val fromKey: String,
    val relation: String,
    val toKey: String,
    val evidence: String,
    val weight: Float,
    val confidence: Float,
    val updatedAt: Long,
    val isHistorical: Boolean = false
)

@Serializable
data class DigitalTwinDecisionPolicyDto(
    val subject: String,
    val relation: String,
    val target: String,
    val rationale: String,
    val strength: Float
)

@Serializable
data class DigitalTwinMemoryDto(
    val category: String,
    val key: String,
    val value: String,
    val confidence: Float
)

@Serializable
data class DigitalTwinStatsDto(
    val nodeCount: Int,
    val edgeCount: Int,
    val decisionPolicyCount: Int,
    val memoryCount: Int,
    val summaryCount: Int
)

@Serializable
data class DigitalTwinExportDto(
    val schema: String = "rem-digital-twin-kg-v1",
    val exportedAt: String,
    val profileName: String,
    val stats: DigitalTwinStatsDto,
    val nodes: List<DigitalTwinNodeDto>,
    val edges: List<DigitalTwinEdgeDto>,
    val decisionPolicies: List<DigitalTwinDecisionPolicyDto>,
    val flatMemories: List<DigitalTwinMemoryDto>,
    val sessionSummaries: List<String>
)

@Serializable
data class KgBackupNodeDto(
    val id: Long,
    val nodeKey: String,
    val nodeType: String,
    val label: String,
    val summary: String = "",
    val salience: Float = 0.6f,
    val sourceType: String = "chat",
    val firstSeenAt: Long = 0L,
    val lastSeenAt: Long = 0L
)

@Serializable
data class KgBackupEdgeDto(
    val fromKey: String,
    val toKey: String,
    val relation: String,
    val evidence: String = "",
    val weight: Float = 1.0f,
    val confidence: Float = 0.8f,
    val updatedAt: Long = 0L
)

@Serializable
data class KgBackupWrapperDto(
    val nodes: List<KgBackupNodeDto> = emptyList(),
    val edges: List<KgBackupEdgeDto> = emptyList()
)

object KnowledgeGraphHelpers {
    val VALID_NODE_TYPES = setOf(
        "SELF",
        "NILAI",
        "POLA_PIKIR",
        "KEBIASAAN",
        "TUJUAN",
        "EMOSI_PEMICU",
        "KEPUTUSAN",
        "MINAT",
        "ORANG",
        "KONTEKS"
    )

    val VALID_RELATIONS = setOf(
        "MENGUTAMAKAN",
        "MEMICU",
        "MENURUNKAN",
        "MENDUKUNG",
        "CENDERUNG_SAAT",
        "ALASANNYA",
        "MENGHINDARI",
        "TERCERMIN_DI",
        "KONSISTEN_PADA",
        "SEDANG_MEMBANGUN",
        "ALOKASI_UTAMA",
        "TERKAIT"
    )

    private val DECISION_POLICY_RELATIONS = setOf(
        "ALASANNYA",
        "CENDERUNG_SAAT",
        "MENGUTAMAKAN",
        "MENGHINDARI",
        "MEMICU",
        "MENURUNKAN"
    )

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun normalizeNodeType(raw: String): String {
        val clean = raw.trim().uppercase().replace(' ', '_')
        return if (clean in VALID_NODE_TYPES) clean else "POLA_PIKIR"
    }

    fun normalizeRelation(raw: String): String {
        val clean = raw.trim().uppercase().replace(' ', '_')
        return if (clean in VALID_RELATIONS) clean else "TERKAIT"
    }

    fun canonicalNodeKey(type: String, label: String): String {
        val normType = normalizeNodeType(type)
        val cleanLabel = label.trim().lowercase()
        if (normType == "SELF" || cleanLabel in setOf("tuan", "saya", "aku", "user", "diri sendiri", "self")) {
            return "self"
        }
        val slug = cleanLabel
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(56)
            .ifEmpty { "item" }
        return "${normType.lowercase()}:$slug"
    }

    fun selectRelevantTriples(
        nodes: List<KgNodeEntity>,
        edges: List<KgEdgeEntity>,
        userMessage: String,
        maxEdges: Int = 14
    ): List<KgTripleSummary> {
        if (nodes.isEmpty() || edges.isEmpty()) return emptyList()
        val nodeById = nodes.associateBy { it.id }

        val queryTokens = userMessage.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .distinct()

        val hop1SeedIds = if (queryTokens.isEmpty()) {
            emptySet()
        } else {
            nodes.filter { node ->
                if (node.nodeKey == "self") return@filter false
                val hay = "${node.label} ${node.summary}".lowercase()
                queryTokens.any { token -> hay.contains(token) }
            }.map { it.id }.toSet()
        }

        val hop2NodeIds = mutableSetOf<Long>().apply { addAll(hop1SeedIds) }
        if (hop1SeedIds.isNotEmpty()) {
            edges.forEach { edge ->
                if (edge.fromNodeId in hop1SeedIds) hop2NodeIds.add(edge.toNodeId)
                if (edge.toNodeId in hop1SeedIds) hop2NodeIds.add(edge.fromNodeId)
            }
        }

        val scored = edges.mapNotNull { edge ->
            val from = nodeById[edge.fromNodeId] ?: return@mapNotNull null
            val to = nodeById[edge.toNodeId] ?: return@mapNotNull null
            val baseScore = edge.weight * ((from.salience + to.salience) * 0.5f) * edge.confidence
            val hopBonus = when {
                edge.fromNodeId in hop1SeedIds || edge.toNodeId in hop1SeedIds -> 4.0f
                edge.fromNodeId in hop2NodeIds || edge.toNodeId in hop2NodeIds -> 1.6f
                else -> 0f
            }
            val factor = if (isHistoricalEdge(edge)) 0.25f else 1.0f
            Triple(KgTripleSummary(from, edge, to), (baseScore + hopBonus) * factor, edge.updatedAt)
        }

        return scored
            .sortedWith(compareByDescending<Triple<KgTripleSummary, Float, Long>> { it.second }.thenByDescending { it.third })
            .take(maxEdges)
            .map { it.first }
    }

    fun isHistoricalEdge(edge: KgEdgeEntity): Boolean {
        return edge.weight < 0.4f || edge.evidence.startsWith("[Historis")
    }

    fun searchTriples(
        nodes: List<KgNodeEntity>,
        edges: List<KgEdgeEntity>,
        query: String,
        maxResults: Int = 18
    ): List<KgTripleSummary> {
        val nodeById = nodes.associateBy { it.id }
        val clean = query.trim().lowercase()
        val matchingNodeIds = if (clean.isEmpty()) {
            nodes.map { it.id }.toSet()
        } else {
            nodes.filter {
                it.label.contains(clean, ignoreCase = true) ||
                    it.summary.contains(clean, ignoreCase = true) ||
                    it.nodeType.contains(clean, ignoreCase = true)
            }.map { it.id }.toSet()
        }

        return edges.mapNotNull { edge ->
            val from = nodeById[edge.fromNodeId] ?: return@mapNotNull null
            val to = nodeById[edge.toNodeId] ?: return@mapNotNull null
            val matches = clean.isEmpty() ||
                edge.fromNodeId in matchingNodeIds ||
                edge.toNodeId in matchingNodeIds ||
                edge.relation.contains(clean, ignoreCase = true) ||
                edge.evidence.contains(clean, ignoreCase = true)
            if (!matches) null else KgTripleSummary(from, edge, to)
        }.sortedByDescending { it.edge.weight * it.edge.confidence }.take(maxResults)
    }

    fun buildDigitalTwinJson(
        profileName: String,
        nodes: List<KgNodeEntity>,
        edges: List<KgEdgeEntity>,
        memories: List<UserMemoryEntity>,
        summaries: List<ConversationSummaryEntity>
    ): String {
        val nodeById = nodes.associateBy { it.id }
        val nodeDtos = nodes.map { n ->
            DigitalTwinNodeDto(
                nodeKey = n.nodeKey,
                type = n.nodeType,
                label = n.label,
                summary = n.summary,
                salience = n.salience,
                sourceType = n.sourceType,
                firstSeenAt = n.firstSeenAt,
                lastSeenAt = n.lastSeenAt
            )
        }
        val edgeDtos = edges.mapNotNull { e ->
            val from = nodeById[e.fromNodeId] ?: return@mapNotNull null
            val to = nodeById[e.toNodeId] ?: return@mapNotNull null
            DigitalTwinEdgeDto(
                fromKey = from.nodeKey,
                relation = e.relation,
                toKey = to.nodeKey,
                evidence = e.evidence,
                weight = e.weight,
                confidence = e.confidence,
                updatedAt = e.updatedAt,
                isHistorical = isHistoricalEdge(e)
            )
        }
        val policies = edges.mapNotNull { e ->
            if (isHistoricalEdge(e) || e.relation !in DECISION_POLICY_RELATIONS) return@mapNotNull null
            val from = nodeById[e.fromNodeId] ?: return@mapNotNull null
            val to = nodeById[e.toNodeId] ?: return@mapNotNull null
            DigitalTwinDecisionPolicyDto(
                subject = from.label,
                relation = e.relation,
                target = to.label,
                rationale = e.evidence,
                strength = e.weight
            )
        }
        val export = DigitalTwinExportDto(
            exportedAt = Instant.now().toString(),
            profileName = profileName.ifBlank { "Tuan" },
            stats = DigitalTwinStatsDto(
                nodeCount = nodeDtos.size,
                edgeCount = edgeDtos.size,
                decisionPolicyCount = policies.size,
                memoryCount = memories.size,
                summaryCount = summaries.size
            ),
            nodes = nodeDtos,
            edges = edgeDtos,
            decisionPolicies = policies,
            flatMemories = memories.map {
                DigitalTwinMemoryDto(it.category, it.factKey, it.factValue, it.confidence)
            },
            sessionSummaries = summaries.map { it.summaryText }
        )
        return json.encodeToString(export)
    }

    fun buildBackupDto(nodes: List<KgNodeEntity>, edges: List<KgEdgeEntity>): KgBackupWrapperDto {
        val nodeById = nodes.associateBy { it.id }
        return KgBackupWrapperDto(
            nodes = nodes.map {
                KgBackupNodeDto(
                    id = it.id,
                    nodeKey = it.nodeKey,
                    nodeType = it.nodeType,
                    label = it.label,
                    summary = it.summary,
                    salience = it.salience,
                    sourceType = it.sourceType,
                    firstSeenAt = it.firstSeenAt,
                    lastSeenAt = it.lastSeenAt
                )
            },
            edges = edges.mapNotNull { e ->
                val from = nodeById[e.fromNodeId] ?: return@mapNotNull null
                val to = nodeById[e.toNodeId] ?: return@mapNotNull null
                KgBackupEdgeDto(
                    fromKey = from.nodeKey,
                    toKey = to.nodeKey,
                    relation = e.relation,
                    evidence = e.evidence,
                    weight = e.weight,
                    confidence = e.confidence,
                    updatedAt = e.updatedAt
                )
            }
        )
    }
}
