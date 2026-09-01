package com.aura.ai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Indexed on [timestampMillis] — every read (`ChatMessageDao`) orders by it, and this is the
 *  one table with unbounded, ever-growing row count (nothing prunes chat history), so an
 *  unindexed sort would get more expensive, not just constant, over a long-lived install. */
@Entity(tableName = "chat_messages", indices = [Index("timestampMillis")])
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sender: String,
    val text: String,
    val timestampMillis: Long,
)
