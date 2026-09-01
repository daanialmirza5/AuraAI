package com.aura.ai.core.capabilities

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCapabilityRegistry
    @Inject
    constructor() : CapabilityRegistry {
        private val byAgent = ConcurrentHashMap<String, Set<Capability>>()
        private val byCapability = ConcurrentHashMap<Capability, MutableSet<String>>()

        override fun register(
            agentName: String,
            capabilities: Set<Capability>,
        ) {
            unregister(agentName)
            byAgent[agentName] = capabilities
            capabilities.forEach { capability ->
                byCapability.computeIfAbsent(capability) { ConcurrentHashMap.newKeySet() }.add(agentName)
            }
        }

        override fun unregister(agentName: String) {
            val previous = byAgent.remove(agentName) ?: return
            previous.forEach { capability -> byCapability[capability]?.remove(agentName) }
        }

        override fun agentsFor(capability: Capability): Set<String> = byCapability[capability]?.toSet() ?: emptySet()

        override fun capabilitiesOf(agentName: String): Set<Capability> = byAgent[agentName] ?: emptySet()
    }
