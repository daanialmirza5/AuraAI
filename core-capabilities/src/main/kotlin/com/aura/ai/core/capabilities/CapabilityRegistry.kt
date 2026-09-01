package com.aura.ai.core.capabilities

/**
 * The bidirectional index between agents and what they can do — "which agent(s) provide
 * capability X" and "what can agent Y do" are both O(1) lookups. Distinct from
 * `com.aura.ai.core.agents.registry.AgentRegistry` (which knows about full `Agent` objects, their
 * tools, their permissions) on purpose: this registry only ever deals in names and capabilities,
 * so `com.aura.ai.core.orchestrator.selection.AgentSelectionEngine` can resolve a capability
 * dependency into candidate agent *names* without needing `core-agents` for anything beyond a
 * final `AgentRegistry.get(name)` lookup.
 */
interface CapabilityRegistry {
    fun register(
        agentName: String,
        capabilities: Set<Capability>,
    )

    fun unregister(agentName: String)

    fun agentsFor(capability: Capability): Set<String>

    fun capabilitiesOf(agentName: String): Set<Capability>
}
