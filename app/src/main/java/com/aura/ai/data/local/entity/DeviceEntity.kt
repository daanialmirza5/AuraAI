package com.aura.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val isOn: Boolean,
    val meta: String,
    val sortOrder: Int,
)
