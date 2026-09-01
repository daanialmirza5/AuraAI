package com.aura.ai.plugin.runtime

import com.aura.ai.plugin.api.Plugin
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginResult

/**
 * "Plugin lifecycle" and "Plugin registration," run for real: the strict `onLoad → onEnable →
 * (onDisable → onEnable)* → onUnload` sequence `com.aura.ai.plugin.api.Plugin` documents, plus
 * every state transition and event this produces. `plugin-loader` is the only intended caller in
 * the normal path — it decides compatibility and permissions *before* calling [load]; this
 * interface only knows how to run the lifecycle once those decisions are made.
 */
interface PluginRuntime {
    /** Registers [plugin], constructs its sandboxed context with exactly [grantedPermissions],
     *  and calls `onLoad`. */
    suspend fun load(
        plugin: Plugin,
        grantedPermissions: Set<PluginPermission>,
    ): PluginResult<Unit>

    /** Calls `onEnable` on an already-[load]ed (or previously disabled) plugin. */
    suspend fun enable(pluginId: String): PluginResult<Unit>

    /** Calls `onDisable`, then unconditionally unregisters every tool/capability/intent this
     *  plugin registered — regardless of what `onDisable` itself did, so a plugin that forgets
     *  to clean up after itself can never leave an orphaned registration behind. */
    suspend fun disable(pluginId: String): PluginResult<Unit>

    /** Calls `onUnload` (after the same unconditional cleanup [disable] performs, in case the
     *  plugin was still enabled) and discards its context. Terminal for this plugin instance. */
    suspend fun unload(pluginId: String): PluginResult<Unit>
}
