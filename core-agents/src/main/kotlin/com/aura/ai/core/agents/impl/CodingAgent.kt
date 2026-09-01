package com.aura.ai.core.agents.impl

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentHealth
import com.aura.ai.core.agents.AgentHealthStatus
import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.ai.AiMessage
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.MessageRole
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.events.EventBus
import com.aura.ai.core.events.TaskFailedEvent
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.providers.AIProviderManager
import javax.inject.Inject

/**
 * No local tool can write code — this agent's whole job genuinely requires a connected AI
 * provider. It still makes a real `AIProviderManager.generate` call (a valid text
 * `GenerationRequest` is entirely constructible today) rather than failing immediately; the
 * honest gap is downstream, in every `AIProvider` this phase ships being scaffolded — see
 * `com.aura.ai.core.providers.ScaffoldAIProvider`.
 */
class CodingAgent
    @Inject
    constructor(
        private val providerManager: AIProviderManager,
        private val eventBus: EventBus,
    ) : Agent {
        override val name = "CodingAgent"
        override val description = "Writes or reviews code. Requires a connected AI provider."
        override val capabilities = setOf(Capability.CodeGeneration, Capability.CodeReview)
        override val requiredPermissions = emptyList<String>()
        override val requiredTools = emptyList<String>()
        override val supportedIntents = setOf(IntentType.Coding)
        override val priority = 30

        override suspend fun execute(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val request = GenerationRequest(messages = listOf(AiMessage(MessageRole.User, task.goal)))
            return when (val result = providerManager.generate(request)) {
                is AuraResult.Success -> AuraResult.Success(AgentResult(summary = result.value.text))
                is AuraResult.Failure -> {
                    eventBus.publish(TaskFailedEvent(taskId = task.id, reason = result.error.message, agentName = name))
                    result
                }
            }
        }

        override suspend fun health(): AgentHealth {
            val available = providerManager.activeProvider.value.isAvailable()
            return if (available) {
                AgentHealth(AgentHealthStatus.Healthy, "A connected AI provider is available.")
            } else {
                AgentHealth(AgentHealthStatus.Degraded, "No AI provider is connected yet — code generation will fail until one is.")
            }
        }
    }
