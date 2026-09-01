package com.aura.ai.core.planner

enum class StepStatus { Pending, Running, Completed, Failed, Skipped }

/**
 * One node in an [ExecutionPlan]. A step with [toolName] `null` means "no local tool covers
 * this — hand it to the active AI provider directly" (e.g. free-form generation, or a
 * [com.aura.ai.core.intent.IntentType.Coding] request); an executor should treat that the same
 * way it treats a tool that isn't registered yet: a [com.aura.ai.core.ai.AuraError.RequiresProvider]
 * failure in this phase, since no provider is connected.
 *
 * [parameters] values may reference a prior step's output with `${step:<id>.output}` — see
 * [PlanParameterResolver]. This is how "Export PDF" can consume what "Generate" produced without
 * the Planner needing to know anything about execution order beyond [dependsOn].
 */
data class PlanStep(
    val id: String,
    val description: String,
    val toolName: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val dependsOn: List<String> = emptyList(),
    val status: StepStatus = StepStatus.Pending,
)
