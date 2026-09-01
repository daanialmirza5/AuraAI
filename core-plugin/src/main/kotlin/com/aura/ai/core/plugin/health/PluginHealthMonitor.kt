package com.aura.ai.core.plugin.health

import com.aura.ai.plugin.api.PluginHealthReport

/** "Plugin Health," host-side: queries every currently-loaded plugin's own
 *  [com.aura.ai.plugin.api.Plugin.health] on demand and hands back the combined picture — the
 *  plugin-system analogue of how `com.aura.ai.core.orchestrator.dispatch.TaskDispatcher` calls
 *  `Agent.health()` before dispatching, except this one is a pull, not a pre-flight gate. */
interface PluginHealthMonitor {
    suspend fun checkAll(): Map<String, PluginHealthReport>

    suspend fun check(pluginId: String): PluginHealthReport?
}
