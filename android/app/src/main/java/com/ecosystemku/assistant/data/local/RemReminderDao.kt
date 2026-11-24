package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RemReminderDao {
    @Query("SELECT * FROM rem_reminders WHERE fired = 0 AND triggerAtMs >= :nowMs ORDER BY triggerAtMs ASC")
    fun observeActive(nowMs: Long): Flow<List<RemReminderEntity>>

    @Query("SELECT * FROM rem_reminders WHERE fired = 0 AND triggerAtMs >= :nowMs ORDER BY triggerAtMs ASC")
    suspend fun getActive(nowMs: Long): List<RemReminderEntity>

    @Query("SELECT * FROM rem_reminders ORDER BY triggerAtMs DESC")
    suspend fun getAll(): List<RemReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: RemReminderEntity): Long

    @Query("UPDATE rem_reminders SET fired = 1 WHERE id = :id")
    suspend fun markFired(id: Long)

    @Query("DELETE FROM rem_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM rem_reminders")
    suspend fun clearAll()
}
