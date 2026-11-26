package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kg_nodes",
    indices = [Index(value = ["nodeKey"], unique = true)]
)
data class KgNodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val nodeKey: String,
    val nodeType: String,
    val label: String,
    val summary: String = "",
    val salience: Float = 0.6f,
    val sourceType: String = "chat",
    val firstSeenAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val status: String = "active", // "active" | "deprecated" | "compacted"
    val confidence: Float = 0.8f,
    val lastConfirmedAt: Long = System.currentTimeMillis(),
    val sourceRef: String = "",
    val validationState: String = "unverified" // "unverified" | "confirmed" | "rejected"
)

@Entity(
    tableName = "kg_edges",
    indices = [
        Index(value = ["fromNodeId"]),
        Index(value = ["toNodeId"]),
        Index(value = ["fromNodeId", "toNodeId", "relation"], unique = true)
    ]
)
data class KgEdgeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fromNodeId: Long,
    val toNodeId: Long,
    val relation: String,
    val evidence: String = "",
    val weight: Float = 1.0f,
    val confidence: Float = 0.8f,
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "active", // "active" | "historical"
    val createdAt: Long = System.currentTimeMillis(),
    val lastConfirmedAt: Long = System.currentTimeMillis(),
    val sourceType: String = "chat",
    val sourceRef: String = ""
)
