package com.aura.ai.core.reasoning.context

/** [isLow] is a policy judgment (see the real implementation for the threshold), not just a raw
 *  reading — every caller should be able to ask "is battery a constraint right now?" without
 *  re-deriving that threshold itself. */
data class BatteryStatus(
    val percent: Int,
    val isCharging: Boolean,
    val isLow: Boolean,
)

/**
 * "Battery awareness" — the device's current power state. Same provider-agnostic shape as
 * [PermissionChecker]/[NetworkStatusProvider]: `null` from [currentStatus] means "unknown," not
 * "the device has no battery." [UnknownBatteryStatusProvider] is the only implementation this
 * module ships; a real `BatteryManager`-backed one is bound at the `:app` layer.
 */
interface BatteryStatusProvider {
    fun currentStatus(): BatteryStatus?
}

class UnknownBatteryStatusProvider : BatteryStatusProvider {
    override fun currentStatus(): BatteryStatus? = null
}
