package com.aura.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.ai.data.local.entity.WorkflowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows ORDER BY name ASC")
    fun observeAll(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :id")
    suspend fun get(id: String): WorkflowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workflow: WorkflowEntity)

    @Delete
    suspend fun delete(workflow: WorkflowEntity)

    @Query("UPDATE workflows SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(
        id: String,
        enabled: Boolean,
    )
}
