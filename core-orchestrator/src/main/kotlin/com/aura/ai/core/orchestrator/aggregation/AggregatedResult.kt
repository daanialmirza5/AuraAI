package com.aura.ai.core.orchestrator.aggregation

import com.aura.ai.core.orchestrator.dispatch.AgentExecutionOutcome

/**
 * "Confidence aggregation" and the "Result Aggregator" — combined into one record. [overallConfidence]
 * is the mean of every succeeded agent's own `AgentResult.confidence`; an agent that failed
 * contributes nothing to the average rather than counting as a confidence of zero, since
 * confidence is a property of an *answer*, not a stand-in for success/failure (failures are
 * already visible in [failed]).
 */
data class AggregatedResult(
    val succeeded: List<AgentExecutionOutcome>,
    val failed: List<AgentExecutionOutcome>,
    val overallConfidence: Float,
    val summary: String,
    val mergedData: Map<String, String>,
) {
    val allSucceeded: Boolean get() = failed.isEmpty()
    val partialSuccess: Boolean get() = succeeded.isNotEmpty() && failed.isNotEmpty()
}
