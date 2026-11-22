package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KronikDao {
    @Query(
        """
        SELECT * FROM kronik_entries
        WHERE isArchived = 0
        ORDER BY pinned DESC, date DESC, updatedAt DESC
        """
    )
    fun observeVisibleEntries(): Flow<List<KronikEntryEntity>>

    @Query(
        """
        SELECT * FROM kronik_entries
        WHERE isArchived = 0
        ORDER BY pinned DESC, date DESC, updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getVisibleEntries(limit: Int = 20): List<KronikEntryEntity>

    @Query("SELECT * FROM kronik_entries ORDER BY pinned DESC, date DESC, updatedAt DESC")
    suspend fun getAllEntries(): List<KronikEntryEntity>

    @Query("SELECT * FROM kronik_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): KronikEntryEntity?

    @Query("SELECT * FROM kronik_entries WHERE date = :date AND isArchived = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getByDate(date: String): KronikEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: KronikEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<KronikEntryEntity>)

    @Query("UPDATE kronik_entries SET isArchived = 1, archivedAt = :archivedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveById(id: String, archivedAt: String, updatedAt: String)

    @Query("DELETE FROM kronik_entries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM kronik_entries")
    suspend fun clearAll()
}
