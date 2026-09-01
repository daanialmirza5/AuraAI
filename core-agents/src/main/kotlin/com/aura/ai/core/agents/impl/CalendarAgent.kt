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

/** Wraps core-actions' `create_calendar_event` tool — covers the `Calendar` intent. Needs no
 *  permission: `ACTION_INSERT` hands the actual write off to the Calendar app itself. */
class CalendarAgent
    @Inject
    constructor(
        toolRegistry: ToolRegistry,
        permissionChecker: PermissionChecker,
        eventBus: EventBus,
    ) : ToolBackedAgent(toolRegistry, permissionChecker, eventBus) {
        override val name = "CalendarAgent"
        override val description = "Creates calendar events via the device's Calendar app."
        override val capabilities = setOf(Capability.CalendarManagement)
        override val requiredPermissions = emptyList<String>()
        override val requiredTools = listOf("create_calendar_event")
        override val supportedIntents = setOf(IntentType.Calendar)
        override val priority = 30

        override suspend fun execute(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val title = task.parameters["title"] ?: task.intent.slots["eventText"] ?: task.goal
            return runTool("create_calendar_event", mapOf("title" to title)).map { toolResult ->
                context.put("calendarEvent", toolResult.summary)
                AgentResult(summary = toolResult.summary, data = toolResult.data)
            }
        }
    }
