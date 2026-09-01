package com.aura.ai.core.orchestrator.selection

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.registry.AgentRegistry
import com.aura.ai.core.capabilities.CapabilityRegistry
import com.aura.ai.core.intent.RecognizedIntent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAgentSelectionEngine
    @Inject
    constructor(
        private val agentRegistry: AgentRegistry,
        private val capabilityRegistry: CapabilityRegistry,
    ) : AgentSelectionEngine {
        override fun selectAgents(intent: RecognizedIntent): List<Agent> {
            val primary = agentRegistry.findByIntent(intent.type)
            val requiredCapabilities = primary.flatMap { it.dependsOnCapabilities }.toSet()
            val supporting =
                requiredCapabilities
                    .flatMap { capabilityRegistry.agentsFor(it) }
                    .mapNotNull { agentRegistry.get(it) }

            return (primary + supporting).distinctBy { it.name }.sortedBy { it.priority }
        }
    }
