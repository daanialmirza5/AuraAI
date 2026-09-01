package com.aura.ai.core.agents.registry

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.capabilities.CapabilityRegistry
import com.aura.ai.core.intent.IntentType
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registering an [Agent] here also indexes it into [capabilityRegistry] — the one place those two
 * registries are kept in sync, so nothing that calls [register] has to remember to update both.
 */
@Singleton
class DefaultAgentRegistry
    @Inject
    constructor(
        private val capabilityRegistry: CapabilityRegistry,
    ) : AgentRegistry {
        private val agents = ConcurrentHashMap<String, Agent>()

        override fun register(agent: Agent) {
            agents[agent.name] = agent
            capabilityRegistry.register(agent.name, agent.capabilities)
        }

        override fun unregister(name: String) {
            agents.remove(name)
            capabilityRegistry.unregister(name)
        }

        override fun get(name: String): Agent? = agents[name]

        override fun all(): List<Agent> = agents.values.toList()

        override fun findByIntent(intent: IntentType): List<Agent> = agents.values.filter { intent in it.supportedIntents }

        override fun findByCapability(capability: Capability): List<Agent> =
            capabilityRegistry.agentsFor(capability).mapNotNull { agents[it] }
    }
