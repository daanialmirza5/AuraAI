package com.aura.ai.core.memory.model

/**
 * A [memory] plus every sub-score that went into ranking it — kept separate from [MemoryEntry]
 * itself because these scores are only meaningful *in the context of a specific ranking request*
 * (a query, a conversation, an active goal); baking them into the stored entry would make them
 * stale the moment context changes. Exposed by `MemoryRanker`, `SemanticSearch`, and
 * `ContextBuilder` alike so a caller always sees *why* a memory was chosen, not just that it was.
 */
data class ScoredMemory(
    val memory: MemoryEntry,
    val recencyScore: Float,
    val importanceScore: Float,
    val similarityScore: Float,
    val contextRelevanceScore: Float,
    val goalRelevanceScore: Float,
    val taskRelevanceScore: Float,
    val finalScore: Float,
)
