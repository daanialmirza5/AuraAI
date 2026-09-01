package com.aura.ai.plugin.runtime

import com.aura.ai.plugin.api.Plugin
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginResult

/**
 * "Plugin Updates" — hot-swaps an already-registered plugin for a newer version of itself.
 * Rejects [update] outright if [newPlugin]'s manifest isn't compatible with the running host, or
 * isn't actually newer than what's installed — an update is never allowed to silently downgrade
 * or break compatibility. If the existing version was `Enabled`, the new one is disabled/unloaded
 * and re-loaded/re-enabled in its place, so the net effect from a caller's perspective is "the
 * same plugin id, now running newer code, no interruption beyond the swap itself."
 */
interface PluginUpdateManager {
    suspend fun update(
        newPlugin: Plugin,
        grantedPermissions: Set<PluginPermission>,
    ): PluginResult<Unit>
}
