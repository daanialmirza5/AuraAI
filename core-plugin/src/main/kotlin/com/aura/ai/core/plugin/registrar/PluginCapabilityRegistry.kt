package com.aura.ai.core.plugin.registrar

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The host-side store behind [com.aura.ai.plugin.api.PluginCapabilityRegistrar] — deliberately
 * independent of `core-capabilities.CapabilityRegistry` (see that interface's own doc for why
 * plugin capabilities are a free-form, open vocabulary rather than the host's closed
 * `Capability` enum). Two `ConcurrentHashMap`s, each the inverse of the other, the same shape
 * `core-capabilities.DefaultCapabilityRegistry` uses for the same O(1)-both-directions reason.
 */
@Singleton
class PluginCapabilityRegistry
    @Inject
    constructor() {
        private val byPlugin = ConcurrentHashMap<String, MutableSet<String>>()
        private val byCapability = ConcurrentHashMap<String, MutableSet<String>>()

        fun register(
            pluginId: String,
            capabilityName: String,
        ) {
            byPlugin.computeIfAbsent(pluginId) { ConcurrentHashMap.newKeySet() }.add(capabilityName)
            byCapability.computeIfAbsent(capabilityName) { ConcurrentHashMap.newKeySet() }.add(pluginId)
        }

        fun unregister(
            pluginId: String,
            capabilityName: String,
        ) {
            byPlugin[pluginId]?.remove(capabilityName)
            byCapability[capabilityName]?.remove(pluginId)
        }

        fun unregisterAll(pluginId: String) {
            val capabilities = byPlugin.remove(pluginId) ?: return
            capabilities.forEach { byCapability[it]?.remove(pluginId) }
        }

        fun capabilitiesOf(pluginId: String): Set<String> = byPlugin[pluginId]?.toSet() ?: emptySet()

        fun pluginsFor(capabilityName: String): Set<String> = byCapability[capabilityName]?.toSet() ?: emptySet()
    }
