package com.aura.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isOn: Boolean,
    val sortOrder: Int,
)
