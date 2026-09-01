package com.aura.ai.plugin.loader

/** One [PluginSource]'s discovery of one plugin, checked, granted, loaded, and enabled (or not),
 *  with the reason either way. */
data class PluginLoadResult(
    val pluginId: String,
    val success: Boolean,
    val detail: String,
)

/**
 * The entry point that turns "plugins compiled into this app" into "plugins actually running" —
 * discovers every [PluginSource], checks each candidate's compatibility, decides its permission
 * grants, and hands it to `plugin-runtime.PluginRuntime` to load and enable. Called once, from
 * `com.aura.ai.AuraApplication.onCreate`, the same moment `core-tools.ToolRegistry` and
 * `core-agents.AgentRegistry` are populated.
 */
interface PluginLoader {
    suspend fun loadAll(): List<PluginLoadResult>
}
