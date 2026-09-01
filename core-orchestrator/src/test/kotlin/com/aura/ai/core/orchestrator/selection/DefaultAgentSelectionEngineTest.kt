package com.aura.ai.core.orchestrator.selection

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentHealth
import com.aura.ai.core.agents.AgentHealthStatus
import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.agents.registry.AgentRegistry
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.capabilities.CapabilityRegistry
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.intent.RecognizedIntent
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeAgent(
    override val name: String,
    override val supportedIntents: Set<IntentType> = emptySet(),
    override val priority: Int = Agent.DEFAULT_PRIORITY,
    override val dependsOnCapabilities: Set<Capability> = emptySet(),
) : Agent {
    override val description: String = name
    override val capabilities: Set<Capability> = emptySet()
    override val requiredPermissions: List<String> = emptyList()
    override val requiredTools: List<String> = emptyList()

    override suspend fun execute(
        task: AgentTask,
        context: SharedContext,
    ): AuraResult<AgentResult> = error("not needed for this test")

    override suspend fun health(): AgentHealth = AgentHealth(AgentHealthStatus.Healthy, "ok")
}

private class FakeAgentRegistry(
    private val agents: List<Agent>,
) : AgentRegistry {
    override fun register(agent: Agent) = error("not needed for this test")

    override fun unregister(name: String) = error("not needed for this test")

    override fun get(name: String): Agent? = agents.firstOrNull { it.name == name }

    override fun all(): List<Agent> = agents

    override fun findByIntent(intent: IntentType): List<Agent> = agents.filter { intent in it.supportedIntents }

    override fun findByCapability(capability: Capability): List<Agent> = agents.filter { capability in it.capabilities }
}

private class FakeCapabilityRegistry(
    private val agentsByCapability: Map<Capability, Set<String>> = emptyMap(),
) : CapabilityRegistry {
    override fun register(
        agentName: String,
        capabilities: Set<Capability>,
    ) = error("not needed for this test")

    override fun unregister(agentName: String) = error("not needed for this test")

    override fun agentsFor(capability: Capability): Set<String> = agentsByCapability[capability].orEmpty()

    override fun capabilitiesOf(agentName: String): Set<Capability> = emptySet()
}

private fun intent(type: IntentType) = RecognizedIntent(type = type, confidence = 1f, rawText = "goal")

class DefaultAgentSelectionEngineTest {
    @Test
    fun `selects only the primary agent when it needs no supporting capabilities`() {
        val calendarAgent = FakeAgent(name = "calendar", supportedIntents = setOf(IntentType.Calendar))
        val engine = DefaultAgentSelectionEngine(FakeAgentRegistry(listOf(calendarAgent)), FakeCapabilityRegistry())

        val selected = engine.selectAgents(intent(IntentType.Calendar))

        assertEquals(listOf("calendar"), selected.map { it.name })
    }

    @Test
    fun `pulls in supporting agents that provide a capability the primary agent depends on`() {
        val planner =
            FakeAgent(
                name = "planner",
                supportedIntents = setOf(IntentType.Research),
                dependsOnCapabilities = setOf(Capability.MemoryRecall),
            )
        val memory = FakeAgent(name = "memory", priority = 10)
        val registry = FakeAgentRegistry(listOf(planner, memory))
        val capabilityRegistry = FakeCapabilityRegistry(mapOf(Capability.MemoryRecall to setOf("memory")))
        val engine = DefaultAgentSelectionEngine(registry, capabilityRegistry)

        val selected = engine.selectAgents(intent(IntentType.Research))

        assertEquals(setOf("planner", "memory"), selected.map { it.name }.toSet())
    }

    @Test
    fun `result is sorted by priority, lower first`() {
        val primary =
            FakeAgent(
                name = "low_priority",
                supportedIntents = setOf(IntentType.Automation),
                priority = 50,
                dependsOnCapabilities = setOf(Capability.DeviceAutomation),
            )
        val supporting = FakeAgent(name = "high_priority", priority = 5)
        val registry = FakeAgentRegistry(listOf(primary, supporting))
        val capabilityRegistry = FakeCapabilityRegistry(mapOf(Capability.DeviceAutomation to setOf("high_priority")))
        val engine = DefaultAgentSelectionEngine(registry, capabilityRegistry)

        val selected = engine.selectAgents(intent(IntentType.Automation))

        assertEquals(listOf("high_priority", "low_priority"), selected.map { it.name })
    }

    @Test
    fun `duplicate agents reachable via both the primary match and a capability dependency are not repeated`() {
        val selfDependent =
            FakeAgent(
                name = "self",
                supportedIntents = setOf(IntentType.Shopping),
                dependsOnCapabilities = setOf(Capability.ShoppingSearch),
            )
        val registry = FakeAgentRegistry(listOf(selfDependent))
        val capabilityRegistry = FakeCapabilityRegistry(mapOf(Capability.ShoppingSearch to setOf("self")))
        val engine = DefaultAgentSelectionEngine(registry, capabilityRegistry)

        val selected = engine.selectAgents(intent(IntentType.Shopping))

        assertEquals(listOf("self"), selected.map { it.name })
    }

    @Test
    fun `no matching agent for the intent yields an empty selection, not an error`() {
        val engine = DefaultAgentSelectionEngine(FakeAgentRegistry(emptyList()), FakeCapabilityRegistry())

        val selected = engine.selectAgents(intent(IntentType.Coding))

        assertEquals(emptyList<Agent>(), selected)
    }
}
