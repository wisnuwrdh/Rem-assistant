package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeGraphDao {
    @Query("SELECT * FROM kg_nodes ORDER BY salience DESC, lastSeenAt DESC")
    fun observeNodes(): Flow<List<KgNodeEntity>>

    @Query("SELECT * FROM kg_edges ORDER BY weight DESC, updatedAt DESC")
    fun observeEdges(): Flow<List<KgEdgeEntity>>

    @Query("SELECT * FROM kg_nodes ORDER BY salience DESC, lastSeenAt DESC")
    suspend fun getAllNodes(): List<KgNodeEntity>

    @Query("SELECT * FROM kg_edges ORDER BY weight DESC, updatedAt DESC")
    suspend fun getAllEdges(): List<KgEdgeEntity>

    @Query("SELECT * FROM kg_nodes WHERE nodeKey = :nodeKey LIMIT 1")
    suspend fun getNodeByKey(nodeKey: String): KgNodeEntity?

    @Query("SELECT * FROM kg_nodes WHERE id = :id LIMIT 1")
    suspend fun getNodeById(id: Long): KgNodeEntity?

    @Query("SELECT * FROM kg_edges WHERE id = :edgeId LIMIT 1")
    suspend fun getEdgeById(edgeId: Long): KgEdgeEntity?

    @Query("SELECT * FROM kg_edges WHERE fromNodeId = :fromId AND toNodeId = :toId AND relation = :relation LIMIT 1")
    suspend fun getEdgeByTriple(fromId: Long, toId: Long, relation: String): KgEdgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: KgNodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: KgEdgeEntity): Long

    @Query("UPDATE kg_edges SET weight = :weight, evidence = :evidence, updatedAt = :updatedAt WHERE id = :edgeId")
    suspend fun updateEdgeEvolution(edgeId: Long, weight: Float, evidence: String, updatedAt: Long)

    @Query("UPDATE kg_nodes SET label = :label, summary = :summary, lastSeenAt = :updatedAt WHERE id = :nodeId")
    suspend fun updateNodeDetails(nodeId: Long, label: String, summary: String, updatedAt: Long)

    @Query("DELETE FROM kg_edges WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
    suspend fun deleteEdgesForNode(nodeId: Long)

    @Query("DELETE FROM kg_nodes WHERE id = :nodeId")
    suspend fun deleteNodeById(nodeId: Long)

    @Query("DELETE FROM kg_edges WHERE id = :edgeId")
    suspend fun deleteEdgeById(edgeId: Long)

    @Query("DELETE FROM kg_edges")
    suspend fun clearAllEdges()

    @Query("DELETE FROM kg_nodes")
    suspend fun clearAllNodes()

    @Transaction
    suspend fun deleteNodeAndEdges(nodeId: Long) {
        deleteEdgesForNode(nodeId)
        deleteNodeById(nodeId)
    }

    @Transaction
    suspend fun clearAll() {
        clearAllEdges()
        clearAllNodes()
    }

    @Transaction
    suspend fun upsertNode(
        nodeKey: String,
        nodeType: String,
        label: String,
        summary: String,
        salienceBoost: Float,
        sourceType: String,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val existing = getNodeByKey(nodeKey)
        return if (existing != null) {
            val nextSalience = (existing.salience + salienceBoost).coerceIn(0.1f, 1.0f)
            val nextSummary = summary.ifBlank { existing.summary }
            insertNode(
                existing.copy(
                    label = label.ifBlank { existing.label },
                    summary = nextSummary,
                    salience = nextSalience,
                    lastSeenAt = nowMs
                )
            )
            existing.id
        } else {
            insertNode(
                KgNodeEntity(
                    nodeKey = nodeKey,
                    nodeType = nodeType,
                    label = label,
                    summary = summary,
                    salience = salienceBoost.coerceIn(0.2f, 1.0f),
                    sourceType = sourceType,
                    firstSeenAt = nowMs,
                    lastSeenAt = nowMs
                )
            )
        }
    }

    @Transaction
    suspend fun upsertEdge(
        fromNodeId: Long,
        toNodeId: Long,
        relation: String,
        evidence: String,
        weightBoost: Float,
        confidence: Float,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val existing = getEdgeByTriple(fromNodeId, toNodeId, relation)
        return if (existing != null) {
            val nextWeight = (existing.weight + weightBoost).coerceIn(0.3f, 5.0f)
            val nextConf = maxOf(existing.confidence, confidence).coerceIn(0.3f, 1.0f)
            val nextEvidence = evidence.ifBlank { existing.evidence }
            insertEdge(
                existing.copy(
                    evidence = nextEvidence,
                    weight = nextWeight,
                    confidence = nextConf,
                    updatedAt = nowMs
                )
            )
            existing.id
        } else {
            insertEdge(
                KgEdgeEntity(
                    fromNodeId = fromNodeId,
                    toNodeId = toNodeId,
                    relation = relation,
                    evidence = evidence,
                    weight = weightBoost.coerceIn(0.5f, 5.0f),
                    confidence = confidence.coerceIn(0.3f, 1.0f),
                    updatedAt = nowMs
                )
            )
        }
    }
}
