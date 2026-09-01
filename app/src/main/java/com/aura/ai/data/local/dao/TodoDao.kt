package com.aura.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.ai.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(todos: List<TodoEntity>)

    @Query("SELECT COUNT(*) FROM todos")
    suspend fun count(): Int

    @Query("UPDATE todos SET done = NOT done WHERE id = :id")
    suspend fun toggleDone(id: String)
}
