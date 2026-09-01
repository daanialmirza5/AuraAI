package com.aura.ai.core.plugin.model

import com.aura.ai.plugin.api.Plugin
import com.aura.ai.plugin.api.PluginManifest
import com.aura.ai.plugin.api.PluginPermission

/** The host's own bookkeeping for one plugin — what `PluginRegistry` actually stores. Distinct
 *  from [com.aura.ai.plugin.api.Plugin] itself: this wraps a `Plugin` instance with everything
 *  the host knows *about* it that the plugin itself never sees (its own [state], what it was
 *  actually [grantedPermissions] versus what it requested in [manifest]). */
data class PluginRecord(
    val plugin: Plugin,
    val manifest: PluginManifest,
    val state: PluginState,
    val grantedPermissions: Set<PluginPermission>,
    val registeredAtMillis: Long = System.currentTimeMillis(),
    val lastError: String? = null,
)
