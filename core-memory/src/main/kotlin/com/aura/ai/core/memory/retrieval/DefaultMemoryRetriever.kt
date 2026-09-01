package com.aura.ai.core.memory.retrieval

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.store.LongTermMemoryStore
import com.aura.ai.core.memory.store.WorkingMemoryStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMemoryRetriever
    @Inject
    constructor(
        private val longTermMemoryStore: LongTermMemoryStore,
        private val workingMemoryStore: WorkingMemoryStore,
    ) : MemoryRetriever {
        override suspend fun retrieve(
            query: String,
            limit: Int,
        ): AuraResult<List<MemoryEntry>> {
            val working = workingMemoryStore.getActive()
            val longTermResult = longTermMemoryStore.recall(query, limit = limit)
            val longTerm = (longTermResult as? AuraResult.Success)?.value ?: return longTermResult

            // Working memory entries take priority — they're what's salient *right now* — with
            // long-term recall filling the rest of the budget, deduplicated by id.
            val seen = HashSet<String>()
            val merged = (working + longTerm).filter { seen.add(it.id) }.take(limit)
            return AuraResult.Success(merged)
        }
    }
