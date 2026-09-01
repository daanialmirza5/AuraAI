package com.aura.ai.core.reasoning.confidence

import com.aura.ai.core.reasoning.model.ConfidenceInput
import com.aura.ai.core.reasoning.model.ConfidenceScore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultConfidenceEvaluator
    @Inject
    constructor() : ConfidenceEvaluator {
        private companion object {
            const val WEIGHT_INTENT = 0.30f
            const val WEIGHT_TOOL_COVERAGE = 0.30f
            const val WEIGHT_CONSTRAINT = 0.25f
            const val WEIGHT_MEMORY = 0.15f

            /** Needed but not found — not zero, since the goal may still be resolvable without it
             *  (e.g. the user simply states the missing detail themselves next turn). */
            const val UNSATISFIED_MEMORY_NEED_SCORE = 0.3f
        }

        override fun evaluate(input: ConfidenceInput): ConfidenceScore {
            val toolCoverage =
                if (input.subGoals.isEmpty()) {
                    1f
                } else {
                    input.subGoals.count { !it.requiresAI }.toFloat() / input.subGoals.size
                }

            val constraintScore =
                if (input.constraints.isEmpty()) {
                    1f
                } else {
                    input.constraints.count { it.satisfied }.toFloat() / input.constraints.size
                }

            val memoryScore =
                when {
                    !input.decisions.requiresMemory -> 1f
                    input.hasRelevantMemory -> 1f
                    else -> UNSATISFIED_MEMORY_NEED_SCORE
                }

            val overall =
                (
                    input.intentConfidence * WEIGHT_INTENT +
                        toolCoverage * WEIGHT_TOOL_COVERAGE +
                        constraintScore * WEIGHT_CONSTRAINT +
                        memoryScore * WEIGHT_MEMORY
                ).coerceIn(0f, 1f)

            return ConfidenceScore(
                intentConfidence = input.intentConfidence,
                toolCoverageConfidence = toolCoverage,
                constraintConfidence = constraintScore,
                memoryConfidence = memoryScore,
                overall = overall,
            )
        }
    }
