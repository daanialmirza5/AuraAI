package com.aura.ai.core.reasoning.confidence

import com.aura.ai.core.reasoning.model.ConfidenceInput
import com.aura.ai.core.reasoning.model.ConfidenceScore

/** "Confidence scoring" — combines how sure intent recognition was, how much of the goal maps to
 *  real tools, how many constraints are satisfied, and whether needed memory was found into one
 *  [ConfidenceScore]. */
interface ConfidenceEvaluator {
    fun evaluate(input: ConfidenceInput): ConfidenceScore
}
