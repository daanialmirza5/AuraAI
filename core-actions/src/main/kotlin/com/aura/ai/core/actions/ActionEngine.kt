package com.aura.ai.core.actions

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.tools.ToolResult

/**
 * The one thing anything above this layer needs to know: give it a tool name and arguments, get
 * back a result. It intentionally doesn't know what a [com.aura.ai.core.planner.PlanStep] or an
 * [com.aura.ai.core.planner.ExecutionPlan] is — that would couple this Android-aware module to
 * core-planner's (portable, testable) data model for no real benefit. The app module is where a
 * plan's steps get walked and handed to this one at a time.
 */
interface ActionEngine {
    /** The only entry point the reasoning/agent/plan pipeline ever calls. Refuses a tool whose
     *  [com.aura.ai.core.tools.Tool.requiresConfirmation] is `true` — see [executeConfirmed]. */
    suspend fun execute(
        toolName: String,
        arguments: Map<String, String>,
    ): AuraResult<ToolResult>

    /** Runs a tool unconditionally, bypassing the [com.aura.ai.core.tools.Tool.requiresConfirmation]
     *  gate. Reached only from a direct, explicit user action in the UI (e.g. tapping "Run" on an
     *  automation) — the user's own tap *is* the confirmation the gate exists to require. Never
     *  called from anywhere in the autonomous reasoning/agent/plan pipeline. */
    suspend fun executeConfirmed(
        toolName: String,
        arguments: Map<String, String>,
    ): AuraResult<ToolResult>
}
