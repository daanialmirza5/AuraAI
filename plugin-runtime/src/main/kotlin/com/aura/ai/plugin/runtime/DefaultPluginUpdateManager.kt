package com.aura.ai.plugin.runtime

import com.aura.ai.core.plugin.compatibility.PluginCompatibilityChecker
import com.aura.ai.core.plugin.compatibility.PluginCompatibilityResult
import com.aura.ai.core.plugin.model.PluginState
import com.aura.ai.core.plugin.registry.PluginRegistry
import com.aura.ai.plugin.api.Plugin
import com.aura.ai.plugin.api.PluginError
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPluginUpdateManager
    @Inject
    constructor(
        private val pluginRuntime: PluginRuntime,
        private val pluginRegistry: PluginRegistry,
        private val compatibilityChecker: PluginCompatibilityChecker,
    ) : PluginUpdateManager {
        override suspend fun update(
            newPlugin: Plugin,
            grantedPermissions: Set<PluginPermission>,
        ): PluginResult<Unit> {
            val newManifest = newPlugin.manifest

            when (val compatibility = compatibilityChecker.check(newManifest)) {
                is PluginCompatibilityResult.Incompatible ->
                    return PluginResult.Failure(PluginError.InvalidRequest(compatibility.reason))
                PluginCompatibilityResult.Compatible -> Unit
            }

            val existing = pluginRegistry.get(newManifest.id)
            if (existing != null && newManifest.version <= existing.manifest.version) {
                return PluginResult.Failure(
                    PluginError.InvalidRequest(
                        "New version ${newManifest.version} is not newer than the installed ${existing.manifest.version}.",
                    ),
                )
            }

            val wasEnabled = existing?.state == PluginState.Enabled
            if (existing != null) {
                if (wasEnabled) pluginRuntime.disable(newManifest.id)
                pluginRuntime.unload(newManifest.id)
            }

            val loadResult = pluginRuntime.load(newPlugin, grantedPermissions)
            if (loadResult is PluginResult.Failure) return loadResult

            return if (wasEnabled) pluginRuntime.enable(newManifest.id) else PluginResult.Success(Unit)
        }
    }
