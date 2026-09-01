package com.aura.ai.core.orchestrator.dispatch

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentHealth
import com.aura.ai.core.agents.AgentHealthStatus
import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.DefaultSharedContext
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.events.AgentCompletedEvent
import com.aura.ai.core.events.AuraEvent
import com.aura.ai.core.events.EventBus
import com.aura.ai.core.events.PermissionDeniedEvent
import com.aura.ai.core.events.TaskFailedEvent
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.intent.RecognizedIntent
import kotlinx.coroutines.delay
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

private class ScriptedAgent(
    override val name: String = "scripted",
    private val health: AgentHealth = AgentHealth(AgentHealthStatus.Healthy, "ok"),
    private val results: List<AuraResult<AgentResult>> = emptyList(),
    private val delayMillisPerCall: Long = 0,
) : Agent {
    override val description: String = name
    override val capabilities: Set<Capability> = emptySet()
    override val requiredPermissions: List<String> = emptyList()
    override val requiredTools: List<String> = emptyList()
    override val supportedIntents: Set<IntentType> = emptySet()

    var callCount = 0
        private set

    override suspend fun execute(
        task: AgentTask,
        context: SharedContext,
    ): AuraResult<AgentResult> {
        val result = results[callCount.coerceAtMost(results.size - 1)]
        callCount++
        if (delayMillisPerCall > 0) delay(delayMillisPerCall)
        return result
    }

    override suspend fun health(): AgentHealth = health
}

private fun task() = AgentTask(goal = "goal", intent = RecognizedIntent(type = IntentType.Automation, confidence = 1f, rawText = "goal"))

class DefaultTaskDispatcherTest {
    @Test
    fun `an unavailable agent is never executed and fails immediately with zero attempts`() =
        runTest {
            val agent = ScriptedAgent(health = AgentHealth(AgentHealthStatus.Unavailable, "no tools registered"))
            val eventBus = RecordingEventBus()
            val dispatcher = DefaultTaskDispatcher(eventBus)

            val outcome = dispatcher.dispatch(agent, task(), DefaultSharedContext("goal"), DispatchPolicy())

            assertEquals(0, outcome.attempts)
            assertEquals(0, agent.callCount)
            assertTrue(outcome.result is AuraResult.Failure)
            assertTrue(eventBus.published.any { it is TaskFailedEvent })
        }

    @Test
    fun `missing permissions on an otherwise-healthy agent are published but do not block dispatch`() =
        runTest {
            val agent =
                ScriptedAgent(
                    health =
                        AgentHealth(
                            AgentHealthStatus.Degraded,
                            "missing a permission",
                            missingPermissions = listOf("android.permission.CAMERA"),
                        ),
                    results = listOf(AuraResult.Success(AgentResult(summary = "ok"))),
                )
            val eventBus = RecordingEventBus()
            val dispatcher = DefaultTaskDispatcher(eventBus)

            val outcome = dispatcher.dispatch(agent, task(), DefaultSharedContext("goal"), DispatchPolicy())

            assertTrue(outcome.result is AuraResult.Success)
            val permissionEvent = eventBus.published.filterIsInstance<PermissionDeniedEvent>().single()
            assertEquals("android.permission.CAMERA", permissionEvent.permission)
        }

    @Test
    fun `a successful first attempt does not retry`() =
        runTest {
            val agent = ScriptedAgent(results = listOf(AuraResult.Success(AgentResult(summary = "done"))))
            val eventBus = RecordingEventBus()
            val dispatcher = DefaultTaskDispatcher(eventBus)

            val outcome = dispatcher.dispatch(agent, task(), DefaultSharedContext("goal"), DispatchPolicy(maxAttempts = 3))

            assertEquals(1, outcome.attempts)
            assertEquals(1, agent.callCount)
            assertTrue(eventBus.published.any { it is AgentCompletedEvent && it.succeeded })
        }

    @Test
    fun `a failing agent is retried up to maxAttempts, then reports the final failure`() =
        runTest {
            val failure = AuraResult.Failure(AuraError.Unknown("transient"))
            val agent = ScriptedAgent(results = listOf(failure, failure, failure))
            val eventBus = RecordingEventBus()
            val dispatcher = DefaultTaskDispatcher(eventBus)

            val outcome =
                dispatcher.dispatch(
                    agent,
                    task(),
                    DefaultSharedContext("goal"),
                    DispatchPolicy(maxAttempts = 3, retryDelayMillis = 1),
                )

            assertEquals(3, outcome.attempts)
            assertEquals(3, agent.callCount)
            assertTrue(outcome.result is AuraResult.Failure)
            assertTrue(eventBus.published.any { it is AgentCompletedEvent && !it.succeeded })
            assertTrue(eventBus.published.any { it is TaskFailedEvent })
        }

    @Test
    fun `a retry that eventually succeeds stops immediately and reports success`() =
        runTest {
            val agent =
                ScriptedAgent(
                    results =
                        listOf(
                            AuraResult.Failure(AuraError.Unknown("transient")),
                            AuraResult.Success(AgentResult(summary = "recovered")),
                        ),
                )
            val eventBus = RecordingEventBus()
            val dispatcher = DefaultTaskDispatcher(eventBus)

            val outcome =
                dispatcher.dispatch(
                    agent,
                    task(),
                    DefaultSharedContext("goal"),
                    DispatchPolicy(maxAttempts = 3, retryDelayMillis = 1),
                )

            assertEquals(2, outcome.attempts)
            assertTrue(outcome.result is AuraResult.Success)
        }

    @Test
    fun `an agent that never returns within the timeout is reported as a timeout failure`() =
        runTest {
            val agent = ScriptedAgent(results = listOf(AuraResult.Success(AgentResult(summary = "too slow"))), delayMillisPerCall = 500)
            val eventBus = RecordingEventBus()
            val dispatcher = DefaultTaskDispatcher(eventBus)

            val outcome =
                dispatcher.dispatch(
                    agent,
                    task(),
                    DefaultSharedContext("goal"),
                    DispatchPolicy(maxAttempts = 1, timeoutMillis = 10),
                )

            assertTrue(outcome.result is AuraResult.Failure)
            val error = (outcome.result as AuraResult.Failure).error
            assertTrue(error.message.contains("timed out"))
        }
}
