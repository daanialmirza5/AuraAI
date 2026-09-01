package com.aura.ai.core.tools

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ToolDescriptor

/**
 * One invokable capability AURA can perform — "open_app", "set_reminder", "web_search", and so
 * on. core-actions provides the real, Android-backed implementations; the app module can equally
 * register its own (see `AppAutomationTool`, registered from the app layer, wired to the app's
 * existing device/automation repositories rather than anything generic).
 *
 * Deliberately framework-agnostic: nothing here knows about core-planner's `PlanStep` or any
 * particular AI provider's function-calling wire format — [toDescriptor] is the one place a
 * `Tool` is reduced to the shape a provider request needs.
 */
interface Tool {
    val name: String
    val description: String
    val parameters: Map<String, ParameterSchema>

    /** `true` for a tool sensitive/destructive enough that it must never run autonomously —
     *  [com.aura.ai.core.actions.ActionEngine.execute] (the path the reasoning/agent/plan pipeline
     *  always uses) refuses these; only [com.aura.ai.core.actions.ActionEngine.executeConfirmed]
     *  (reached exclusively from a direct, explicit user action in the UI — the tap itself is the
     *  confirmation) will run them. Defaults to `false` so every tool written before this flag
     *  existed keeps its exact prior behavior. */
    val requiresConfirmation: Boolean get() = false

    suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult>
}

fun Tool.toDescriptor(): ToolDescriptor =
    ToolDescriptor(
        name = name,
        description = description,
        parameters = parameters,
    )
