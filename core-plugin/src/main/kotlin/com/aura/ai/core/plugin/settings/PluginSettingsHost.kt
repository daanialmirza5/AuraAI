package com.aura.ai.core.plugin.settings

import com.aura.ai.plugin.api.PluginSettingDescriptor
import com.aura.ai.plugin.api.PluginSettings
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** The process-wide, namespaced-by-plugin-id store behind every plugin's
 *  [com.aura.ai.plugin.api.PluginContext.settings] — same in-memory-this-phase honesty as
 *  [com.aura.ai.core.plugin.storage.PluginStorageHost]. [get] falls back to the declared
 *  [PluginSettingDescriptor.defaultValue] for a key that was never explicitly [PluginSettings.set]. */
@Singleton
class PluginSettingsHost
    @Inject
    constructor() {
        private val declaredByPlugin = ConcurrentHashMap<String, List<PluginSettingDescriptor>>()
        private val valuesByPlugin = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

        fun scopedTo(pluginId: String): PluginSettings =
            object : PluginSettings {
                override fun declare(descriptors: List<PluginSettingDescriptor>) {
                    declaredByPlugin[pluginId] = descriptors
                }

                override fun descriptors(): List<PluginSettingDescriptor> = declaredByPlugin[pluginId] ?: emptyList()

                override suspend fun get(key: String): String? {
                    val explicit = valuesByPlugin[pluginId]?.get(key)
                    if (explicit != null) return explicit
                    return declaredByPlugin[pluginId]?.firstOrNull { it.key == key }?.defaultValue
                }

                override suspend fun set(
                    key: String,
                    value: String,
                ) {
                    valuesByPlugin.computeIfAbsent(pluginId) { ConcurrentHashMap() }[key] = value
                }
            }
    }
