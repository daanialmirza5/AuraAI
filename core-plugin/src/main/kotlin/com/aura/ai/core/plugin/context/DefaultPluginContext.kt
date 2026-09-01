package com.aura.ai.core.plugin.context

import com.aura.ai.core.plugin.registrar.DefaultPluginToolRegistrar
import com.aura.ai.plugin.api.PluginCapabilityRegistrar
import com.aura.ai.plugin.api.PluginContext
import com.aura.ai.plugin.api.PluginEventBus
import com.aura.ai.plugin.api.PluginIntentRegistrar
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginSettings
import com.aura.ai.plugin.api.PluginStorage

/**
 * The real [PluginContext] — holds genuine references to host infrastructure
 * ([tools] wraps `core-tools.ToolRegistry`, etc.), but a [com.aura.ai.plugin.api.Plugin]
 * implementation compiled only against `plugin-api` has no import path to any of that; it only
 * ever sees this through the [PluginContext] interface's narrow surface. That gap — real access
 * on this side, zero visibility on the plugin's side — is the sandbox boundary. See
 * `docs/PLUGIN_SECURITY.md`.
 */
class DefaultPluginContext(
    override val pluginId: String,
    override val grantedPermissions: Set<PluginPermission>,
    override val tools: DefaultPluginToolRegistrar,
    override val capabilities: PluginCapabilityRegistrar,
    override val intents: PluginIntentRegistrar,
    override val storage: PluginStorage,
    override val settings: PluginSettings,
    override val events: PluginEventBus,
    private val logSink: (pluginId: String, message: String) -> Unit,
) : PluginContext {
    override fun log(message: String) {
        logSink(pluginId, message)
    }
}
