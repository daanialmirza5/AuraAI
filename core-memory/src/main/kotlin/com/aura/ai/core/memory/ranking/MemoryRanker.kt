package com.aura.ai.core.memory.ranking

import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.ScoredMemory

/**
 * Scores and sorts candidate memories against all 6 factors the brief specifies: Recency,
 * Importance, Semantic Similarity, Conversation Context, User Goals, Current Task.
 * `WeightedMemoryRanker` is the only implementation this phase ships — a weighted sum of all 6,
 * each individually visible on the returned [ScoredMemory] rather than collapsed away, so a
 * caller (or a future tuning pass) can see *why* a memory ranked where it did.
 */
interface MemoryRanker {
    suspend fun rank(
        memories: List<MemoryEntry>,
        context: RankingContext,
    ): List<ScoredMemory>
}
