package com.aura.ai.plugin.api

/**
 * The *only* handle a [Plugin] ever receives into the host — every capability a plugin has flows
 * through one of these properties, and every one of them is a `plugin-api` interface, never an
 * internal type. This is the sandbox boundary made concrete: `core-plugin`'s real implementation
 * (`DefaultPluginContext`) holds real references to `core-tools.ToolRegistry`,
 * `core-capabilities.CapabilityRegistry`, and so on, but a `Plugin` implementation compiled only
 * against `plugin-api` has no way to reach any of that — it can only call through the narrow
 * interfaces exposed here. See `docs/PLUGIN_SECURITY.md`.
 *
 * [grantedPermissions] mirrors, rather than exceeds, [PluginManifest.requiredPermissions] — a
 * plugin is never granted more than it declared it needed. Each accessor below is expected to
 * fail with [PluginError.PermissionDenied] (not throw, not silently no-op) if the corresponding
 * [PluginPermission] wasn't granted; see `core-plugin`'s registrar implementations for exactly
 * how.
 */
interface PluginContext {
    val pluginId: String
    val grantedPermissions: Set<PluginPermission>

    fun hasPermission(permission: PluginPermission): Boolean = permission in grantedPermissions

    val tools: PluginToolRegistrar
    val capabilities: PluginCapabilityRegistrar
    val intents: PluginIntentRegistrar
    val storage: PluginStorage
    val settings: PluginSettings
    val events: PluginEventBus

    /** A minimal logging facade — a plugin never gets raw `android.util.Log` access, but still
     *  needs *some* way to report what it's doing during development. Every call is tagged with
     *  [pluginId] by the real implementation, so a misbehaving plugin's log spam is always
     *  attributable. */
    fun log(message: String)
}
