package com.aura.ai.domain.model

/** When a [Workflow] runs on its own, without a "Run Now" tap. `Manual` workflows only ever run
 *  via the explicit run action — nothing schedules them. */
enum class WorkflowTrigger { Manual, Daily, Weekly }

/** One call in a [Workflow] — the exact same `(toolName, arguments)` shape
 *  `com.aura.ai.core.actions.ActionEngine`/`com.aura.ai.core.tools.Tool` already use everywhere
 *  else, so a workflow step is never a second way to describe "run a tool." */
data class WorkflowStep(
    val toolName: String,
    val arguments: Map<String, String> = emptyMap(),
)

data class Workflow(
    val id: String,
    val name: String,
    val description: String,
    val trigger: WorkflowTrigger,
    /** Local time-of-day the trigger fires, for [WorkflowTrigger.Daily]/[WorkflowTrigger.Weekly] —
     *  ignored for [WorkflowTrigger.Manual]. */
    val triggerHour: Int = 8,
    val triggerMinute: Int = 0,
    /** 1 (Monday) through 7 (Sunday), [java.time.DayOfWeek]'s own numbering — only meaningful for
     *  [WorkflowTrigger.Weekly]. */
    val triggerDayOfWeek: Int = 1,
    val steps: List<WorkflowStep>,
    val enabled: Boolean = true,
)
