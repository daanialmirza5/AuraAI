package com.aura.ai.plugin.api

enum class PluginSettingType { String, Number, Boolean }

/** One user-configurable setting a plugin exposes — enough shape for a future generic settings
 *  UI to render this without knowing anything about the specific plugin. */
data class PluginSettingDescriptor(
    val key: String,
    val label: String,
    val type: PluginSettingType,
    val defaultValue: String,
    val description: String = "",
)

/**
 * "Plugin Settings" — distinct from [PluginStorage]: storage is for a plugin's own internal
 * state, settings are for values the *user* is meant to see and change. [declare] is normally
 * called once, in [Plugin.onLoad], so the host knows what settings this plugin has before any UI
 * ever needs to render them; [get] falls back to the declared [PluginSettingDescriptor.defaultValue]
 * for a key that's never been explicitly [set].
 */
interface PluginSettings {
    fun declare(descriptors: List<PluginSettingDescriptor>)

    fun descriptors(): List<PluginSettingDescriptor>

    suspend fun get(key: String): String?

    suspend fun set(
        key: String,
        value: String,
    )
}
