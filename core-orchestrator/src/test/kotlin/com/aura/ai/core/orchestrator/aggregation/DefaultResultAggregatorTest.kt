package com.aura.ai.core.orchestrator.aggregation

import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.orchestrator.dispatch.AgentExecutionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun succeeded(
    agentName: String,
    summary: String,
    confidence: Float,
    data: Map<String, String> = emptyMap(),
) = AgentExecutionOutcome(
    agentName = agentName,
    taskId = "task",
    result = AuraResult.Success(AgentResult(summary = summary, data = data, confidence = confidence)),
    attempts = 1,
    durationMillis = 10L,
)

private fun failed(agentName: String) =
    AgentExecutionOutcome(
        agentName = agentName,
        taskId = "task",
        result = AuraResult.Failure(AuraError.NotSupported("boom")),
        attempts = 1,
        durationMillis = 10L,
    )

class DefaultResultAggregatorTest {
    private val aggregator = DefaultResultAggregator()

    @Test
    fun `no outcomes produces a zero-confidence result with an explanatory summary`() {
        val result = aggregator.aggregate(emptyList())

        assertEquals(0f, result.overallConfidence)
        assertEquals("No agents ran.", result.summary)
        assertTrue(result.allSucceeded)
        assertFalse(result.partialSuccess)
    }

    @Test
    fun `overallConfidence is the mean of only the succeeded agents' confidence`() {
        val outcomes = listOf(succeeded("a", "did a", confidence = 0.6f), succeeded("b", "did b", confidence = 1.0f), failed("c"))

        val result = aggregator.aggregate(outcomes)

        assertEquals(0.8f, result.overallConfidence, 0.0001f)
    }

    @Test
    fun `a fully failed batch has zero confidence, not a crash`() {
        val result = aggregator.aggregate(listOf(failed("a"), failed("b")))

        assertEquals(0f, result.overallConfidence)
        assertFalse(result.allSucceeded)
        assertFalse(result.partialSuccess)
    }

    @Test
    fun `partialSuccess is true only when both succeeded and failed are non-empty`() {
        val result = aggregator.aggregate(listOf(succeeded("a", "ok", 1f), failed("b")))

        assertTrue(result.partialSuccess)
        assertFalse(result.allSucceeded)
    }

    @Test
    fun `mergedData combines every succeeded agent's data, later agents winning on key collision`() {
        val outcomes =
            listOf(
                succeeded("a", "first", confidence = 1f, data = mapOf("x" to "1", "shared" to "from-a")),
                succeeded("b", "second", confidence = 1f, data = mapOf("y" to "2", "shared" to "from-b")),
            )

        val result = aggregator.aggregate(outcomes)

        assertEquals(mapOf("x" to "1", "y" to "2", "shared" to "from-b"), result.mergedData)
    }

    @Test
    fun `summary lists succeeded agents first, then failed agents`() {
        val outcomes = listOf(succeeded("a", "did a thing", confidence = 1f), failed("b"))

        val result = aggregator.aggregate(outcomes)

        assertEquals("a: did a thing | b: failed", result.summary)
    }

    @Test
    fun `succeeded and failed are partitioned by result type, preserving outcome order`() {
        val a = succeeded("a", "ok", 1f)
        val b = failed("b")
        val c = succeeded("c", "ok", 1f)

        val result = aggregator.aggregate(listOf(a, b, c))

        assertEquals(listOf(a, c), result.succeeded)
        assertEquals(listOf(b), result.failed)
    }
}
