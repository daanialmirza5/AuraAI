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
import com.aura.ai.core.events.MemoryRetrievedEvent
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.memory.extraction.MemoryExtractor
import com.aura.ai.core.memory.graph.KnowledgeGraph
import com.aura.ai.core.memory.retrieval.MemoryRetriever
import com.aura.ai.core.memory.store.LongTermMemoryStore
import javax.inject.Inject

/**
 * Wraps `core-memory`'s Phase 4 machinery — the brief's "Agent Memory Access" made concrete: this
 * is the one agent that talks to `MemoryRetriever`/`MemoryExtractor`/`LongTermMemoryStore`
 * directly, so every other agent gets memory *through* [SharedContext] (via
 * [Agent.dependsOnCapabilities] on [Capability.MemoryRecall]) rather than each independently
 * depending on `core-memory` for the same purpose.
 *
 * No `IntentType` maps to "recall memory" as a primary intent — [supportedIntents] is honestly
 * empty. This agent runs when `com.aura.ai.core.orchestrator.selection.AgentSelectionEngine`
 * pulls it in as a capability dependency of another selected agent (see [PlannerAgent]), not from
 * a direct intent match.
 */
class MemoryAgent
    @Inject
    constructor(
        private val memoryRetriever: MemoryRetriever,
        private val memoryExtractor: MemoryExtractor,
        private val longTermMemoryStore: LongTermMemoryStore,
        private val knowledgeGraph: KnowledgeGraph,
        private val eventBus: EventBus,
    ) : Agent {
        override val name = "MemoryAgent"
        override val description = "Retrieves relevant memories for a goal, or extracts and stores new ones."
        override val capabilities = setOf(Capability.MemoryRecall, Capability.MemoryStorage)
        override val requiredPermissions = emptyList<String>()
        override val requiredTools = emptyList<String>()
        override val supportedIntents = emptySet<IntentType>()
        override val priority = 10

        /** `task.parameters["mode"] == "remember"` stores; anything else (the default) recalls. */
        override suspend fun execute(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> = if (task.parameters["mode"] == "remember") remember(task, context) else recall(task, context)

        private suspend fun recall(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val result = memoryRetriever.retrieve(task.goal)
            val memories =
                when (result) {
                    is AuraResult.Success -> result.value
                    is AuraResult.Failure -> return result
                }

            eventBus.publish(MemoryRetrievedEvent(query = task.goal, memoryCount = memories.size))
            context.put("relatedMemoryCount", memories.size.toString())
            if (memories.isNotEmpty()) {
                context.put("relatedMemorySummary", memories.joinToString("; ") { it.content })
            }

            return AuraResult.Success(
                AgentResult(
                    summary =
                        if (memories.isEmpty()) {
                            "No related memories found."
                        } else {
                            "Found ${memories.size} related memor${if (memories.size == 1) "y" else "ies"}."
                        },
                    data = mapOf("memoryCount" to memories.size.toString()),
                    confidence = if (memories.isEmpty()) 0.5f else 1f,
                ),
            )
        }

        private suspend fun remember(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val content = task.parameters["content"] ?: task.goal
            val extractions = memoryExtractor.extract(content)

            var stored = 0
            for (extraction in extractions) {
                // Knowledge-graph linking happens once, right before persistence — see
                // KnowledgeGraph.autoLink's own doc for why this is the one call site, not something
                // recomputed on every read.
                val linked = knowledgeGraph.autoLink(extraction.memory)
                if (longTermMemoryStore.remember(linked) is AuraResult.Success) stored++
            }

            context.put("memoriesStored", stored.toString())
            return AuraResult.Success(
                AgentResult(summary = "Stored $stored memor${if (stored == 1) "y" else "ies"} from this task."),
            )
        }

        override suspend fun health(): AgentHealth = AgentHealth(AgentHealthStatus.Healthy, "Ready.")
    }
