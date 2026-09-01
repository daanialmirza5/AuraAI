package com.aura.ai.core.plugin.compatibility

import com.aura.ai.core.plugin.HostSdk
import com.aura.ai.plugin.api.PluginManifest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPluginCompatibilityChecker
    @Inject
    constructor() : PluginCompatibilityChecker {
        override fun check(manifest: PluginManifest): PluginCompatibilityResult {
            if (manifest.id.isBlank()) {
                return PluginCompatibilityResult.Incompatible("Plugin id must not be blank.")
            }
            if (manifest.name.isBlank()) {
                return PluginCompatibilityResult.Incompatible("Plugin name must not be blank.")
            }

            val host = HostSdk.VERSION
            if (host < manifest.minHostVersion) {
                return PluginCompatibilityResult.Incompatible(
                    "Plugin '${manifest.id}' requires host SDK ${manifest.minHostVersion} or later; this host is $host.",
                )
            }

            val maxVersion = manifest.maxHostVersion
            if (maxVersion != null && host > maxVersion) {
                return PluginCompatibilityResult.Incompatible(
                    "Plugin '${manifest.id}' supports host SDK up to $maxVersion; this host is $host.",
                )
            }

            return PluginCompatibilityResult.Compatible
        }
    }
