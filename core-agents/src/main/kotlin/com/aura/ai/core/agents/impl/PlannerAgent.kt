package com.aura.ai.core.agents.impl

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentHealth
import com.aura.ai.core.agents.AgentHealthStatus
import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.events.EventBus
import com.aura.ai.core.events.PlanCreatedEvent
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.planner.Planner
import com.aura.ai.core.tools.ToolRegistry
import javax.inject.Inject

/**
 * Wraps `com.aura.ai.core.planner.Planner` — the universal fallback agent, since every
 * `IntentType` resolves to *some* plan (even if that plan's steps need a provider that isn't
 * connected yet). Declares [dependsOnCapabilities] on [Capability.MemoryRecall] so
 * `ExecutionCoordinator` always runs [MemoryAgent] first when both are selected — a plan is
 * better grounded once related memory is already in [SharedContext], even though this agent
 * doesn't read that memory directly yet (a natural next step, not built this phase).
 */
class PlannerAgent
    @Inject
    constructor(
        private val planner: Planner,
        private val toolRegistry: ToolRegistry,
        private val eventBus: EventBus,
    ) : Agent {
        override val name = "PlannerAgent"
        override val description = "Decomposes a goal into an ordered, tool-bound execution plan."
        override val capabilities = setOf(Capability.GoalPlanning)
        override val requiredPermissions = emptyList<String>()
        override val requiredTools = emptyList<String>()
        override val supportedIntents = IntentType.entries.toSet()
        override val dependsOnCapabilities = setOf(Capability.MemoryRecall)
        override val priority = 20

        override suspend fun execute(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val planResult = planner.plan(task.goal, task.intent, toolRegistry.descriptors())
            val plan =
                when (planResult) {
                    is AuraResult.Success -> planResult.value
                    is AuraResult.Failure -> return planResult
                }

            eventBus.publish(PlanCreatedEvent(goal = task.goal, planId = plan.id, stepCount = plan.steps.size))
            context.put("planId", plan.id)
            context.put("planStepCount", plan.steps.size.toString())

            return AuraResult.Success(
                AgentResult(
                    summary = "Generated a ${plan.steps.size}-step plan for \"${task.goal}\".",
                    data = mapOf("planId" to plan.id, "stepCount" to plan.steps.size.toString()),
                ),
            )
        }

        override suspend fun health(): AgentHealth = AgentHealth(AgentHealthStatus.Healthy, "Ready.")
    }
