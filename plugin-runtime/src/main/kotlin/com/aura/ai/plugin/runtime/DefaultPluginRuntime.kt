package com.aura.ai.plugin.runtime

import com.aura.ai.core.events.EventBus
import com.aura.ai.core.events.PluginDisabledEvent
import com.aura.ai.core.events.PluginEnabledEvent
import com.aura.ai.core.events.PluginFailedEvent
import com.aura.ai.core.events.PluginLoadedEvent
import com.aura.ai.core.events.PluginUnloadedEvent
import com.aura.ai.core.plugin.context.DefaultPluginContext
import com.aura.ai.core.plugin.context.PluginContextFactory
import com.aura.ai.core.plugin.event.DefaultPluginEventBus
import com.aura.ai.core.plugin.model.PluginRecord
import com.aura.ai.core.plugin.model.PluginState
import com.aura.ai.core.plugin.registrar.PluginCapabilityRegistry
import com.aura.ai.core.plugin.registrar.PluginIntentRegistry
import com.aura.ai.core.plugin.registry.PluginRegistry
import com.aura.ai.plugin.api.Plugin
import com.aura.ai.plugin.api.PluginError
import com.aura.ai.plugin.api.PluginEvent
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginResult
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPluginRuntime
    @Inject
    constructor(
        private val pluginRegistry: PluginRegistry,
        private val pluginContextFactory: PluginContextFactory,
        private val pluginCapabilityRegistry: PluginCapabilityRegistry,
        private val pluginIntentRegistry: PluginIntentRegistry,
        private val pluginEventBus: DefaultPluginEventBus,
        private val hostEventBus: EventBus,
    ) : PluginRuntime {
        /** One [DefaultPluginContext] per loaded plugin, retained across `enable`/`disable` calls so
         *  a plugin's own registrar bookkeeping (which tools it registered, in particular — see
         *  `DefaultPluginToolRegistrar`) survives being disabled and re-enabled. */
        private val contexts = ConcurrentHashMap<String, DefaultPluginContext>()

        override suspend fun load(
            plugin: Plugin,
            grantedPermissions: Set<PluginPermission>,
        ): PluginResult<Unit> {
            val manifest = plugin.manifest
            pluginRegistry.register(PluginRecord(plugin, manifest, PluginState.Registered, grantedPermissions))

            val context = pluginContextFactory.create(manifest.id, grantedPermissions)
            contexts[manifest.id] = context

            return when (val result = plugin.onLoad(context)) {
                is PluginResult.Success -> {
                    pluginRegistry.updateState(manifest.id, PluginState.Loaded)
                    pluginEventBus.publishLifecycleEvent(PluginEvent.Loaded(manifest.id))
                    hostEventBus.publish(PluginLoadedEvent(manifest.id))
                    PluginResult.Success(Unit)
                }
                is PluginResult.Failure -> {
                    markFailed(manifest.id, result.error.message)
                    result
                }
            }
        }

        override suspend fun enable(pluginId: String): PluginResult<Unit> {
            val record = requireRecord(pluginId) ?: return notRegistered(pluginId)
            if (record.state != PluginState.Loaded && record.state != PluginState.Disabled) {
                return PluginResult.Failure(
                    PluginError.InvalidRequest("Plugin '$pluginId' cannot be enabled from state ${record.state}."),
                )
            }
            val context = contexts[pluginId] ?: return noContext(pluginId)

            return when (val result = record.plugin.onEnable(context)) {
                is PluginResult.Success -> {
                    pluginRegistry.updateState(pluginId, PluginState.Enabled)
                    pluginEventBus.publishLifecycleEvent(PluginEvent.Enabled(pluginId))
                    hostEventBus.publish(PluginEnabledEvent(pluginId))
                    PluginResult.Success(Unit)
                }
                is PluginResult.Failure -> {
                    markFailed(pluginId, result.error.message)
                    result
                }
            }
        }

        override suspend fun disable(pluginId: String): PluginResult<Unit> {
            val record = requireRecord(pluginId) ?: return notRegistered(pluginId)
            val context = contexts[pluginId] ?: return noContext(pluginId)

            val result = record.plugin.onDisable(context)
            cleanupRegistrations(pluginId, context)

            pluginRegistry.updateState(pluginId, PluginState.Disabled)
            pluginEventBus.publishLifecycleEvent(PluginEvent.Disabled(pluginId))
            hostEventBus.publish(PluginDisabledEvent(pluginId))

            return if (result is PluginResult.Failure) result else PluginResult.Success(Unit)
        }

        override suspend fun unload(pluginId: String): PluginResult<Unit> {
            val record = requireRecord(pluginId) ?: return notRegistered(pluginId)
            val context = contexts[pluginId] ?: return noContext(pluginId)

            val result = record.plugin.onUnload(context)
            cleanupRegistrations(pluginId, context)
            contexts.remove(pluginId)

            pluginRegistry.updateState(pluginId, PluginState.Unloaded)
            pluginEventBus.publishLifecycleEvent(PluginEvent.Unloaded(pluginId))
            hostEventBus.publish(PluginUnloadedEvent(pluginId))

            return if (result is PluginResult.Failure) result else PluginResult.Success(Unit)
        }

        /** Unregisters everything this plugin registered — tools (which track their own registered
         *  names), and capabilities/intents (which the host-side registries already track by plugin
         *  id) — unconditionally, regardless of whether the plugin's own callback succeeded. */
        private fun cleanupRegistrations(
            pluginId: String,
            context: DefaultPluginContext,
        ) {
            context.tools.unregisterAll()
            pluginCapabilityRegistry.unregisterAll(pluginId)
            pluginIntentRegistry.unregisterAll(pluginId)
        }

        private suspend fun markFailed(
            pluginId: String,
            reason: String,
        ) {
            pluginRegistry.updateState(pluginId, PluginState.Failed, reason)
            pluginEventBus.publishLifecycleEvent(PluginEvent.Failed(pluginId, reason))
            hostEventBus.publish(PluginFailedEvent(pluginId, reason))
        }

        private fun requireRecord(pluginId: String) = pluginRegistry.get(pluginId)

        private fun notRegistered(pluginId: String) =
            PluginResult.Failure(PluginError.NotSupported("No plugin is registered with id '$pluginId'."))

        private fun noContext(pluginId: String) =
            PluginResult.Failure(PluginError.Unknown("Plugin '$pluginId' has no context — it was never successfully loaded."))
    }
