package com.aura.ai.ai.workflow

import com.aura.ai.core.actions.ActionEngine
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.tools.ToolResult
import com.aura.ai.domain.model.Workflow
import com.aura.ai.domain.model.WorkflowStep
import com.aura.ai.domain.model.WorkflowTrigger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Records every call it receives (name only) rather than doing anything real — enough to assert
 *  both "did this step run" and "did execution stop where it should have." */
private class RecordingActionEngine(
    private val failOn: String? = null,
) : ActionEngine {
    val calls = mutableListOf<String>()

    // Deliberately behaves differently from executeConfirmed() (always refuses) so a test can
    // prove which one WorkflowExecutor actually calls, rather than the two being indistinguishable.
    override suspend fun execute(
        toolName: String,
        arguments: Map<String, String>,
    ): AuraResult<ToolResult> = AuraResult.Failure(AuraError.NotSupported("execute() should never be called by WorkflowExecutor"))

    override suspend fun executeConfirmed(
        toolName: String,
        arguments: Map<String, String>,
    ): AuraResult<ToolResult> {
        calls += toolName
        return if (toolName == failOn) {
            AuraResult.Failure(AuraError.Unknown("simulated failure"))
        } else {
            AuraResult.Success(ToolResult(summary = "ran $toolName"))
        }
    }
}

private fun workflow(steps: List<WorkflowStep>) =
    Workflow(
        id = "test",
        name = "Test Workflow",
        description = "",
        trigger = WorkflowTrigger.Manual,
        steps = steps,
    )

class WorkflowExecutorTest {
    @Test
    fun `runs every step in order when all succeed`() =
        runBlocking {
            val engine = RecordingActionEngine()
            val executor = WorkflowExecutor(engine)
            val wf = workflow(listOf(WorkflowStep("media_control", mapOf("action" to "pause")), WorkflowStep("notify")))

            val result = executor.execute(wf)

            assertEquals(listOf("media_control", "notify"), engine.calls)
            assertTrue(result is AuraResult.Success)
            assertEquals(2, (result as AuraResult.Success).value.size)
        }

    @Test
    fun `stops at the first failing step and never runs the rest`() =
        runBlocking {
            val engine = RecordingActionEngine(failOn = "media_control")
            val executor = WorkflowExecutor(engine)
            val wf = workflow(listOf(WorkflowStep("media_control"), WorkflowStep("notify"), WorkflowStep("shopping_search")))

            val result = executor.execute(wf)

            assertEquals(listOf("media_control"), engine.calls)
            assertTrue(result is AuraResult.Failure)
        }

    @Test
    fun `an empty workflow succeeds trivially`() =
        runBlocking {
            val engine = RecordingActionEngine()
            val executor = WorkflowExecutor(engine)

            val result = executor.execute(workflow(emptyList()))

            assertTrue(result is AuraResult.Success)
            assertTrue((result as AuraResult.Success).value.isEmpty())
        }

    @Test
    fun `uses executeConfirmed, not execute, so confirmation-gated steps still run`() =
        runBlocking {
            // RecordingActionEngine.execute() unconditionally fails — if WorkflowExecutor called it
            // instead of executeConfirmed(), this would come back Failure, not Success.
            val engine = RecordingActionEngine()
            val executor = WorkflowExecutor(engine)

            val result = executor.execute(workflow(listOf(WorkflowStep("schedule_action"))))

            assertEquals(listOf("schedule_action"), engine.calls)
            assertTrue(result is AuraResult.Success)
        }
}
