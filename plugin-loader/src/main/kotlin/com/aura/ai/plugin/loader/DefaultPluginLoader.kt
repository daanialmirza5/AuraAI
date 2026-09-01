package com.aura.ai.plugin.loader

import com.aura.ai.core.plugin.compatibility.PluginCompatibilityChecker
import com.aura.ai.core.plugin.compatibility.PluginCompatibilityResult
import com.aura.ai.core.plugin.permission.PluginPermissionPolicy
import com.aura.ai.plugin.api.Plugin
import com.aura.ai.plugin.api.PluginResult
import com.aura.ai.plugin.runtime.PluginRuntime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPluginLoader
    @Inject
    constructor(
        private val sources: Set<@JvmSuppressWildcards PluginSource>,
        private val compatibilityChecker: PluginCompatibilityChecker,
        private val permissionPolicy: PluginPermissionPolicy,
        private val pluginRuntime: PluginRuntime,
    ) : PluginLoader {
        override suspend fun loadAll(): List<PluginLoadResult> = sources.flatMap { it.discover() }.map { plugin -> loadOne(plugin) }

        private suspend fun loadOne(plugin: Plugin): PluginLoadResult {
            val manifest = plugin.manifest

            val compatibility = compatibilityChecker.check(manifest)
            if (compatibility is PluginCompatibilityResult.Incompatible) {
                return PluginLoadResult(manifest.id, success = false, detail = compatibility.reason)
            }

            val grants = permissionPolicy.decideGrants(manifest)

            val loadResult = pluginRuntime.load(plugin, grants)
            if (loadResult is PluginResult.Failure) {
                return PluginLoadResult(manifest.id, success = false, detail = loadResult.error.message)
            }

            return when (val enableResult = pluginRuntime.enable(manifest.id)) {
                is PluginResult.Success -> PluginLoadResult(manifest.id, success = true, detail = "Loaded and enabled.")
                is PluginResult.Failure -> PluginLoadResult(manifest.id, success = false, detail = enableResult.error.message)
            }
        }
    }
