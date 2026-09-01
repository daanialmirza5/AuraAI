package com.aura.ai.plugin.api

enum class PluginHealthStatus { Healthy, Degraded, Unavailable }

/** What [Plugin.health] reports — the same "structured, not just prose" shape
 *  `com.aura.ai.core.agents.AgentHealth` established: a caller can act on [status] without
 *  parsing [detail]. */
data class PluginHealthReport(
    val status: PluginHealthStatus,
    val detail: String,
)
