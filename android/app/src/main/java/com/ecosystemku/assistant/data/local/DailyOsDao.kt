package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyOsDao {
    @Query("SELECT * FROM dailyos_habits ORDER BY sortOrder ASC")
    fun observeHabits(): Flow<List<DailyOsHabitEntity>>

    @Query("SELECT * FROM dailyos_todos ORDER BY sortOrder ASC")
    fun observeTodos(): Flow<List<DailyOsTodoEntity>>

    @Query("SELECT * FROM dailyos_habits ORDER BY sortOrder ASC")
    suspend fun getAllHabits(): List<DailyOsHabitEntity>

    @Query("SELECT * FROM dailyos_todos ORDER BY sortOrder ASC")
    suspend fun getAllTodos(): List<DailyOsTodoEntity>

    @Query("SELECT * FROM dailyos_habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: String): DailyOsHabitEntity?

    @Query("SELECT * FROM dailyos_todos WHERE id = :id LIMIT 1")
    suspend fun getTodoById(id: String): DailyOsTodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: DailyOsHabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<DailyOsHabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodo(todo: DailyOsTodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodos(todos: List<DailyOsTodoEntity>)

    @Query("DELETE FROM dailyos_todos WHERE id = :id")
    suspend fun deleteTodoById(id: String)

    @Query("DELETE FROM dailyos_habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Query("DELETE FROM dailyos_habits")
    suspend fun clearHabits()

    @Query("DELETE FROM dailyos_todos")
    suspend fun clearTodos()

    @Transaction
    suspend fun replaceAllData(habits: List<DailyOsHabitEntity>, todos: List<DailyOsTodoEntity>) {
        clearHabits()
        clearTodos()
        if (habits.isNotEmpty()) upsertHabits(habits)
        if (todos.isNotEmpty()) upsertTodos(todos)
    }
}
