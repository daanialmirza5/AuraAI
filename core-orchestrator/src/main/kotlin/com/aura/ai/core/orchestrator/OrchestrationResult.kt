package com.aura.ai.core.orchestrator

import com.aura.ai.core.intent.RecognizedIntent
import com.aura.ai.core.orchestrator.aggregation.AggregatedResult
import com.aura.ai.core.orchestrator.dispatch.AgentExecutionOutcome

/** Everything one `AgentOrchestrator.orchestrate` call produced: which agents ran, what each one
 *  returned, the combined [aggregated] view, and a snapshot of whatever they left in
 *  `com.aura.ai.core.agents.SharedContext` for a caller to inspect afterward. */
data class OrchestrationResult(
    val goal: String,
    val intent: RecognizedIntent,
    val selectedAgents: List<String>,
    val outcomes: List<AgentExecutionOutcome>,
    val aggregated: AggregatedResult,
    val sharedContextSnapshot: Map<String, String>,
)
