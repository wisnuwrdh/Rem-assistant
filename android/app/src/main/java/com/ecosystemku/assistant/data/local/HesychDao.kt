package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HesychDao {
    @Query("SELECT * FROM hesych_items ORDER BY favorite DESC, updatedAt DESC, id DESC")
    fun observeItems(): Flow<List<HesychItemEntity>>

    @Query("SELECT * FROM hesych_items ORDER BY favorite DESC, updatedAt DESC, id DESC")
    suspend fun getAllItems(): List<HesychItemEntity>

    @Query("SELECT * FROM hesych_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): HesychItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: HesychItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<HesychItemEntity>)

    @Query("DELETE FROM hesych_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("SELECT * FROM hesych_history WHERE itemId = :itemId ORDER BY changedAt DESC, hid DESC")
    fun observeHistoryForItem(itemId: Long): Flow<List<HesychHistoryEntity>>

    @Query("SELECT * FROM hesych_history WHERE itemId = :itemId ORDER BY changedAt DESC, hid DESC")
    suspend fun getHistoryForItem(itemId: Long): List<HesychHistoryEntity>

    @Query("SELECT * FROM hesych_history ORDER BY changedAt DESC, hid DESC")
    suspend fun getAllHistory(): List<HesychHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HesychHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryAll(entries: List<HesychHistoryEntity>)

    @Query("DELETE FROM hesych_history WHERE itemId = :itemId")
    suspend fun deleteHistoryForItem(itemId: Long)

    @Query(
        """
        DELETE FROM hesych_history
        WHERE itemId = :itemId AND hid NOT IN (
            SELECT hid FROM hesych_history
            WHERE itemId = :itemId
            ORDER BY changedAt DESC, hid DESC
            LIMIT :keepCount
        )
        """
    )
    suspend fun trimHistoryForItem(itemId: Long, keepCount: Int = 10)

    @Query("SELECT * FROM hesych_meta WHERE key = :key LIMIT 1")
    suspend fun getMeta(key: String): HesychMetaEntity?

    @Query("SELECT * FROM hesych_meta")
    suspend fun getAllMeta(): List<HesychMetaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: HesychMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetaAll(entries: List<HesychMetaEntity>)

    @Query("DELETE FROM hesych_meta WHERE key = :key")
    suspend fun deleteMeta(key: String)

    @Query("DELETE FROM hesych_items")
    suspend fun clearItems()

    @Query("DELETE FROM hesych_history")
    suspend fun clearHistory()

    @Query("DELETE FROM hesych_meta")
    suspend fun clearMeta()

    @Transaction
    suspend fun clearAll() {
        clearHistory()
        clearItems()
        clearMeta()
    }

    @Transaction
    suspend fun replaceAllData(
        items: List<HesychItemEntity>,
        history: List<HesychHistoryEntity>,
        meta: List<HesychMetaEntity>
    ) {
        clearAll()
        if (items.isNotEmpty()) upsertItems(items)
        if (history.isNotEmpty()) insertHistoryAll(history)
        if (meta.isNotEmpty()) upsertMetaAll(meta)
    }
}
