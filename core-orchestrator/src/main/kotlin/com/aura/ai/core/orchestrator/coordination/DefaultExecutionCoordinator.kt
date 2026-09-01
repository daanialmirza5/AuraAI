package com.aura.ai.core.orchestrator.coordination

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.orchestrator.dispatch.AgentExecutionOutcome
import com.aura.ai.core.orchestrator.dispatch.TaskDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultExecutionCoordinator
    @Inject
    constructor(
        private val taskDispatcher: TaskDispatcher,
    ) : ExecutionCoordinator {
        override suspend fun coordinate(
            agents: List<Agent>,
            task: AgentTask,
            context: SharedContext,
        ): List<AgentExecutionOutcome> {
            val remaining = agents.toMutableList()
            val satisfiedCapabilities = mutableSetOf<Capability>()
            val outcomes = mutableListOf<AgentExecutionOutcome>()

            while (remaining.isNotEmpty()) {
                val ready = remaining.filter { agent -> agent.dependsOnCapabilities.all { it in satisfiedCapabilities } }

                // Nothing "ready" means an unresolvable dependency (or a cycle) — run everything
                // left anyway rather than deadlock. A degraded outcome beats none at all.
                val wave = ready.ifEmpty { remaining.toList() }

                val waveOutcomes =
                    coroutineScope {
                        wave.map { agent -> async { taskDispatcher.dispatch(agent, task, context) } }.awaitAll()
                    }

                outcomes += waveOutcomes
                wave.forEach { satisfiedCapabilities += it.capabilities }
                remaining.removeAll(wave)
            }

            return outcomes
        }
    }
