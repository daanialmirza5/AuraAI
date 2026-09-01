package com.aura.ai.core.reasoning.goal

import com.aura.ai.core.ai.AuraResult

/**
 * "Goal Manager" — turns a raw goal string into an [ActiveGoal] grounded in whatever memory
 * already exists about it. Deliberately the very first thing `ReasoningEngine.reason` calls
 * ("Understand goal" in the brief's own worked example) — everything downstream (decomposition,
 * decisions, confidence) reads [ActiveGoal.relatedMemories] rather than re-querying memory itself.
 */
interface GoalManager {
    suspend fun establishGoal(rawGoal: String): AuraResult<ActiveGoal>
}
