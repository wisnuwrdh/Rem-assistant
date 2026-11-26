package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMemoryDao {
    @Query("SELECT * FROM user_memories ORDER BY CASE WHEN status = 'active' THEN 0 ELSE 1 END, updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories WHERE status = 'active' ORDER BY confidence DESC, updatedAt DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int = 24): List<UserMemoryEntity>

    @Query("SELECT * FROM user_memories WHERE status = 'active' ORDER BY updatedAt ASC, id ASC")
    suspend fun getActiveMemories(): List<UserMemoryEntity>

    @Query("SELECT * FROM user_memories WHERE factKey = :factKey LIMIT 1")
    suspend fun getByFactKey(factKey: String): UserMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemoryEntity): Long

    @Transaction
    suspend fun upsertByFactKey(
        category: String,
        factKey: String,
        factValue: String,
        confidence: Float,
        sourceMessageId: Long?
    ) {
        val existing = getByFactKey(factKey)
        val now = System.currentTimeMillis()
        if (existing != null) {
            if (existing.status == "active" && !existing.factValue.equals(factValue.trim(), ignoreCase = true)) {
                insertMemory(
                    UserMemoryEntity(
                        id = 0L,
                        category = existing.category,
                        factKey = "${factKey}__hist_${now}".take(80),
                        factValue = existing.factValue,
                        confidence = existing.confidence,
                        sourceMessageId = existing.sourceMessageId,
                        updatedAt = existing.updatedAt,
                        status = "deprecated",
                        createdAt = if (existing.createdAt > 0L) existing.createdAt else existing.updatedAt,
                        deprecatedAt = now,
                        supersededBy = factKey
                    )
                )
            }
            insertMemory(
                existing.copy(
                    category = category,
                    factValue = factValue,
                    confidence = confidence,
                    sourceMessageId = sourceMessageId ?: existing.sourceMessageId,
                    updatedAt = now,
                    status = "active",
                    deprecatedAt = null,
                    supersededBy = null
                )
            )
        } else {
            insertMemory(
                UserMemoryEntity(
                    category = category,
                    factKey = factKey,
                    factValue = factValue,
                    confidence = confidence,
                    sourceMessageId = sourceMessageId,
                    updatedAt = now,
                    status = "active",
                    createdAt = now
                )
            )
        }
    }

    @Query("SELECT * FROM user_memories ORDER BY updatedAt ASC, id ASC")
    suspend fun getAllMemories(): List<UserMemoryEntity>

    @Query("UPDATE user_memories SET status = 'deprecated', deprecatedAt = :deprecatedAt, supersededBy = :supersededBy, updatedAt = :deprecatedAt WHERE id = :id")
    suspend fun deprecateById(id: Long, deprecatedAt: Long = System.currentTimeMillis(), supersededBy: String? = null)

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_memories")
    suspend fun clearAll()

    @Query("SELECT * FROM conversation_summaries ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun observeLatestSummaries(limit: Int = 5): Flow<List<ConversationSummaryEntity>>

    @Query("SELECT * FROM conversation_summaries ORDER BY createdAt DESC, id DESC LIMIT :limit")
    suspend fun getLatestSummaries(limit: Int = 5): List<ConversationSummaryEntity>

    @Query("SELECT * FROM conversation_summaries ORDER BY createdAt ASC, id ASC")
    suspend fun getAllSummaries(): List<ConversationSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: ConversationSummaryEntity): Long

    @Query("DELETE FROM conversation_summaries WHERE id = :id")
    suspend fun deleteSummaryById(id: Long)

    @Query("DELETE FROM conversation_summaries")
    suspend fun clearSummaries()
}
