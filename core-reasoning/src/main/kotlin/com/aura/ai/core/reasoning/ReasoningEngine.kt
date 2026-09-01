package com.aura.ai.core.reasoning

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.intent.RecognizedIntent
import com.aura.ai.core.reasoning.model.ReasoningOutcome

/**
 * The reasoning layer that sits between `com.aura.ai.core.intent.IntentRecognizer` and
 * `com.aura.ai.core.planner.Planner` — everything the brief lists (Decision Engine, Goal Manager,
 * Task Decomposer, Constraint Engine, Confidence Evaluator, Execution Context, Capability
 * Resolver, Action Validator) is a stage this composes, in the order the brief's own worked
 * example lays out: understand the goal, determine what's missing, decide what's required,
 * generate (and validate) an execution plan. `DefaultReasoningEngine` is the only implementation.
 */
interface ReasoningEngine {
    suspend fun reason(
        goal: String,
        intent: RecognizedIntent,
    ): AuraResult<ReasoningOutcome>
}
