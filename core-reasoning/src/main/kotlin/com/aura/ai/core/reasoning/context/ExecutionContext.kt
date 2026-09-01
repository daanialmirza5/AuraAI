package com.aura.ai.core.reasoning.context

import com.aura.ai.core.ai.ToolDescriptor

/**
 * "Execution Context" — the brief's own named component: a snapshot of everything about the
 * device and its environment a reasoning pass needs to check itself against. Captured once per
 * `ReasoningEngine.reason` call by `ExecutionContextProvider`, then threaded read-only through
 * every downstream component (`ConstraintEngine`, `DecisionEngine`, `ActionValidator`) — nothing
 * re-queries the device mid-reasoning, so every decision in one pass is judged against the exact
 * same facts.
 */
data class ExecutionContext(
    val availableTools: List<ToolDescriptor>,
    val connectivity: ConnectivityState,
    val battery: BatteryStatus?,
    /** Only the permissions a given reasoning pass actually asked about — see
     *  `ExecutionContextProvider.capture`. Not every Android permission the app might ever use. */
    val permissionState: Map<String, PermissionState>,
    val aiProviderAvailable: Boolean,
    val capturedAtMillis: Long,
) {
    val isOnline: Boolean get() = connectivity == ConnectivityState.Online

    /** Conservative on purpose: only [BatteryStatus.isLow] counts as a real constraint. A
     *  [battery] of `null` (unknown) is not treated as low — see [PermissionChecker]'s doc for why
     *  "unknown" and "confirmed bad" are kept distinct throughout this module. */
    val isBatteryConstrained: Boolean get() = battery?.let { it.isLow && !it.isCharging } == true

    fun hasTool(name: String): Boolean = availableTools.any { it.name == name }

    fun isGranted(permission: String): Boolean = permissionState[permission] == PermissionState.Granted
}
