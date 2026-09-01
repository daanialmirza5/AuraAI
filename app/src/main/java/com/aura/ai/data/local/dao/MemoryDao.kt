package com.aura.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.ai.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(memories: List<MemoryEntity>)

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM memories")
    suspend fun deleteAll()
}
