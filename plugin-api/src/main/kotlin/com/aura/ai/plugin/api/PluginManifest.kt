package com.aura.ai.plugin.api

/**
 * Everything the host needs to know about a plugin before running a single line of its code —
 * "Plugin manifest." [minHostVersion]/[maxHostVersion] are what `core-plugin`'s compatibility
 * checker validates against the running host's SDK version before the plugin is ever loaded;
 * [requiredPermissions] are what it's granted (or not) before [Plugin.onLoad] runs. Every field
 * here is plain data — a manifest describes a plugin, it doesn't run any of it.
 */
data class PluginManifest(
    val id: String,
    val name: String,
    val description: String,
    val version: PluginVersion,
    val author: String = "",
    val minHostVersion: PluginVersion,
    /** `null` means "no known upper bound" — compatible with this host version and every later one. */
    val maxHostVersion: PluginVersion? = null,
    val requiredPermissions: Set<PluginPermission> = emptySet(),
)
