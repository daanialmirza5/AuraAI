package com.aura.ai.plugin.api

import kotlinx.coroutines.flow.Flow

/**
 * "Plugin Events" — the plugin-facing half of the event system. Deliberately its own sealed
 * hierarchy, independent of `core-events.AuraEvent`: a plugin never depends on `core-events`
 * (same isolation rule as everything else in this module), so it can't observe or publish
 * `AuraEvent`s directly. `plugin-runtime` is the bridge — every lifecycle transition here has a
 * corresponding `core-events` event the rest of the host observes; see `docs/PLUGIN_RUNTIME.md`.
 */
sealed class PluginEvent(
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    data class Loaded(
        val pluginId: String,
    ) : PluginEvent()

    data class Enabled(
        val pluginId: String,
    ) : PluginEvent()

    data class Disabled(
        val pluginId: String,
    ) : PluginEvent()

    data class Unloaded(
        val pluginId: String,
    ) : PluginEvent()

    data class Failed(
        val pluginId: String,
        val reason: String,
    ) : PluginEvent()

    data class HealthChanged(
        val pluginId: String,
        val status: PluginHealthStatus,
    ) : PluginEvent()

    /** A plugin's own custom event, for other plugins to observe — the plugin-to-plugin analogue
     *  of `core-agents`' rule that agents only ever talk through the event bus, never directly. */
    data class Custom(
        val pluginId: String,
        val name: String,
        val payload: Map<String, String> = emptyMap(),
    ) : PluginEvent()
}

/** Exposed via `PluginContext.events`. [publish] only accepts [PluginEvent.Custom] — a plugin
 *  reports its own facts, it doesn't get to fabricate another plugin's lifecycle transitions. */
interface PluginEventBus {
    suspend fun publish(event: PluginEvent.Custom)

    fun events(): Flow<PluginEvent>
}
