package com.aura.ai.domain.model

enum class DeviceIcon { Bulb, Thermostat, Lock, Camera, Speaker }

data class Device(
    val id: String,
    val name: String,
    val icon: DeviceIcon,
    val isOn: Boolean,
    val meta: String,
)
