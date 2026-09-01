package com.aura.ai.core.plugin.permission

import com.aura.ai.plugin.api.PluginManifest
import com.aura.ai.plugin.api.PluginPermission

/**
 * "Plugin permissions" — decides how much of [PluginManifest.requiredPermissions] a plugin
 * actually receives; a granted set is never a superset of what was declared, but may be a subset.
 * `DefaultPluginPermissionPolicy` is intentionally permissive (grants everything declared) — see
 * its own doc for why that's the honest, correct default for *this* phase's plugin source
 * (compiled into the app, not sideloaded from anywhere untrusted), and where a stricter,
 * user-prompted policy would plug in behind this same interface later.
 */
interface PluginPermissionPolicy {
    fun decideGrants(manifest: PluginManifest): Set<PluginPermission>
}
