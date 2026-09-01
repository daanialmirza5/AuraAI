package com.aura.ai.core.orchestrator.dispatch

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentHealthStatus
import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.events.AgentCompletedEvent
import com.aura.ai.core.events.EventBus
import com.aura.ai.core.events.PermissionDeniedEvent
import com.aura.ai.core.events.TaskFailedEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTaskDispatcher
    @Inject
    constructor(
        private val eventBus: EventBus,
    ) : TaskDispatcher {
        override suspend fun dispatch(
            agent: Agent,
            task: AgentTask,
            context: SharedContext,
            policy: DispatchPolicy,
        ): AgentExecutionOutcome {
            val health = agent.health()
            health.missingPermissions.forEach { permission ->
                eventBus.publish(PermissionDeniedEvent(permission = permission, agentName = agent.name))
            }

            if (health.status == AgentHealthStatus.Unavailable) {
                eventBus.publish(TaskFailedEvent(taskId = task.id, reason = health.detail, agentName = agent.name))
                return AgentExecutionOutcome(
                    agentName = agent.name,
                    taskId = task.id,
                    result = AuraResult.Failure(AuraError.NotSupported(health.detail)),
                    attempts = 0,
                    durationMillis = 0,
                )
            }

            val startedAt = System.currentTimeMillis()
            var lastResult: AuraResult<AgentResult>? = null
            var attempt = 0

            while (attempt < policy.maxAttempts) {
                attempt++
                val attemptResult = withTimeoutOrNull(policy.timeoutMillis) { agent.execute(task, context) }
                lastResult = attemptResult ?: AuraResult.Failure(
                    AuraError.Unknown("Agent '${agent.name}' timed out after ${policy.timeoutMillis}ms (attempt $attempt)."),
                )
                if (lastResult is AuraResult.Success) break
                if (attempt < policy.maxAttempts) delay(policy.retryDelayMillis)
            }

            val durationMillis = System.currentTimeMillis() - startedAt
            val finalResult = lastResult ?: AuraResult.Failure(AuraError.Unknown("Agent '${agent.name}' produced no result."))

            when (finalResult) {
                is AuraResult.Success ->
                    eventBus.publish(
                        AgentCompletedEvent(agent.name, task.id, succeeded = true, durationMillis = durationMillis),
                    )
                is AuraResult.Failure -> {
                    eventBus.publish(AgentCompletedEvent(agent.name, task.id, succeeded = false, durationMillis = durationMillis))
                    eventBus.publish(TaskFailedEvent(taskId = task.id, reason = finalResult.error.message, agentName = agent.name))
                }
            }

            return AgentExecutionOutcome(
                agentName = agent.name,
                taskId = task.id,
                result = finalResult,
                attempts = attempt,
                durationMillis = durationMillis,
            )
        }
    }
