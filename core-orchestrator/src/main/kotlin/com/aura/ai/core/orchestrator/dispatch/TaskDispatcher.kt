package com.aura.ai.core.orchestrator.dispatch

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext

/**
 * Runs exactly one [Agent] against one [AgentTask] — health-gated, retried, and timed out per
 * [DispatchPolicy] — and publishes every event that decision produces
 * (`com.aura.ai.core.events.PermissionDeniedEvent`, `AgentCompletedEvent`, `TaskFailedEvent`).
 * `com.aura.ai.core.orchestrator.coordination.ExecutionCoordinator` is the only caller in the
 * normal path; it invokes one [dispatch] per agent, possibly many in parallel within one wave.
 */
interface TaskDispatcher {
    suspend fun dispatch(
        agent: Agent,
        task: AgentTask,
        context: SharedContext,
        policy: DispatchPolicy = DispatchPolicy(),
    ): AgentExecutionOutcome
}
