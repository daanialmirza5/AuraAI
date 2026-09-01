package com.aura.ai.core.reasoning.constraint

import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.model.ConstraintCheckResult

/**
 * "Constraint checking" — turns the raw facts on an [ExecutionContext] into named, explainable
 * pass/fail checks: tool availability, permissions, network, battery, and (when the goal needs
 * one) a connected AI provider. Every [ConstraintCheckResult.detail] is written to be used
 * directly as a `ReasoningTrace` reason — this is where "Storage available," "PDF exporter
 * registered," and "No network required" actually get produced.
 */
interface ConstraintEngine {
    fun evaluate(
        requiredToolNames: List<String>,
        requiresAI: Boolean,
        requiresInternet: Boolean,
        context: ExecutionContext,
    ): List<ConstraintCheckResult>
}
