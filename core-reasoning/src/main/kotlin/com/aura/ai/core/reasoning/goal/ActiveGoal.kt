package com.aura.ai.core.reasoning.goal

import com.aura.ai.core.memory.model.MemoryEntry

/**
 * "Understand goal" made concrete: the raw request plus whatever standing context
 * `com.aura.ai.core.memory.retrieval.MemoryRetriever` already knows that's relevant to it — e.g.
 * a "Preference" memory recording "I prefer Kotlin" surfacing itself when the goal is "write me a
 * script." [relatedMemories] being empty is itself a signal `DecisionEngine`/`ConfidenceEvaluator`
 * read directly: a goal that clearly needs personal context but found none should lower
 * confidence, not silently proceed as if nothing was missing.
 */
data class ActiveGoal(
    val id: String,
    val rawText: String,
    val relatedMemories: List<MemoryEntry>,
    val establishedAtMillis: Long,
)
