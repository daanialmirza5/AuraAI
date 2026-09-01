# plugin-marketplace

Browsing, searching, and installing plugins from a catalog. Pure Kotlin/JVM; depends on
`plugin-api`, `core-plugin`, `core-ai`. See `docs/PLUGIN_SDK.md` at the project root for how this
fits the rest of the plugin system.

## What lives here

- **`PluginListing`** — a marketplace-facing summary of a plugin, smaller than a full
  `PluginManifest`.
- **`PluginMarketplaceClient`** / **`LocalPluginMarketplaceClient`** — `browse`/`search` are real:
  they list whatever `core-plugin.PluginRegistry` actually has registered, every listing correctly
  marked `installed = true`. `install`/`checkForUpdates` need a remote catalog to mean anything and
  honestly fail with `PluginError.NotSupported` — the same "seam is real, connection isn't"
  convention `core-providers.ScaffoldAIProvider` and `core-memory.NoOpEmbeddingProvider`
  established.

## Status

`browse`/`search` are fully real, not a stub — they reflect the actual, live plugin registry.
`install`/`checkForUpdates` are honestly scaffolded: this phase never connects to a cloud provider
of any kind, so there is no remote catalog to install from. The trust-model question this raises
(what permission policy applies to a plugin that *did* come from an untrusted remote source) is
deliberately left to whichever future phase actually connects one — see `docs/PLUGIN_SECURITY.md §5`.
