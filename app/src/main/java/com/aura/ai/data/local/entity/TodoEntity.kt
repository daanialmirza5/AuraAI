package com.aura.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val meta: String,
    val done: Boolean,
    val priority: String,
    val sortOrder: Int,
)
