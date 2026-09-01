package com.aura.ai.core.orchestrator

import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.DefaultSharedContext
import com.aura.ai.core.agents.registry.AgentRegistry
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.events.EventBus
import com.aura.ai.core.events.IntentRecognizedEvent
import com.aura.ai.core.events.ProviderConnectedEvent
import com.aura.ai.core.intent.IntentRecognizer
import com.aura.ai.core.orchestrator.aggregation.ResultAggregator
import com.aura.ai.core.orchestrator.coordination.ExecutionCoordinator
import com.aura.ai.core.orchestrator.selection.AgentSelectionEngine
import com.aura.ai.core.providers.AIProviderManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAgentOrchestrator
    @Inject
    constructor(
        private val intentRecognizer: IntentRecognizer,
        private val agentSelectionEngine: AgentSelectionEngine,
        private val executionCoordinator: ExecutionCoordinator,
        private val resultAggregator: ResultAggregator,
        private val agentRegistry: AgentRegistry,
        private val aiProviderManager: AIProviderManager,
        private val eventBus: EventBus,
    ) : AgentOrchestrator {
        override suspend fun orchestrate(
            goal: String,
            additionalAgentNames: Set<String>,
        ): AuraResult<OrchestrationResult> {
            val intentResult = intentRecognizer.recognize(goal)
            val intent =
                when (intentResult) {
                    is AuraResult.Success -> intentResult.value
                    is AuraResult.Failure -> return intentResult
                }
            eventBus.publish(
                IntentRecognizedEvent(utterance = goal, intentType = intent.type.name, confidence = intent.confidence),
            )

            publishProviderConnectedIfAvailable()

            val selected = agentSelectionEngine.selectAgents(intent)
            val explicit = additionalAgentNames.mapNotNull { agentRegistry.get(it) }
            val agents = (selected + explicit).distinctBy { it.name }

            val context = DefaultSharedContext(goal)
            val task = AgentTask(goal = goal, intent = intent)

            val outcomes = executionCoordinator.coordinate(agents, task, context)
            val aggregated = resultAggregator.aggregate(outcomes)

            return AuraResult.Success(
                OrchestrationResult(
                    goal = goal,
                    intent = intent,
                    selectedAgents = agents.map { it.name },
                    outcomes = outcomes,
                    aggregated = aggregated,
                    sharedContextSnapshot = context.snapshot(),
                ),
            )
        }

        /** Never fires today — every `AIProvider` this phase ships is scaffolded, so
         *  `isAvailable()` is always `false`. The check stays in place so the day a real provider is
         *  connected, `ProviderConnectedEvent` starts firing with no further code changes. */
        private suspend fun publishProviderConnectedIfAvailable() {
            val provider = aiProviderManager.activeProvider.value
            if (provider.isAvailable()) {
                eventBus.publish(ProviderConnectedEvent(providerId = provider.id))
            }
        }
    }
