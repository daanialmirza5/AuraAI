package com.aura.ai.core.plugin.registrar

import com.aura.ai.plugin.api.PluginError
import com.aura.ai.plugin.api.PluginIntentDescriptor
import com.aura.ai.plugin.api.PluginIntentRegistrar
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginResult

class DefaultPluginIntentRegistrar(
    private val pluginId: String,
    private val grantedPermissions: Set<PluginPermission>,
    private val registry: PluginIntentRegistry,
) : PluginIntentRegistrar {
    override fun register(descriptor: PluginIntentDescriptor): PluginResult<Unit> {
        if (PluginPermission.RegisterIntents !in grantedPermissions) {
            return PluginResult.Failure(
                PluginError.PermissionDenied(
                    PluginPermission.RegisterIntents,
                    "Plugin '$pluginId' was not granted RegisterIntents.",
                ),
            )
        }
        registry.register(pluginId, descriptor)
        return PluginResult.Success(Unit)
    }

    override fun unregister(id: String): PluginResult<Unit> {
        registry.unregister(pluginId, id)
        return PluginResult.Success(Unit)
    }
}
