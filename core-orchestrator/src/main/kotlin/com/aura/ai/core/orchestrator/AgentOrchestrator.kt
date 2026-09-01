package com.aura.ai.core.orchestrator

import com.aura.ai.core.ai.AuraResult

/**
 * The single entry point into AURA's multi-agent system: give it a raw goal, get back every
 * agent's contribution, aggregated. Internally recognizes intent, selects agents (by intent match
 * plus capability-dependency closure), coordinates their execution (parallel within a dependency
 * wave, sequential across waves), and aggregates the results — see `docs/EXECUTION_PIPELINE.md`
 * for the full sequence.
 */
interface AgentOrchestrator {
    /** [additionalAgentNames] force-includes agents beyond intent/capability selection — e.g.
     *  `"NotificationAgent"`, which (like [com.aura.ai.core.agents.impl.VisionAgent]) has no
     *  `IntentType` of its own and is otherwise only reachable as a capability dependency. */
    suspend fun orchestrate(
        goal: String,
        additionalAgentNames: Set<String> = emptySet(),
    ): AuraResult<OrchestrationResult>
}
