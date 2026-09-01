package com.aura.ai.core.plugin.registrar

import com.aura.ai.core.tools.ToolRegistry
import com.aura.ai.plugin.api.PluginError
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginResult
import com.aura.ai.plugin.api.PluginToolDescriptor
import com.aura.ai.plugin.api.PluginToolRegistrar
import java.util.concurrent.ConcurrentHashMap

class DefaultPluginToolRegistrar(
    private val pluginId: String,
    private val grantedPermissions: Set<PluginPermission>,
    private val toolRegistry: ToolRegistry,
) : PluginToolRegistrar {
    /** Tool names this specific plugin has registered — what `unregisterAll` (called by
     *  `plugin-runtime` on disable/unload) needs to clean up, without touching any other
     *  plugin's or the host's own tools. */
    private val registeredNames = ConcurrentHashMap.newKeySet<String>()

    override fun register(descriptor: PluginToolDescriptor): PluginResult<Unit> {
        if (PluginPermission.RegisterTools !in grantedPermissions) {
            return PluginResult.Failure(
                PluginError.PermissionDenied(
                    PluginPermission.RegisterTools,
                    "Plugin '$pluginId' was not granted RegisterTools.",
                ),
            )
        }
        toolRegistry.register(PluginBackedTool(descriptor))
        registeredNames += descriptor.name
        return PluginResult.Success(Unit)
    }

    override fun unregister(name: String): PluginResult<Unit> {
        toolRegistry.unregister(name)
        registeredNames -= name
        return PluginResult.Success(Unit)
    }

    fun unregisterAll() {
        registeredNames.toList().forEach { unregister(it) }
    }
}
