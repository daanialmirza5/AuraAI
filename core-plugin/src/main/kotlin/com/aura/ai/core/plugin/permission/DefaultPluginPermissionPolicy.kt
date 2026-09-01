package com.aura.ai.core.plugin.permission

import com.aura.ai.plugin.api.PluginManifest
import com.aura.ai.plugin.api.PluginPermission
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Grants exactly what a plugin declared it needs, nothing more, nothing less. This is the right
 * default for this phase because `plugin-loader`'s only plugin source this phase is plugins
 * compiled directly into the app binary (see `docs/PLUGIN_RUNTIME.md`) — the same trust level as
 * a built-in `com.aura.ai.core.tools.Tool` or `com.aura.ai.core.agents.Agent`, which also never
 * go through a user-facing grant prompt. A future phase accepting plugins from an untrusted
 * source (`plugin-marketplace` once it's actually connected to something) is exactly where a
 * stricter [PluginPermissionPolicy] — one that consults the user before granting anything —
 * plugs in behind this same interface, with zero change to anything that calls it.
 */
@Singleton
class DefaultPluginPermissionPolicy
    @Inject
    constructor() : PluginPermissionPolicy {
        override fun decideGrants(manifest: PluginManifest): Set<PluginPermission> = manifest.requiredPermissions
    }
