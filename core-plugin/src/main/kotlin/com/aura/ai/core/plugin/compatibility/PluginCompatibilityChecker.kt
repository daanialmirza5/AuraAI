package com.aura.ai.core.plugin.compatibility

import com.aura.ai.plugin.api.PluginManifest

/** "Plugin Compatibility" — the outcome of checking one [PluginManifest] before anything from it
 *  ever runs. */
sealed interface PluginCompatibilityResult {
    data object Compatible : PluginCompatibilityResult

    data class Incompatible(
        val reason: String,
    ) : PluginCompatibilityResult
}

/**
 * Validates a manifest is well-formed and declares a host version range this running host falls
 * within — "Plugin Versioning" and "Plugin Compatibility" together. Checked once, before
 * `plugin-runtime` ever constructs a [com.aura.ai.plugin.api.PluginContext] for the plugin, let
 * alone calls [com.aura.ai.plugin.api.Plugin.onLoad].
 */
interface PluginCompatibilityChecker {
    fun check(manifest: PluginManifest): PluginCompatibilityResult
}
