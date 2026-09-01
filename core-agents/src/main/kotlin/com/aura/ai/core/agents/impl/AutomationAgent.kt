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

/** Wraps the app-specific `app_automation` tool — toggles a smart home device or automation
 *  already configured in AURA. Covers the `Automation` intent. */
class AutomationAgent
    @Inject
    constructor(
        toolRegistry: ToolRegistry,
        permissionChecker: PermissionChecker,
        eventBus: EventBus,
    ) : ToolBackedAgent(toolRegistry, permissionChecker, eventBus) {
        override val name = "AutomationAgent"
        override val description = "Toggles a smart home device or automation already configured in AURA."
        override val capabilities = setOf(Capability.DeviceAutomation)
        override val requiredPermissions = emptyList<String>()
        override val requiredTools = listOf("app_automation")
        override val supportedIntents = setOf(IntentType.Automation)
        override val priority = 30

        override suspend fun execute(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val request = task.parameters["request"] ?: task.goal
            return runTool("app_automation", mapOf("request" to request)).map { toolResult ->
                context.put("automationResult", toolResult.summary)
                AgentResult(summary = toolResult.summary, data = toolResult.data)
            }
        }
    }
