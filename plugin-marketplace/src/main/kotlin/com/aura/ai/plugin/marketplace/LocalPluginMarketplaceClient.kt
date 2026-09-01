package com.aura.ai.plugin.marketplace

import com.aura.ai.core.plugin.registry.PluginRegistry
import com.aura.ai.plugin.api.PluginError
import com.aura.ai.plugin.api.PluginResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only [PluginMarketplaceClient] this phase ships. [browse]/[search] are real — they list
 * whatever `PluginRegistry` actually has registered, every listing correctly marked
 * [PluginListing.installed] `true` (since every plugin known to this host, this phase, got there
 * via `plugin-loader` reading plugins compiled into the app — see `docs/PLUGIN_RUNTIME.md`).
 * [install]/[checkForUpdates] need a remote catalog to mean anything and honestly fail with
 * [PluginError.NotSupported] rather than pretending to fetch something — the same convention
 * `core-providers.ScaffoldAIProvider` and `core-memory.NoOpEmbeddingProvider` established for
 * every other "the seam is real, the connection isn't" gap in this codebase.
 */
@Singleton
class LocalPluginMarketplaceClient
    @Inject
    constructor(
        private val pluginRegistry: PluginRegistry,
    ) : PluginMarketplaceClient {
        override suspend fun browse(): PluginResult<List<PluginListing>> =
            PluginResult.Success(
                pluginRegistry.all().map { record ->
                    PluginListing(
                        id = record.manifest.id,
                        name = record.manifest.name,
                        description = record.manifest.description,
                        version = record.manifest.version,
                        author = record.manifest.author,
                        installed = true,
                    )
                },
            )

        override suspend fun search(query: String): PluginResult<List<PluginListing>> {
            val browseResult = browse()
            val listings = (browseResult as? PluginResult.Success)?.value ?: return browseResult
            val needle = query.trim().lowercase()
            val matches =
                if (needle.isEmpty()) {
                    listings
                } else {
                    listings.filter { it.name.lowercase().contains(needle) || it.description.lowercase().contains(needle) }
                }
            return PluginResult.Success(matches)
        }

        override suspend fun install(pluginId: String): PluginResult<Unit> =
            PluginResult.Failure(
                PluginError.NotSupported(
                    "No remote plugin marketplace is connected in this phase — '$pluginId' can only be added by " +
                        "compiling it into the app and registering it as a Plugin, the same way every currently " +
                        "installed plugin got here.",
                ),
            )

        override suspend fun checkForUpdates(): PluginResult<List<PluginListing>> =
            PluginResult.Failure(
                PluginError.NotSupported("No remote plugin marketplace is connected in this phase — nothing to check against."),
            )
    }
