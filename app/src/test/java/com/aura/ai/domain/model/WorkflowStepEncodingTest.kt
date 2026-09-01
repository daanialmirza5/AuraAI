package com.aura.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure encode/decode round-trips — no Room, no Android. The one property that actually matters:
 *  whatever [WorkflowStepEncoding.encode] produces, [WorkflowStepEncoding.decode] must reconstruct
 *  exactly, including values that contain the encoding's own delimiter characters. */
class WorkflowStepEncodingTest {
    @Test
    fun `round-trips an empty step list`() {
        assertEquals(emptyList<WorkflowStep>(), WorkflowStepEncoding.decode(WorkflowStepEncoding.encode(emptyList())))
    }

    @Test
    fun `round-trips a single step with no arguments`() {
        val steps = listOf(WorkflowStep("media_control", emptyMap()))
        assertEquals(steps, WorkflowStepEncoding.decode(WorkflowStepEncoding.encode(steps)))
    }

    @Test
    fun `round-trips multiple steps with multiple arguments each`() {
        val steps =
            listOf(
                WorkflowStep("notify", mapOf("title" to "Morning Brief", "message" to "Good morning!")),
                WorkflowStep("shopping_search", mapOf("query" to "groceries")),
            )
        assertEquals(steps, WorkflowStepEncoding.decode(WorkflowStepEncoding.encode(steps)))
    }

    @Test
    fun `round-trips argument values that contain the encoding's own delimiter characters`() {
        val steps =
            listOf(
                WorkflowStep("notify", mapOf("message" to "A, B; C = D || E")),
            )
        assertEquals(steps, WorkflowStepEncoding.decode(WorkflowStepEncoding.encode(steps)))
    }

    @Test
    fun `decode of a blank string is an empty list, not a crash`() {
        assertEquals(emptyList<WorkflowStep>(), WorkflowStepEncoding.decode(""))
    }
}
