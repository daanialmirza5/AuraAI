package com.aura.ai.core.agents.impl

import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.agents.ToolBackedAgent
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.map
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.events.EventBus
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.reasoning.context.PermissionChecker
import com.aura.ai.core.tools.ToolRegistry
import javax.inject.Inject

/** Wraps core-actions' `web_search` tool (opens a browser search) — covers the `Research` intent. */
class ResearchAgent
    @Inject
    constructor(
        toolRegistry: ToolRegistry,
        permissionChecker: PermissionChecker,
        eventBus: EventBus,
    ) : ToolBackedAgent(toolRegistry, permissionChecker, eventBus) {
        override val name = "ResearchAgent"
        override val description = "Researches a topic via web search."
        override val capabilities = setOf(Capability.WebSearch)
        override val requiredPermissions = emptyList<String>()
        override val requiredTools = listOf("web_search")
        override val supportedIntents = setOf(IntentType.Research)
        override val priority = 30

        override suspend fun execute(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val query = task.parameters["query"] ?: task.intent.slots["topic"] ?: task.goal
            return runTool("web_search", mapOf("query" to query)).map { toolResult ->
                context.put("researchSummary", toolResult.summary)
                AgentResult(summary = toolResult.summary, data = toolResult.data)
            }
        }
    }
