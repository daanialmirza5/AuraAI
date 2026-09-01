package com.aura.ai.plugin.api

/**
 * The one interface every AURA plugin implements — "Plugin interface" and "Plugin lifecycle"
 * together. [manifest] is read once, before [onLoad] ever runs, to decide compatibility and
 * permissions (see `docs/PLUGIN_SDK.md`). The four lifecycle methods are called in one strict
 * order by `plugin-runtime`: [onLoad] → [onEnable] → (running) → [onDisable] → [onUnload]. A
 * plugin can be disabled and re-enabled any number of times (`onDisable` → `onEnable`) without
 * being reloaded; [onUnload] is terminal for that instance.
 *
 * [context] is the *only* handle a plugin ever receives into the host — see [PluginContext]. A
 * `Plugin` implementation that never imports anything beyond `plugin-api` cannot reach any
 * internal AURA module, by construction, not by convention.
 */
interface Plugin {
    val manifest: PluginManifest

    /** Called once, after permission checks pass and before the plugin does anything observable
     *  (register tools, touch storage). Use this to validate configuration, not to start doing
     *  real work — that's [onEnable]. */
    suspend fun onLoad(context: PluginContext): PluginResult<Unit>

    /** Called after [onLoad] (or after a prior [onDisable]) — the plugin should register
     *  everything it offers (tools, capabilities, intents) here. */
    suspend fun onEnable(context: PluginContext): PluginResult<Unit>

    /** The inverse of [onEnable] — unregister everything registered there. The plugin instance
     *  is still loaded and may be [onEnable]d again later. */
    suspend fun onDisable(context: PluginContext): PluginResult<Unit>

    /** Terminal — release any resources held since [onLoad]. This instance will not be reused;
     *  a future load of the same plugin id creates a new [Plugin] instance. */
    suspend fun onUnload(context: PluginContext): PluginResult<Unit>

    /** A lightweight self-report, independent of lifecycle state — "Plugin Health." Should never
     *  throw and should return quickly; `core-plugin`'s health monitor may call this on any
     *  loaded plugin at any time. */
    suspend fun health(): PluginHealthReport
}
