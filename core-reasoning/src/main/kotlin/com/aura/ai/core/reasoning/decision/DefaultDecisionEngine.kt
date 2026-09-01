package com.aura.ai.core.reasoning.decision

import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.reasoning.model.ReasoningDecisions
import com.aura.ai.core.reasoning.model.SubGoal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDecisionEngine
    @Inject
    constructor(
        private val capabilityResolver: CapabilityResolver,
    ) : DecisionEngine {
        private companion object {
            /** Padded on both sides at match time so "my" doesn't false-positive inside "army."
             *  Small and readable, the same fast-heuristic philosophy as every other rule list in
             *  this codebase (`RuleBasedMemoryClassifier`, `KeywordIntentRecognizer`). */
            val PERSONAL_CONTEXT_MARKERS = listOf(" my ", " i ", " i'm ", " me ", " mine ", " our ")
        }

        override fun decide(
            goal: String,
            subGoals: List<SubGoal>,
            context: ExecutionContext,
            hasRelevantMemory: Boolean,
        ): ReasoningDecisions {
            val impliedTools = subGoals.mapNotNull { it.impliedToolName }.distinct()
            val availableTools = impliedTools.filter { capabilityResolver.isAvailable(it) }
            val requiredPermissions = capabilityResolver.requiredPermissions(impliedTools)
            val missingPermissions = requiredPermissions.filter { context.permissionState[it] != PermissionState.Granted }

            return ReasoningDecisions(
                canPerformLocally = subGoals.none { it.requiresAI },
                requiresAI = subGoals.any { it.requiresAI },
                requiresMemory = hasRelevantMemory || mentionsPersonalContext(goal),
                requiresInternet = capabilityResolver.requiresNetwork(impliedTools),
                availableTools = availableTools,
                missingPermissions = missingPermissions,
                shouldCombineTools = availableTools.size > 1,
                shouldSplitTask = subGoals.size > 1,
            )
        }

        private fun mentionsPersonalContext(goal: String): Boolean {
            val padded = " ${goal.lowercase().trim()} "
            return PERSONAL_CONTEXT_MARKERS.any { padded.contains(it) }
        }
    }
