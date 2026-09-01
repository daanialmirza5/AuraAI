package com.aura.ai.data.repository

import com.aura.ai.data.local.dao.DeviceDao
import com.aura.ai.data.local.entity.DeviceEntity
import com.aura.ai.domain.model.Device
import com.aura.ai.domain.model.DeviceIcon
import com.aura.ai.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private fun DeviceEntity.toDomain() =
    Device(
        id = id,
        name = name,
        icon =
            when (iconKey) {
                "bulb" -> DeviceIcon.Bulb
                "temp" -> DeviceIcon.Thermostat
                "lock" -> DeviceIcon.Lock
                "cam" -> DeviceIcon.Camera
                else -> DeviceIcon.Speaker
            },
        isOn = isOn,
        meta = meta,
    )

@Singleton
class DeviceRepositoryImpl
    @Inject
    constructor(
        private val dao: DeviceDao,
    ) : DeviceRepository {
        override fun observeDevices(): Flow<List<Device>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun toggleDevice(id: String) = dao.toggle(id)
    }
