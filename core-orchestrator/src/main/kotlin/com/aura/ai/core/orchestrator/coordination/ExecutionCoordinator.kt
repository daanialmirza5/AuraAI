package com.aura.ai.core.orchestrator.coordination

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.orchestrator.dispatch.AgentExecutionOutcome

/**
 * "Parallel execution," "Sequential execution," and "Dependency ordering" — all three, from one
 * algorithm: agents are run in dependency-respecting waves (see `DefaultExecutionCoordinator`),
 * every agent within a wave in parallel, waves themselves in sequence. An agent with no
 * `Agent.dependsOnCapabilities` runs in the first wave; a chain of agents each depending on the
 * capability the previous one provides runs one per wave (fully sequential); independent agents
 * with no dependencies on each other all land in the same wave (fully parallel). Real topological
 * ordering, not a hardcoded "memory first" special case.
 */
interface ExecutionCoordinator {
    suspend fun coordinate(
        agents: List<Agent>,
        task: AgentTask,
        context: SharedContext,
    ): List<AgentExecutionOutcome>
}
