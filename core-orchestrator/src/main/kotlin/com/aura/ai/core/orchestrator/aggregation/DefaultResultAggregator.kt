package com.aura.ai.core.orchestrator.aggregation

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.orchestrator.dispatch.AgentExecutionOutcome
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultResultAggregator
    @Inject
    constructor() : ResultAggregator {
        override fun aggregate(outcomes: List<AgentExecutionOutcome>): AggregatedResult {
            val succeeded = outcomes.filter { it.result is AuraResult.Success }
            val failed = outcomes.filter { it.result is AuraResult.Failure }
            val succeededResults = succeeded.mapNotNull { (it.result as? AuraResult.Success)?.value }

            val overallConfidence =
                if (succeededResults.isEmpty()) {
                    0f
                } else {
                    succeededResults.map { it.confidence }.average().toFloat()
                }

            val summary =
                buildString {
                    if (succeeded.isEmpty() && failed.isEmpty()) {
                        append("No agents ran.")
                        return@buildString
                    }
                    append(
                        succeeded.joinToString(" | ") { outcome ->
                            val agentSummary = (outcome.result as? AuraResult.Success)?.value?.summary.orEmpty()
                            "${outcome.agentName}: $agentSummary"
                        },
                    )
                    if (failed.isNotEmpty()) {
                        if (succeeded.isNotEmpty()) append(" | ")
                        append(failed.joinToString(" | ") { "${it.agentName}: failed" })
                    }
                }

            val mergedData = succeededResults.fold(emptyMap<String, String>()) { acc, result -> acc + result.data }

            return AggregatedResult(
                succeeded = succeeded,
                failed = failed,
                overallConfidence = overallConfidence,
                summary = summary,
                mergedData = mergedData,
            )
        }
    }
