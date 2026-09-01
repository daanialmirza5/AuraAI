package com.aura.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val detail: String,
    val tag: String,
    val sortOrder: Int,
)
