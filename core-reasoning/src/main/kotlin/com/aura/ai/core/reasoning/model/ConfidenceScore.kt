package com.aura.ai.core.reasoning.model

/**
 * Confidence scoring, broken into the 4 things that actually drive it rather than one opaque
 * number — the same "show your work" philosophy as
 * `com.aura.ai.core.memory.model.ScoredMemory`. [overall] is a weighted combination
 * (see `ConfidenceEvaluator`); the sub-scores stay on the record so a caller (or the trace) can
 * say *why* confidence was low, not just that it was.
 */
data class ConfidenceScore(
    /** How sure `IntentRecognizer` was about what the user meant. */
    val intentConfidence: Float,
    /** What fraction of this goal's `SubGoal`s resolved to a registered local tool. */
    val toolCoverageConfidence: Float,
    /** What fraction of checked constraints (permissions, network, battery, provider) were satisfied. */
    val constraintConfidence: Float,
    /** 1.0 if the goal didn't need memory, or needed it and found it; lower if it needed relevant
     *  context that `GoalManager` couldn't find. */
    val memoryConfidence: Float,
    val overall: Float,
) {
    val isConfident: Boolean get() = overall >= 0.5f
}

/** Everything `ConfidenceEvaluator` needs beyond the raw inputs already on [ReasoningDecisions]. */
data class ConfidenceInput(
    val intentConfidence: Float,
    val decisions: ReasoningDecisions,
    val constraints: List<ConstraintCheckResult>,
    val subGoals: List<SubGoal>,
    val hasRelevantMemory: Boolean,
)
