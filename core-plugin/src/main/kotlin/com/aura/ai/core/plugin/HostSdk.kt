package com.aura.ai.core.plugin

import com.aura.ai.plugin.api.PluginVersion

/** The running host's own SDK version — what every [com.aura.ai.plugin.api.PluginManifest]'s
 *  `minHostVersion`/`maxHostVersion` is checked against. Bump this when a change to `plugin-api`
 *  could break an existing plugin's assumptions. */
object HostSdk {
    val VERSION = PluginVersion(1, 0, 0)
}
