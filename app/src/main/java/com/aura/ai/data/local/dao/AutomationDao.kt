package com.aura.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.ai.data.local.entity.AutomationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<AutomationEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(automations: List<AutomationEntity>)

    @Query("SELECT COUNT(*) FROM automations")
    suspend fun count(): Int

    @Query("UPDATE automations SET isOn = NOT isOn WHERE id = :id")
    suspend fun toggle(id: String)
}
