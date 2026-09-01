package com.aura.ai.plugin.loader

import com.aura.ai.plugin.api.Plugin

/**
 * Where [Plugin] instances come from — the seam a future dynamic loading mechanism (parsing a
 * manifest and instantiating a class from a `.dex`/`.jar` dropped on-device, say) would implement
 * without touching [PluginLoader] itself. This phase ships exactly one implementation,
 * [InMemoryPluginSource]: plugins compiled directly into the app binary, discovered via the same
 * Hilt `Set<Plugin>` multibinding pattern `core-tools`/`core-agents` already use for `Tool`/`Agent`.
 * See `docs/PLUGIN_RUNTIME.md` for why that's the honest, appropriate scope for this phase rather
 * than attempting real dynamic code loading, which is a materially different security problem.
 */
interface PluginSource {
    fun discover(): List<Plugin>
}
