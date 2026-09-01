package com.aura.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.ai.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun count(): Int
}
