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

/**
 * Wraps core-actions' `notify` tool. No `IntentType` maps to "notification" as a primary
 * intent — this is a supporting agent, invoked explicitly (e.g. as the last step of a document
 * pipeline) rather than intent-dispatched, so [supportedIntents] is honestly empty. The one agent
 * of the 9 with a real runtime permission requirement (`POST_NOTIFICATIONS`).
 */
class NotificationAgent
    @Inject
    constructor(
        toolRegistry: ToolRegistry,
        permissionChecker: PermissionChecker,
        eventBus: EventBus,
    ) : ToolBackedAgent(toolRegistry, permissionChecker, eventBus) {
        override val name = "NotificationAgent"
        override val description = "Shows an immediate local notification."
        override val capabilities = setOf(Capability.Notification)
        override val requiredPermissions = listOf("android.permission.POST_NOTIFICATIONS")
        override val requiredTools = listOf("notify")
        override val supportedIntents = emptySet<IntentType>()
        override val priority = 90

        override suspend fun execute(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val message = task.parameters["message"] ?: task.goal
            val title = task.parameters["title"] ?: "AURA"
            return runTool("notify", mapOf("title" to title, "message" to message)).map { toolResult ->
                AgentResult(summary = toolResult.summary, data = toolResult.data)
            }
        }
    }
