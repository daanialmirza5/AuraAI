package com.aura.ai.core.reasoning.model

/**
 * One unit `TaskDecomposer` breaks a goal into — the *what*, deliberately kept distinct from
 * `com.aura.ai.core.planner.PlanStep` (the *how*: a specific tool and its arguments). A goal like
 * "create my AIML assignment and save it as PDF" decomposes into `SubGoal`s ("research the
 * topic," "generate the content," "export as PDF," ...) before anything decides which tool, if
 * any, actually satisfies each one — that binding happens one layer down, in
 * `com.aura.ai.core.planner.Planner`.
 */
data class SubGoal(
    val id: String,
    val description: String,
    /** Lower runs first. Ties broken by declaration order — see `TaskDecomposer` for how
     *  priorities are assigned. */
    val priority: Int,
    /** True when nothing local can satisfy this sub-goal — it needs a connected AI provider. */
    val requiresAI: Boolean = false,
    /** The registered tool name expected to satisfy this sub-goal, if any is known ahead of
     *  planning. Null means either "no tool covers this" (see [requiresAI]) or "not yet resolved." */
    val impliedToolName: String? = null,
    val dependsOn: List<String> = emptyList(),
)
