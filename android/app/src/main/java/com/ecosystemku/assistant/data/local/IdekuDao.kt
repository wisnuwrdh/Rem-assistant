package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdekuDao {
    @Query("SELECT * FROM ideku_ideas ORDER BY id DESC")
    fun observeAll(): Flow<List<IdekuIdeaEntity>>

    @Query("SELECT * FROM ideku_ideas ORDER BY id DESC")
    suspend fun getAll(): List<IdekuIdeaEntity>

    @Query("SELECT * FROM ideku_ideas WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): IdekuIdeaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: IdekuIdeaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<IdekuIdeaEntity>)

    @Query("DELETE FROM ideku_ideas WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ideku_ideas")
    suspend fun clearAll()
}
