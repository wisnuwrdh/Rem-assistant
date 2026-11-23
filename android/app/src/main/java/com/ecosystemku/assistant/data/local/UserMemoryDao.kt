package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMemoryDao {
    @Query("SELECT * FROM user_memories ORDER BY updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories ORDER BY confidence DESC, updatedAt DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int = 24): List<UserMemoryEntity>

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
            insertMemory(
                existing.copy(
                    category = category,
                    factValue = factValue,
                    confidence = confidence,
                    sourceMessageId = sourceMessageId ?: existing.sourceMessageId,
                    updatedAt = now
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
                    updatedAt = now
                )
            )
        }
    }

    @Query("SELECT * FROM user_memories ORDER BY updatedAt ASC, id ASC")
    suspend fun getAllMemories(): List<UserMemoryEntity>

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
