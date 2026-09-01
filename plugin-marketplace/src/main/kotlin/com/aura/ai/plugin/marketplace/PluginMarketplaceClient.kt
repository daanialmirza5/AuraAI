package com.aura.ai.plugin.marketplace

import com.aura.ai.plugin.api.PluginResult

/**
 * Browsing, searching, and installing plugins from a catalog. Mirrors `core-providers.AIProvider`'s
 * own honesty split exactly: [browse]/[search] are genuinely real this phase (see
 * `LocalPluginMarketplaceClient`, which lists what's actually installed via `core-plugin.PluginRegistry`),
 * while [install] and [checkForUpdates] require a remote catalog this phase deliberately doesn't
 * connect to — see `docs/PLUGIN_SDK.md`.
 */
interface PluginMarketplaceClient {
    suspend fun browse(): PluginResult<List<PluginListing>>

    suspend fun search(query: String): PluginResult<List<PluginListing>>

    suspend fun install(pluginId: String): PluginResult<Unit>

    suspend fun checkForUpdates(): PluginResult<List<PluginListing>>
}
