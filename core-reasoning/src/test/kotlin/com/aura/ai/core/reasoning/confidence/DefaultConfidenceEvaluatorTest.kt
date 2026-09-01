package com.aura.ai.core.reasoning.confidence

import com.aura.ai.core.reasoning.model.ConfidenceInput
import com.aura.ai.core.reasoning.model.ConstraintCheckResult
import com.aura.ai.core.reasoning.model.ConstraintType
import com.aura.ai.core.reasoning.model.ReasoningDecisions
import com.aura.ai.core.reasoning.model.SubGoal
import org.junit.Assert.assertEquals
import org.junit.Test

private fun decisions(requiresMemory: Boolean = false) =
    ReasoningDecisions(
        canPerformLocally = true,
        requiresAI = false,
        requiresMemory = requiresMemory,
        requiresInternet = false,
        availableTools = emptyList(),
        missingPermissions = emptyList(),
        shouldCombineTools = false,
        shouldSplitTask = false,
    )

private fun subGoal(
    id: String,
    requiresAI: Boolean = false,
) = SubGoal(id = id, description = id, priority = 0, requiresAI = requiresAI)

private fun constraint(satisfied: Boolean) =
    ConstraintCheckResult(type = ConstraintType.Permission, description = "d", satisfied = satisfied, detail = "")

private const val DELTA = 0.0001f

class DefaultConfidenceEvaluatorTest {
    private val evaluator = DefaultConfidenceEvaluator()

    @Test
    fun `toolCoverageConfidence is 1 when there are no sub-goals`() {
        val result =
            evaluator.evaluate(
                ConfidenceInput(
                    intentConfidence = 1f,
                    decisions = decisions(),
                    constraints = emptyList(),
                    subGoals = emptyList(),
                    hasRelevantMemory = false,
                ),
            )

        assertEquals(1f, result.toolCoverageConfidence, DELTA)
    }

    @Test
    fun `toolCoverageConfidence is the fraction of sub-goals that do not require AI`() {
        val result =
            evaluator.evaluate(
                ConfidenceInput(
                    intentConfidence = 1f,
                    decisions = decisions(),
                    constraints = emptyList(),
                    subGoals = listOf(subGoal("1"), subGoal("2", requiresAI = true), subGoal("3"), subGoal("4", requiresAI = true)),
                    hasRelevantMemory = false,
                ),
            )

        assertEquals(0.5f, result.toolCoverageConfidence, DELTA)
    }

    @Test
    fun `constraintConfidence is 1 when there are no constraints`() {
        val result =
            evaluator.evaluate(
                ConfidenceInput(
                    intentConfidence = 1f,
                    decisions = decisions(),
                    constraints = emptyList(),
                    subGoals = emptyList(),
                    hasRelevantMemory = false,
                ),
            )

        assertEquals(1f, result.constraintConfidence, DELTA)
    }

    @Test
    fun `constraintConfidence is the fraction of constraints satisfied`() {
        val result =
            evaluator.evaluate(
                ConfidenceInput(
                    intentConfidence = 1f,
                    decisions = decisions(),
                    constraints = listOf(constraint(true), constraint(true), constraint(false), constraint(true)),
                    subGoals = emptyList(),
                    hasRelevantMemory = false,
                ),
            )

        assertEquals(0.75f, result.constraintConfidence, DELTA)
    }

    @Test
    fun `memoryConfidence is 1 when the decision engine did not require memory`() {
        val result =
            evaluator.evaluate(
                ConfidenceInput(
                    intentConfidence = 1f,
                    decisions = decisions(requiresMemory = false),
                    constraints = emptyList(),
                    subGoals = emptyList(),
                    hasRelevantMemory = false,
                ),
            )

        assertEquals(1f, result.memoryConfidence, DELTA)
    }

    @Test
    fun `memoryConfidence is 1 when memory was required and relevant memory was found`() {
        val result =
            evaluator.evaluate(
                ConfidenceInput(
                    intentConfidence = 1f,
                    decisions = decisions(requiresMemory = true),
                    constraints = emptyList(),
                    subGoals = emptyList(),
                    hasRelevantMemory = true,
                ),
            )

        assertEquals(1f, result.memoryConfidence, DELTA)
    }

    @Test
    fun `memoryConfidence drops to the unsatisfied-need penalty when memory was required but not found`() {
        val result =
            evaluator.evaluate(
                ConfidenceInput(
                    intentConfidence = 1f,
                    decisions = decisions(requiresMemory = true),
                    constraints = emptyList(),
                    subGoals = emptyList(),
                    hasRelevantMemory = false,
                ),
            )

        assertEquals(0.3f, result.memoryConfidence, DELTA)
    }

    @Test
    fun `overall is the documented weighted sum of the four factors`() {
        val result =
            evaluator.evaluate(
                ConfidenceInput(
                    intentConfidence = 0.8f,
                    decisions = decisions(requiresMemory = true),
                    constraints = listOf(constraint(true), constraint(false)),
                    subGoals = listOf(subGoal("1"), subGoal("2", requiresAI = true)),
                    hasRelevantMemory = false,
                ),
            )

        // intent=0.8*0.30 + toolCoverage=0.5*0.30 + constraint=0.5*0.25 + memory=0.3*0.15
        val expected = 0.8f * 0.30f + 0.5f * 0.30f + 0.5f * 0.25f + 0.3f * 0.15f
        assertEquals(expected, result.overall, DELTA)
    }

    @Test
    fun `overall is coerced into the 0 to 1 range`() {
        val result =
            evaluator.evaluate(
                ConfidenceInput(
                    intentConfidence = 1f,
                    decisions = decisions(),
                    constraints = emptyList(),
                    subGoals = emptyList(),
                    hasRelevantMemory = false,
                ),
            )

        assertEquals(1f, result.overall, DELTA)
        assertEquals(true, result.isConfident)
    }
}
