package com.aura.ai.core.plugin.registry

import com.aura.ai.core.plugin.model.PluginRecord
import com.aura.ai.core.plugin.model.PluginState
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPluginRegistry
    @Inject
    constructor() : PluginRegistry {
        private val records = ConcurrentHashMap<String, PluginRecord>()

        override fun register(record: PluginRecord) {
            records[record.manifest.id] = record
        }

        override fun updateState(
            pluginId: String,
            state: PluginState,
            lastError: String?,
        ) {
            records.computeIfPresent(pluginId) { _, existing -> existing.copy(state = state, lastError = lastError) }
        }

        override fun get(pluginId: String): PluginRecord? = records[pluginId]

        override fun all(): List<PluginRecord> = records.values.toList()

        override fun unregister(pluginId: String) {
            records.remove(pluginId)
        }
    }
