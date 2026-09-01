package com.aura.ai.core.reasoning.model

/**
 * A fallback approach `ReasoningEngine` offers when the primary path has an unresolved
 * constraint — e.g. no connected AI provider, so the alternative is "produce an outline the user
 * fills in themselves" instead of full generation. Purely descriptive this phase: nothing
 * automatically executes an [AlternativePlan], it exists to be shown to the user or a future
 * orchestrator alongside the primary [com.aura.ai.core.planner.ExecutionPlan].
 */
data class AlternativePlan(
    val description: String,
    /** What's given up by taking this path instead of the primary one, e.g. "No AI-written
     *  content — the user has to write the body themselves." */
    val tradeoff: String,
)
