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

/** Wraps core-actions' `shopping_search` tool — covers the `Shopping` intent. */
class ShoppingAgent
    @Inject
    constructor(
        toolRegistry: ToolRegistry,
        permissionChecker: PermissionChecker,
        eventBus: EventBus,
    ) : ToolBackedAgent(toolRegistry, permissionChecker, eventBus) {
        override val name = "ShoppingAgent"
        override val description = "Searches for items to purchase."
        override val capabilities = setOf(Capability.ShoppingSearch)
        override val requiredPermissions = emptyList<String>()
        override val requiredTools = listOf("shopping_search")
        override val supportedIntents = setOf(IntentType.Shopping)
        override val priority = 30

        override suspend fun execute(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val query = task.parameters["query"] ?: task.intent.slots["item"] ?: task.goal
            return runTool("shopping_search", mapOf("query" to query)).map { toolResult ->
                context.put("shoppingResult", toolResult.summary)
                AgentResult(summary = toolResult.summary, data = toolResult.data)
            }
        }
    }
