package com.aura.ai.core.plugin.registrar

import com.aura.ai.plugin.api.PluginCapabilityRegistrar
import com.aura.ai.plugin.api.PluginError
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginResult

/** Constructed fresh per plugin (by `DefaultPluginContext`'s factory) rather than injected as a
 *  singleton — [pluginId] and [grantedPermissions] are per-plugin facts, [registry] is the one
 *  shared store underneath every plugin's view. */
class DefaultPluginCapabilityRegistrar(
    private val pluginId: String,
    private val grantedPermissions: Set<PluginPermission>,
    private val registry: PluginCapabilityRegistry,
) : PluginCapabilityRegistrar {
    override fun register(capabilityName: String): PluginResult<Unit> {
        if (PluginPermission.RegisterCapabilities !in grantedPermissions) {
            return PluginResult.Failure(
                PluginError.PermissionDenied(
                    PluginPermission.RegisterCapabilities,
                    "Plugin '$pluginId' was not granted RegisterCapabilities.",
                ),
            )
        }
        registry.register(pluginId, capabilityName)
        return PluginResult.Success(Unit)
    }

    override fun unregister(capabilityName: String): PluginResult<Unit> {
        registry.unregister(pluginId, capabilityName)
        return PluginResult.Success(Unit)
    }
}
