package com.aura.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.ai.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(devices: List<DeviceEntity>)

    @Query("SELECT COUNT(*) FROM devices")
    suspend fun count(): Int

    @Query("UPDATE devices SET isOn = NOT isOn WHERE id = :id")
    suspend fun toggle(id: String)
}
