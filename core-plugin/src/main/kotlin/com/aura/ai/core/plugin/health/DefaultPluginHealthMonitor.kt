package com.aura.ai.core.plugin.health

import com.aura.ai.core.plugin.model.PluginState
import com.aura.ai.core.plugin.registry.PluginRegistry
import com.aura.ai.plugin.api.PluginHealthReport
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPluginHealthMonitor
    @Inject
    constructor(
        private val pluginRegistry: PluginRegistry,
    ) : PluginHealthMonitor {
        /** Only these states have a live [com.aura.ai.plugin.api.Plugin] instance it's meaningful to
         *  ask — before [PluginState.Loaded] there's nothing running yet; after [PluginState.Unloaded]
         *  there no longer is. */
        private val queryableStates = setOf(PluginState.Loaded, PluginState.Enabled, PluginState.Disabled)

        override suspend fun checkAll(): Map<String, PluginHealthReport> =
            pluginRegistry
                .all()
                .filter { it.state in queryableStates }
                .associate { it.manifest.id to it.plugin.health() }

        override suspend fun check(pluginId: String): PluginHealthReport? {
            val record = pluginRegistry.get(pluginId) ?: return null
            if (record.state !in queryableStates) return null
            return record.plugin.health()
        }
    }
