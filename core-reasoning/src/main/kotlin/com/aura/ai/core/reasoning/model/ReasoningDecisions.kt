package com.aura.ai.core.reasoning.model

/**
 * The literal 8 questions the brief requires the Reasoning Engine to decide, one field each —
 * deliberately not collapsed into a single "feasible: Boolean," since a caller (or the
 * explainable trace) needs to see *which* of the 8 drove the outcome, not just that one did.
 * Produced by `DecisionEngine.decide`.
 */
data class ReasoningDecisions(
    /** "Can Android perform this?" — true if every [SubGoal] this goal decomposes into either
     *  resolves to a registered local tool or doesn't need one (pure conversation). */
    val canPerformLocally: Boolean,
    /** "Does this require AI?" — true if any [SubGoal] has no local tool that can satisfy it
     *  (e.g. writing prose, code, or any other free-form generation). */
    val requiresAI: Boolean,
    /** "Does this require memory?" — true if the goal references personal/standing context
     *  ("my," "I," a prior preference/goal/project) rather than being fully self-contained. */
    val requiresMemory: Boolean,
    /** "Does this require internet?" — true if any tool this goal would invoke needs live
     *  connectivity to do anything useful (e.g. `web_search`), as opposed to purely local
     *  device/app actions. */
    val requiresInternet: Boolean,
    /** "Which tools are available?" — the names of registered tools relevant to this goal. */
    val availableTools: List<String>,
    /** "Which permissions are missing?" — Android permission names this goal's tools need that
     *  [com.aura.ai.core.reasoning.context.PermissionChecker] could not confirm as granted.
     *  Empty, not just "false," because a caller needs to know exactly which ones. */
    val missingPermissions: List<String>,
    /** "Should multiple tools be combined?" — true if satisfying this goal is expected to invoke
     *  more than one distinct tool in sequence. */
    val shouldCombineTools: Boolean,
    /** "Should the task be split?" — true if [com.aura.ai.core.reasoning.decomposition.TaskDecomposer]
     *  produced more than one [SubGoal] for this goal. */
    val shouldSplitTask: Boolean,
)
