package com.aura.ai.core.reasoning.decision

import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.context.ConnectivityState
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.reasoning.model.SubGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A hand-written fake rather than a mocking library — [CapabilityResolver] is small enough that
 *  a fake reads as clearly as a mock would, and stays consistent with every other fake in this
 *  codebase's tests (`FakeToolRegistry`, `FakeLongTermMemoryStore`). */
private class FakeCapabilityResolver(
    private val available: Set<String> = emptySet(),
    private val permissionsByTool: Map<String, List<String>> = emptyMap(),
    private val networkTools: Set<String> = emptySet(),
) : CapabilityResolver {
    override fun availableTools(): List<ToolDescriptor> = available.map { ToolDescriptor(name = it, description = "") }

    override fun requiredPermissions(toolName: String): List<String> = permissionsByTool[toolName].orEmpty()

    override fun requiresNetwork(toolName: String): Boolean = toolName in networkTools
}

private fun emptyContext(permissionState: Map<String, PermissionState> = emptyMap()) =
    ExecutionContext(
        availableTools = emptyList(),
        connectivity = ConnectivityState.Online,
        battery = null,
        permissionState = permissionState,
        aiProviderAvailable = true,
        capturedAtMillis = 0L,
    )

private fun subGoal(
    id: String,
    requiresAI: Boolean = false,
    impliedToolName: String? = null,
) = SubGoal(id = id, description = id, priority = 0, requiresAI = requiresAI, impliedToolName = impliedToolName)

class DefaultDecisionEngineTest {
    @Test
    fun `canPerformLocally is true only when no sub-goal requires AI`() {
        val engine = DefaultDecisionEngine(FakeCapabilityResolver())

        val allLocal = engine.decide("open spotify", listOf(subGoal("1")), emptyContext(), hasRelevantMemory = false)
        assertTrue(allLocal.canPerformLocally)
        assertFalse(allLocal.requiresAI)

        val oneNeedsAi = engine.decide("write an essay", listOf(subGoal("1", requiresAI = true)), emptyContext(), hasRelevantMemory = false)
        assertFalse(oneNeedsAi.canPerformLocally)
        assertTrue(oneNeedsAi.requiresAI)
    }

    @Test
    fun `requiresMemory is true when the goal mentions personal context, even with no prior memory`() {
        val engine = DefaultDecisionEngine(FakeCapabilityResolver())

        val personal = engine.decide("remind me about my meeting", listOf(subGoal("1")), emptyContext(), hasRelevantMemory = false)
        assertTrue(personal.requiresMemory)

        val impersonal = engine.decide("what is the capital of France", listOf(subGoal("1")), emptyContext(), hasRelevantMemory = false)
        assertFalse(impersonal.requiresMemory)
    }

    @Test
    fun `requiresMemory is true when relevant memory was actually found, regardless of wording`() {
        val engine = DefaultDecisionEngine(FakeCapabilityResolver())

        val result = engine.decide("what is the capital of France", listOf(subGoal("1")), emptyContext(), hasRelevantMemory = true)

        assertTrue(result.requiresMemory)
    }

    @Test
    fun `the personal-context check does not false-positive inside an unrelated word`() {
        val engine = DefaultDecisionEngine(FakeCapabilityResolver())

        // "army" ends in "my" as a substring, with no preceding space — must not match the " my " marker.
        val result = engine.decide("what do you know about the army", listOf(subGoal("1")), emptyContext(), hasRelevantMemory = false)

        assertFalse(result.requiresMemory)
    }

    @Test
    fun `availableTools only includes implied tools the resolver actually has registered`() {
        val engine = DefaultDecisionEngine(FakeCapabilityResolver(available = setOf("open_app")))
        val subGoals = listOf(subGoal("1", impliedToolName = "open_app"), subGoal("2", impliedToolName = "not_registered"))

        val result = engine.decide("open spotify and do something else", subGoals, emptyContext(), hasRelevantMemory = false)

        assertEquals(listOf("open_app"), result.availableTools)
    }

    @Test
    fun `missingPermissions lists only permissions not confirmed granted`() {
        val resolver =
            FakeCapabilityResolver(
                available = setOf("record_audio_tool"),
                permissionsByTool = mapOf("record_audio_tool" to listOf("RECORD_AUDIO", "POST_NOTIFICATIONS")),
            )
        val engine = DefaultDecisionEngine(resolver)
        val context =
            emptyContext(
                permissionState =
                    mapOf(
                        "RECORD_AUDIO" to PermissionState.Granted,
                        "POST_NOTIFICATIONS" to PermissionState.Denied,
                    ),
            )

        val result =
            engine.decide(
                "record something",
                listOf(subGoal("1", impliedToolName = "record_audio_tool")),
                context,
                hasRelevantMemory = false,
            )

        assertEquals(listOf("POST_NOTIFICATIONS"), result.missingPermissions)
    }

    @Test
    fun `shouldCombineTools is true only when more than one distinct tool is available`() {
        val engine = DefaultDecisionEngine(FakeCapabilityResolver(available = setOf("a", "b")))
        val subGoals = listOf(subGoal("1", impliedToolName = "a"), subGoal("2", impliedToolName = "b"))

        val result = engine.decide("do two things", subGoals, emptyContext(), hasRelevantMemory = false)

        assertTrue(result.shouldCombineTools)
    }

    @Test
    fun `shouldSplitTask mirrors whether the goal decomposed into more than one sub-goal`() {
        val engine = DefaultDecisionEngine(FakeCapabilityResolver())

        val single = engine.decide("one thing", listOf(subGoal("1")), emptyContext(), hasRelevantMemory = false)
        assertFalse(single.shouldSplitTask)

        val multiple = engine.decide("two things", listOf(subGoal("1"), subGoal("2")), emptyContext(), hasRelevantMemory = false)
        assertTrue(multiple.shouldSplitTask)
    }

    @Test
    fun `requiresInternet reflects the capability resolver's own network requirement for implied tools`() {
        val engine = DefaultDecisionEngine(FakeCapabilityResolver(available = setOf("web_search"), networkTools = setOf("web_search")))

        val result =
            engine.decide(
                "search for something",
                listOf(subGoal("1", impliedToolName = "web_search")),
                emptyContext(),
                hasRelevantMemory = false,
            )

        assertTrue(result.requiresInternet)
    }
}
