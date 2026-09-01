package com.aura.ai.core.memory.retrieval

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.MemoryEntry

/**
 * The first step of "before every AI request, retrieve only relevant memories": pulls
 * *candidates* from both `LongTermMemoryStore` and `WorkingMemoryStore` — deliberately a wider,
 * cheaper net than final relevance, since narrowing to what's actually relevant is
 * `MemoryRanker`'s job, and picking the final set within budget is `ContextBuilder`'s. Keeping
 * "get candidates" and "rank candidates" as separate interfaces means either can be swapped
 * independently — a future retriever backed by a real vector index wouldn't need
 * `WeightedMemoryRanker` to change at all.
 */
interface MemoryRetriever {
    suspend fun retrieve(
        query: String,
        limit: Int = 20,
    ): AuraResult<List<MemoryEntry>>
}
