package com.aura.ai.core.agents

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.events.AuraEvent
import com.aura.ai.core.events.EventBus
import com.aura.ai.core.events.ToolExecutedEvent
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.reasoning.context.PermissionChecker
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolRegistry
import com.aura.ai.core.tools.ToolResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class RecordingEventBus : EventBus {
    val published = mutableListOf<AuraEvent>()
    private val flow = MutableSharedFlow<AuraEvent>()

    override suspend fun publish(event: AuraEvent) {
        published += event
    }

    override fun events() = flow
}

private class FakeToolRegistry(
    private val tools: Map<String, Tool>,
) : ToolRegistry {
    override fun register(tool: Tool) = error("not needed for this test")

    override fun unregister(name: String) = error("not needed for this test")

    override fun get(name: String): Tool? = tools[name]

    override fun all(): List<Tool> = tools.values.toList()

    override fun descriptors(): List<ToolDescriptor> = tools.values.map { ToolDescriptor(it.name, it.description) }
}

private class FakePermissionChecker(
    private val states: Map<String, PermissionState>,
) : PermissionChecker {
    override fun check(permission: String): PermissionState = states[permission] ?: PermissionState.Unknown
}

private class FakeTool(
    override val name: String,
    private val result: AuraResult<ToolResult>,
) : Tool {
    override val description: String = name
    override val parameters: Map<String, ParameterSchema> = emptyMap()

    override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> = result
}

/** Minimal concrete subclass, mirroring how `ResearchAgent`/`CalendarAgent`/etc. extend
 *  [ToolBackedAgent] — just enough to exercise the shared [ToolBackedAgent.health] and
 *  [ToolBackedAgent.runTool] logic without any single real agent's own decision logic. */
private class TestToolBackedAgent(
    override val requiredTools: List<String>,
    override val requiredPermissions: List<String>,
    toolRegistry: ToolRegistry,
    permissionChecker: PermissionChecker,
    eventBus: EventBus,
) : ToolBackedAgent(toolRegistry, permissionChecker, eventBus) {
    override val name: String = "test_agent"
    override val description: String = "test agent"
    override val capabilities: Set<Capability> = emptySet()
    override val supportedIntents: Set<IntentType> = emptySet()

    suspend fun callTool(
        toolName: String,
        arguments: Map<String, String> = emptyMap(),
    ) = runTool(toolName, arguments)

    override suspend fun execute(
        task: AgentTask,
        context: SharedContext,
    ): AuraResult<AgentResult> = error("not needed for this test")
}

class ToolBackedAgentTest {
    @Test
    fun `health is Unavailable when any required tool is not registered`() =
        runTest {
            val agent =
                TestToolBackedAgent(
                    requiredTools = listOf("web_search"),
                    requiredPermissions = emptyList(),
                    toolRegistry = FakeToolRegistry(emptyMap()),
                    permissionChecker = FakePermissionChecker(emptyMap()),
                    eventBus = RecordingEventBus(),
                )

            val health = agent.health()

            assertEquals(AgentHealthStatus.Unavailable, health.status)
            assertEquals(listOf("web_search"), health.missingTools)
        }

    @Test
    fun `health is Degraded when tools are present but a permission is not confirmed granted`() =
        runTest {
            val tool = FakeTool("notify", AuraResult.Success(ToolResult("sent")))
            val agent =
                TestToolBackedAgent(
                    requiredTools = listOf("notify"),
                    requiredPermissions = listOf("android.permission.POST_NOTIFICATIONS"),
                    toolRegistry = FakeToolRegistry(mapOf("notify" to tool)),
                    permissionChecker = FakePermissionChecker(mapOf("android.permission.POST_NOTIFICATIONS" to PermissionState.Denied)),
                    eventBus = RecordingEventBus(),
                )

            val health = agent.health()

            assertEquals(AgentHealthStatus.Degraded, health.status)
            assertEquals(listOf("android.permission.POST_NOTIFICATIONS"), health.missingPermissions)
        }

    @Test
    fun `health is Healthy when every required tool is registered and every permission is granted`() =
        runTest {
            val tool = FakeTool("notify", AuraResult.Success(ToolResult("sent")))
            val agent =
                TestToolBackedAgent(
                    requiredTools = listOf("notify"),
                    requiredPermissions = listOf("android.permission.POST_NOTIFICATIONS"),
                    toolRegistry = FakeToolRegistry(mapOf("notify" to tool)),
                    permissionChecker = FakePermissionChecker(mapOf("android.permission.POST_NOTIFICATIONS" to PermissionState.Granted)),
                    eventBus = RecordingEventBus(),
                )

            val health = agent.health()

            assertEquals(AgentHealthStatus.Healthy, health.status)
        }

    @Test
    fun `tool availability is checked before permissions, so an unregistered tool wins over a missing permission`() =
        runTest {
            val agent =
                TestToolBackedAgent(
                    requiredTools = listOf("notify"),
                    requiredPermissions = listOf("android.permission.POST_NOTIFICATIONS"),
                    toolRegistry = FakeToolRegistry(emptyMap()),
                    permissionChecker = FakePermissionChecker(emptyMap()),
                    eventBus = RecordingEventBus(),
                )

            val health = agent.health()

            assertEquals(AgentHealthStatus.Unavailable, health.status)
        }

    @Test
    fun `runTool against an unregistered tool fails without publishing an event`() =
        runTest {
            val eventBus = RecordingEventBus()
            val agent =
                TestToolBackedAgent(
                    requiredTools = emptyList(),
                    requiredPermissions = emptyList(),
                    toolRegistry = FakeToolRegistry(emptyMap()),
                    permissionChecker = FakePermissionChecker(emptyMap()),
                    eventBus = eventBus,
                )

            val result = agent.callTool("nonexistent")

            assertTrue(result is AuraResult.Failure)
            assertTrue((result as AuraResult.Failure).error is AuraError.NotSupported)
            assertTrue(eventBus.published.isEmpty())
        }

    @Test
    fun `runTool publishes a ToolExecutedEvent reflecting success`() =
        runTest {
            val eventBus = RecordingEventBus()
            val tool = FakeTool("web_search", AuraResult.Success(ToolResult(summary = "3 results found")))
            val agent =
                TestToolBackedAgent(
                    requiredTools = emptyList(),
                    requiredPermissions = emptyList(),
                    toolRegistry = FakeToolRegistry(mapOf("web_search" to tool)),
                    permissionChecker = FakePermissionChecker(emptyMap()),
                    eventBus = eventBus,
                )

            val result = agent.callTool("web_search", mapOf("query" to "kotlin"))

            assertTrue(result is AuraResult.Success)
            val event = eventBus.published.filterIsInstance<ToolExecutedEvent>().single()
            assertEquals("web_search", event.toolName)
            assertTrue(event.succeeded)
            assertEquals("3 results found", event.summary)
        }

    @Test
    fun `runTool publishes a ToolExecutedEvent reflecting failure, using the error message as the summary`() =
        runTest {
            val eventBus = RecordingEventBus()
            val tool = FakeTool("web_search", AuraResult.Failure(AuraError.Network("offline")))
            val agent =
                TestToolBackedAgent(
                    requiredTools = emptyList(),
                    requiredPermissions = emptyList(),
                    toolRegistry = FakeToolRegistry(mapOf("web_search" to tool)),
                    permissionChecker = FakePermissionChecker(emptyMap()),
                    eventBus = eventBus,
                )

            val result = agent.callTool("web_search")

            assertTrue(result is AuraResult.Failure)
            val event = eventBus.published.filterIsInstance<ToolExecutedEvent>().single()
            assertTrue(!event.succeeded)
            assertEquals("offline", event.summary)
        }
}
