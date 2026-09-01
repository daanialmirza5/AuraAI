package com.aura.ai.core.reasoning.decision

import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.model.ReasoningDecisions
import com.aura.ai.core.reasoning.model.SubGoal

/**
 * The literal center of the brief: answers all 8 questions the Reasoning Engine "must decide" in
 * one call, given what `TaskDecomposer` broke the goal into and what `ExecutionContextProvider`
 * observed about the device. See [ReasoningDecisions] for what each of the 8 fields means.
 */
interface DecisionEngine {
    fun decide(
        goal: String,
        subGoals: List<SubGoal>,
        context: ExecutionContext,
        hasRelevantMemory: Boolean,
    ): ReasoningDecisions
}
