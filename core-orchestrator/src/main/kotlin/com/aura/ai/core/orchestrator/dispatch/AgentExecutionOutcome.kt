package com.aura.ai.core.orchestrator.dispatch

import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.ai.AuraResult

/** What running one [com.aura.ai.core.agents.Agent] against one task produced — kept even on
 *  failure (unlike a bare `AuraResult`) so `com.aura.ai.core.orchestrator.aggregation.ResultAggregator`
 *  can report *which* agents failed, not just that something did. */
data class AgentExecutionOutcome(
    val agentName: String,
    val taskId: String,
    val result: AuraResult<AgentResult>,
    val attempts: Int,
    val durationMillis: Long,
)
