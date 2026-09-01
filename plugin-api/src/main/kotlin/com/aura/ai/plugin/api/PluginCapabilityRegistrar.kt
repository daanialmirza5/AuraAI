package com.aura.ai.plugin.api

/**
 * "Dynamic Capability Registration." Deliberately a free-form `String` vocabulary, not
 * `core-capabilities.Capability` (a closed enum of the 11 capabilities AURA's own built-in agents
 * provide) — a plugin author can't predict, and shouldn't be constrained to, a fixed set decided
 * before their plugin existed. `core-plugin`'s `PluginCapabilityRegistry` tracks these
 * independently of the host's own capability registry; see `docs/PLUGIN_SDK.md` for the boundary
 * this creates (a plugin capability doesn't yet participate in
 * `core-orchestrator`'s agent-selection closure — a documented, honest gap, not a hidden one).
 */
interface PluginCapabilityRegistrar {
    fun register(capabilityName: String): PluginResult<Unit>

    fun unregister(capabilityName: String): PluginResult<Unit>
}
