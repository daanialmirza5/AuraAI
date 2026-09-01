package com.aura.ai.core.orchestrator.aggregation

import com.aura.ai.core.orchestrator.dispatch.AgentExecutionOutcome

/** Combines every agent's individual [AgentExecutionOutcome] from one orchestration pass into a
 *  single [AggregatedResult] a caller can act on without inspecting each agent's result itself. */
interface ResultAggregator {
    fun aggregate(outcomes: List<AgentExecutionOutcome>): AggregatedResult
}
