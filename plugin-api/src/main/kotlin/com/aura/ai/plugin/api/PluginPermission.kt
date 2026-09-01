package com.aura.ai.plugin.api

/**
 * What a plugin can do — not to be confused with an Android runtime permission (`POST_NOTIFICATIONS`
 * and friends). This is a closed, host-defined vocabulary of *internal capabilities* a plugin
 * might request: registering a tool, reading memory, and so on. A plugin never sees or requests
 * an Android permission string directly — if a plugin's registered tool ultimately needs one
 * (e.g. it wraps a notification), that's declared and checked the same way any other tool's
 * permission requirement is, entirely on the host side (`core-reasoning.PermissionChecker`), long
 * after this permission model has already decided the plugin is even allowed to register tools
 * at all.
 */
enum class PluginPermission {
    /** Register/unregister tools via `PluginContext.tools`. */
    RegisterTools,

    /** Register/unregister capabilities via `PluginContext.capabilities`. */
    RegisterCapabilities,

    /** Register/unregister intent triggers via `PluginContext.intents`. */
    RegisterIntents,

    /** Read/write this plugin's own scoped storage via `PluginContext.storage`. */
    AccessStorage,

    /** Read this plugin's own scoped settings via `PluginContext.settings`. */
    AccessSettings,

    /** Publish/observe events via `PluginContext.events`. */
    PublishEvents,

    /** Show the user a notification through the host's real notification path. */
    NotifyUser,
}
