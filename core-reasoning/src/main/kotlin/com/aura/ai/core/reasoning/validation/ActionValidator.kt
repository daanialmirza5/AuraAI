package com.aura.ai.core.reasoning.validation

import com.aura.ai.core.planner.ExecutionPlan
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.model.ConstraintCheckResult

/**
 * "Execution validation" — checked one layer later than `ConstraintEngine`: that engine judges
 * the *goal* before `com.aura.ai.core.planner.Planner` has run; this validates the *actual*
 * [ExecutionPlan] it produced, step by step. The two can disagree — a goal that looked entirely
 * local can still produce a plan with a step `Planner` decided needs a tool this device doesn't
 * have registered — which is exactly the gap this exists to catch.
 */
interface ActionValidator {
    fun validate(
        plan: ExecutionPlan,
        context: ExecutionContext,
    ): List<ConstraintCheckResult>
}
