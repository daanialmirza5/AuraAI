package com.aura.ai.core.plugin.registry

import com.aura.ai.core.plugin.model.PluginRecord
import com.aura.ai.core.plugin.model.PluginState

/** The host-wide catalog of every known plugin, at whatever [PluginState] it's currently in — the
 *  plugin-system analogue of `com.aura.ai.core.agents.registry.AgentRegistry`. `plugin-runtime`
 *  is the only thing that mutates this in the normal path. */
interface PluginRegistry {
    fun register(record: PluginRecord)

    fun updateState(
        pluginId: String,
        state: PluginState,
        lastError: String? = null,
    )

    fun get(pluginId: String): PluginRecord?

    fun all(): List<PluginRecord>

    fun unregister(pluginId: String)
}
