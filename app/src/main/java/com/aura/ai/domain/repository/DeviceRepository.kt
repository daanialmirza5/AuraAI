package com.aura.ai.domain.repository

import com.aura.ai.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>

    suspend fun toggleDevice(id: String)
}
