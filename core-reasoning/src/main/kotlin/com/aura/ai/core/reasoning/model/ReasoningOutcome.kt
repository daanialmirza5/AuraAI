package com.aura.ai.core.reasoning.model

import com.aura.ai.core.intent.RecognizedIntent
import com.aura.ai.core.planner.ExecutionPlan

/**
 * Everything one `ReasoningEngine.reason` call produces — the full answer to "should this
 * happen, how, and why." [executionPlan] is the literal `com.aura.ai.core.planner.Planner`
 * output; reasoning doesn't reimplement planning, it decides whether/how to invoke it and
 * validates what comes back (see `ActionValidator`). Null only when [verdict] is
 * [ReasoningVerdict.NeedsMoreInformation] or [ReasoningVerdict.Blocked] with no viable plan at all.
 */
data class ReasoningOutcome(
    val goal: String,
    val intent: RecognizedIntent,
    val verdict: ReasoningVerdict,
    val decisions: ReasoningDecisions,
    val subGoals: List<SubGoal>,
    val confidence: ConfidenceScore,
    val constraints: List<ConstraintCheckResult>,
    val alternatives: List<AlternativePlan>,
    val recoveryPlan: FailureRecoveryPlan,
    val executionPlan: ExecutionPlan?,
    val trace: ReasoningTrace,
)
