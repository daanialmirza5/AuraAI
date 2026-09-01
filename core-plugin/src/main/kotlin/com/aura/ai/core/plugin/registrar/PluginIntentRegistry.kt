package com.aura.ai.core.plugin.registrar

import com.aura.ai.plugin.api.PluginIntentDescriptor
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** The host-side store behind [com.aura.ai.plugin.api.PluginIntentRegistrar] — every plugin's
 *  registered [PluginIntentDescriptor]s, keyed by plugin id then intent id. The one place
 *  `com.aura.ai.core.plugin.intent.PluginAwareIntentRecognizer` reads from to know what a plugin
 *  might match before falling back to the built-in recognizer. */
@Singleton
class PluginIntentRegistry
    @Inject
    constructor() {
        private val byPlugin = ConcurrentHashMap<String, ConcurrentHashMap<String, PluginIntentDescriptor>>()

        fun register(
            pluginId: String,
            descriptor: PluginIntentDescriptor,
        ) {
            byPlugin.computeIfAbsent(pluginId) { ConcurrentHashMap() }[descriptor.id] = descriptor
        }

        fun unregister(
            pluginId: String,
            intentId: String,
        ) {
            byPlugin[pluginId]?.remove(intentId)
        }

        fun unregisterAll(pluginId: String) {
            byPlugin.remove(pluginId)
        }

        /** Every registered descriptor across every plugin, paired with the plugin that owns it. */
        fun all(): List<Pair<String, PluginIntentDescriptor>> =
            byPlugin.flatMap { (pluginId, descriptors) -> descriptors.values.map { pluginId to it } }
    }
