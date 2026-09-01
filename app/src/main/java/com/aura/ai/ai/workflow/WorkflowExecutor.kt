package com.aura.ai.ai.workflow

import com.aura.ai.core.actions.ActionEngine
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.tools.ToolResult
import com.aura.ai.domain.model.Workflow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a [Workflow]'s steps in order, stopping at the first failure — the same stop-on-first-
 * failure convention `com.aura.ai.ai.PlanExecutor` already uses for `ExecutionPlan`, kept
 * consistent rather than inventing a second policy (partial-success, best-effort, etc.) for a
 * shape that's otherwise identical. Calls [ActionEngine.executeConfirmed] (not `execute`):
 * creating or enabling a scheduled workflow is itself the deliberate, explicit user action that
 * authorizes everything it will later do unattended — the same reasoning
 * `docs/ANDROID_AUTOMATION.md` §2 applies to a single Quick Action tap, extended to "the user
 * built and turned on this whole routine."
 */
@Singleton
class WorkflowExecutor
    @Inject
    constructor(
        private val actionEngine: ActionEngine,
    ) {
        suspend fun execute(workflow: Workflow): AuraResult<List<ToolResult>> {
            val results = mutableListOf<ToolResult>()
            for (step in workflow.steps) {
                when (val result = actionEngine.executeConfirmed(step.toolName, step.arguments)) {
                    is AuraResult.Success -> results += result.value
                    is AuraResult.Failure -> return AuraResult.Failure(result.error)
                }
            }
            return AuraResult.Success(results)
        }
    }
