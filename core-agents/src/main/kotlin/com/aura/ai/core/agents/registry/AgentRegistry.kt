package com.aura.ai.core.agents.registry

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.intent.IntentType

/**
 * The single, app-wide catalog of every [Agent] currently available — the direct analogue of
 * `com.aura.ai.core.tools.ToolRegistry`, populated the same way: a Hilt `Set<Agent>`
 * multibinding, registered at app startup (see `com.aura.ai.AuraApplication.onCreate`).
 * `com.aura.ai.core.orchestrator.selection.AgentSelectionEngine` is the only thing that queries
 * this in the normal execution path.
 */
interface AgentRegistry {
    fun register(agent: Agent)

    fun unregister(name: String)

    fun get(name: String): Agent?

    fun all(): List<Agent>

    fun findByIntent(intent: IntentType): List<Agent>

    fun findByCapability(capability: Capability): List<Agent>
}
