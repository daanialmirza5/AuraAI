package com.aura.ai.core.agents

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.events.EventBus
import com.aura.ai.core.events.ToolExecutedEvent
import com.aura.ai.core.reasoning.context.PermissionChecker
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.tools.ToolRegistry
import com.aura.ai.core.tools.ToolResult

/**
 * Shared implementation for the 5 agents that do their real work by invoking exactly one
 * registered `com.aura.ai.core.tools.Tool` — [ResearchAgent], [CalendarAgent], [AutomationAgent],
 * [ShoppingAgent], [NotificationAgent] via `impl/`. [health] and [runTool] are written once here
 * instead of five times: [health] reports [AgentHealthStatus.Unavailable] if any [Agent.requiredTools]
 * isn't registered (nothing this agent does is possible), [AgentHealthStatus.Degraded] if a
 * required permission isn't confirmed granted (execution will likely fail, but the tool itself
 * exists), and reuses `com.aura.ai.core.reasoning.context.PermissionChecker` — the same real,
 * Android-backed permission check `core-reasoning` already built — rather than reinventing it.
 */
abstract class ToolBackedAgent(
    protected val toolRegistry: ToolRegistry,
    protected val permissionChecker: PermissionChecker,
    protected val eventBus: EventBus,
) : Agent {
    override suspend fun health(): AgentHealth {
        val missingTools = requiredTools.filterNot { toolRegistry.get(it) != null }
        val missingPermissions =
            requiredPermissions.filterNot {
                permissionChecker.check(it) == PermissionState.Granted
            }
        return when {
            missingTools.isNotEmpty() ->
                AgentHealth(
                    status = AgentHealthStatus.Unavailable,
                    detail = "Missing required tool(s): ${missingTools.joinToString()}.",
                    missingTools = missingTools,
                )
            missingPermissions.isNotEmpty() ->
                AgentHealth(
                    status = AgentHealthStatus.Degraded,
                    detail = "Missing permission(s): ${missingPermissions.joinToString()}.",
                    missingPermissions = missingPermissions,
                )
            else -> AgentHealth(status = AgentHealthStatus.Healthy, detail = "Ready.")
        }
    }

    /** Looks up [toolName] in the registry and runs it, publishing a [ToolExecutedEvent]
     *  regardless of outcome — the one place every tool-backed agent's tool call actually happens. */
    protected suspend fun runTool(
        toolName: String,
        arguments: Map<String, String>,
    ): AuraResult<ToolResult> {
        val tool =
            toolRegistry.get(toolName)
                ?: return AuraResult.Failure(AuraError.NotSupported("No tool registered for '$toolName'."))

        val result = tool.execute(arguments)
        eventBus.publish(
            ToolExecutedEvent(
                toolName = toolName,
                succeeded = result is AuraResult.Success,
                summary =
                    when (result) {
                        is AuraResult.Success -> result.value.summary
                        is AuraResult.Failure -> result.error.message
                    },
            ),
        )
        return result
    }
}
