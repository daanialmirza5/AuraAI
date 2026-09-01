package com.aura.ai.core.plugin.context

import com.aura.ai.core.plugin.event.DefaultPluginEventBus
import com.aura.ai.core.plugin.registrar.DefaultPluginCapabilityRegistrar
import com.aura.ai.core.plugin.registrar.DefaultPluginIntentRegistrar
import com.aura.ai.core.plugin.registrar.DefaultPluginToolRegistrar
import com.aura.ai.core.plugin.registrar.PluginCapabilityRegistry
import com.aura.ai.core.plugin.registrar.PluginIntentRegistry
import com.aura.ai.core.plugin.settings.PluginSettingsHost
import com.aura.ai.core.plugin.storage.PluginStorageHost
import com.aura.ai.core.tools.ToolRegistry
import com.aura.ai.plugin.api.PluginPermission
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/** Distinguishes the injected plugin log sink from any other `(String, String) -> Unit` binding.
 *  The real sink is provided by `:app` (e.g. gated behind `BuildConfig.DEBUG`) so no
 *  plugin-authored log content reaches a release build's logcat — see `docs/SECURITY.md` §3. This
 *  module stays free of any Android dependency; it only depends on the function shape. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PluginLogSink

/**
 * Builds one [DefaultPluginContext] per plugin — every registrar it hands out is a fresh,
 * per-plugin-scoped object closed over that plugin's own id and granted permissions, backed by
 * the handful of process-wide singletons ([ToolRegistry], [PluginCapabilityRegistry], etc.)
 * every plugin's context ultimately shares.
 */
@Singleton
class PluginContextFactory
    @Inject
    constructor(
        private val toolRegistry: ToolRegistry,
        private val pluginCapabilityRegistry: PluginCapabilityRegistry,
        private val pluginIntentRegistry: PluginIntentRegistry,
        private val storageHost: PluginStorageHost,
        private val settingsHost: PluginSettingsHost,
        private val eventBus: DefaultPluginEventBus,
        @PluginLogSink private val logSink: @JvmSuppressWildcards (pluginId: String, message: String) -> Unit,
    ) {
        fun create(
            pluginId: String,
            grantedPermissions: Set<PluginPermission>,
        ): DefaultPluginContext =
            DefaultPluginContext(
                pluginId = pluginId,
                grantedPermissions = grantedPermissions,
                tools = DefaultPluginToolRegistrar(pluginId, grantedPermissions, toolRegistry),
                capabilities = DefaultPluginCapabilityRegistrar(pluginId, grantedPermissions, pluginCapabilityRegistry),
                intents = DefaultPluginIntentRegistrar(pluginId, grantedPermissions, pluginIntentRegistry),
                storage = storageHost.scopedTo(pluginId),
                settings = settingsHost.scopedTo(pluginId),
                events = eventBus,
                logSink = logSink,
            )
    }
