package com.aura.ai.core.reasoning.decomposition

import com.aura.ai.core.intent.RecognizedIntent
import com.aura.ai.core.reasoning.model.SubGoal

/**
 * "Should the task be split?" starts here: breaks a goal into an ordered, prioritized list of
 * [SubGoal]s. A single-element result *is* the answer "no, this doesn't need splitting" — callers
 * never need a separate boolean, `subGoals.size > 1` already says it (see
 * `com.aura.ai.core.reasoning.model.ReasoningDecisions.shouldSplitTask`).
 */
interface TaskDecomposer {
    fun decompose(
        goal: String,
        intent: RecognizedIntent,
    ): List<SubGoal>
}
