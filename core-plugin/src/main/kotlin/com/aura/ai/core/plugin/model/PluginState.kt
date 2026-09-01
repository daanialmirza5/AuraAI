package com.aura.ai.core.plugin.model

/**
 * A plugin's lifecycle state, host-side — the states `com.aura.ai.plugin.api.Plugin`'s own
 * callbacks (`onLoad`/`onEnable`/`onDisable`/`onUnload`) transition between. See
 * `plugin-runtime`'s `PluginRuntime` for the transitions this enum permits.
 */
enum class PluginState {
    /** Manifest validated and compatible, but [com.aura.ai.plugin.api.Plugin.onLoad] hasn't run yet. */
    Registered,

    /** [com.aura.ai.plugin.api.Plugin.onLoad] succeeded; not yet enabled. */
    Loaded,

    /** [com.aura.ai.plugin.api.Plugin.onEnable] succeeded — actively registered tools/capabilities/intents. */
    Enabled,

    /** [com.aura.ai.plugin.api.Plugin.onDisable] succeeded — loaded, but not contributing anything right now. */
    Disabled,

    /** [com.aura.ai.plugin.api.Plugin.onUnload] succeeded — terminal for this instance. */
    Unloaded,

    /** A lifecycle callback failed, or the manifest was invalid/incompatible — never ran, or
     *  stopped running. */
    Failed,
}
