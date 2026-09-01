package com.aura.ai.core.agents.registry

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentHealth
import com.aura.ai.core.agents.AgentHealthStatus
import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.capabilities.CapabilityRegistry
import com.aura.ai.core.intent.IntentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAgent(
    override val name: String,
    override val supportedIntents: Set<IntentType> = emptySet(),
    override val capabilities: Set<Capability> = emptySet(),
) : Agent {
    override val description: String = name
    override val requiredPermissions: List<String> = emptyList()
    override val requiredTools: List<String> = emptyList()

    override suspend fun execute(
        task: AgentTask,
        context: SharedContext,
    ): AuraResult<AgentResult> = error("not needed for this test")

    override suspend fun health(): AgentHealth = AgentHealth(AgentHealthStatus.Healthy, "ok")
}

private class RecordingCapabilityRegistry : CapabilityRegistry {
    val registered = mutableMapOf<String, Set<Capability>>()
    val unregisteredNames = mutableListOf<String>()

    override fun register(
        agentName: String,
        capabilities: Set<Capability>,
    ) {
        registered[agentName] = capabilities
    }

    override fun unregister(agentName: String) {
        registered.remove(agentName)
        unregisteredNames += agentName
    }

    override fun agentsFor(capability: Capability): Set<String> = registered.filterValues { capability in it }.keys

    override fun capabilitiesOf(agentName: String): Set<Capability> = registered[agentName].orEmpty()
}

class DefaultAgentRegistryTest {
    @Test
    fun `registering an agent also indexes its capabilities into the capability registry`() {
        val capabilityRegistry = RecordingCapabilityRegistry()
        val registry = DefaultAgentRegistry(capabilityRegistry)
        val agent = FakeAgent(name = "research", capabilities = setOf(Capability.WebSearch))

        registry.register(agent)

        assertEquals(agent, registry.get("research"))
        assertEquals(setOf(Capability.WebSearch), capabilityRegistry.registered["research"])
    }

    @Test
    fun `unregistering an agent removes it from both the agent map and the capability registry`() {
        val capabilityRegistry = RecordingCapabilityRegistry()
        val registry = DefaultAgentRegistry(capabilityRegistry)
        registry.register(FakeAgent(name = "research", capabilities = setOf(Capability.WebSearch)))

        registry.unregister("research")

        assertNull(registry.get("research"))
        assertTrue("research" in capabilityRegistry.unregisteredNames)
    }

    @Test
    fun `findByIntent returns only agents that declare support for that intent`() {
        val registry = DefaultAgentRegistry(RecordingCapabilityRegistry())
        registry.register(FakeAgent(name = "calendar", supportedIntents = setOf(IntentType.Calendar)))
        registry.register(FakeAgent(name = "shopping", supportedIntents = setOf(IntentType.Shopping)))

        val result = registry.findByIntent(IntentType.Calendar)

        assertEquals(listOf("calendar"), result.map { it.name })
    }

    @Test
    fun `findByCapability resolves through the capability registry back to real agent instances`() {
        val capabilityRegistry = RecordingCapabilityRegistry()
        val registry = DefaultAgentRegistry(capabilityRegistry)
        val agent = FakeAgent(name = "memory", capabilities = setOf(Capability.MemoryRecall, Capability.MemoryStorage))
        registry.register(agent)

        val result = registry.findByCapability(Capability.MemoryStorage)

        assertEquals(listOf(agent), result)
    }

    @Test
    fun `all returns every currently registered agent`() {
        val registry = DefaultAgentRegistry(RecordingCapabilityRegistry())
        registry.register(FakeAgent(name = "a"))
        registry.register(FakeAgent(name = "b"))

        assertEquals(setOf("a", "b"), registry.all().map { it.name }.toSet())
    }
}
