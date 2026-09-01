package com.aura.ai.plugin.marketplace

import com.aura.ai.plugin.api.PluginVersion

/** One marketplace-facing summary of a plugin — deliberately smaller than a full
 *  `com.aura.ai.plugin.api.PluginManifest`, the way a store listing is smaller than the app it
 *  describes. */
data class PluginListing(
    val id: String,
    val name: String,
    val description: String,
    val version: PluginVersion,
    val author: String,
    val installed: Boolean,
)
