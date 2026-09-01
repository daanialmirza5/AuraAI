package com.aura.ai.core.memory.search

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.ScoredMemory
import com.aura.ai.core.memory.ranking.MemoryRanker
import com.aura.ai.core.memory.ranking.RankingContext
import com.aura.ai.core.memory.store.LongTermMemoryStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deliberately a thin composition of two things this module already built, rather than a third
 * place that re-implements "try embeddings, fall back to keywords": [LongTermMemoryStore] for
 * broad candidates, [MemoryRanker] (whose similarity factor already has that exact fallback
 * logic — see `WeightedMemoryRanker`) to actually rank them by relevance to [search]'s query.
 */
@Singleton
class HybridSemanticSearch
    @Inject
    constructor(
        private val longTermMemoryStore: LongTermMemoryStore,
        private val memoryRanker: MemoryRanker,
    ) : SemanticSearch {
        private companion object {
            const val CANDIDATE_POOL_SIZE = 200
        }

        override suspend fun search(
            query: String,
            limit: Int,
        ): AuraResult<List<ScoredMemory>> {
            val candidatesResult = longTermMemoryStore.recall(query = "", limit = CANDIDATE_POOL_SIZE)
            val candidates =
                when (candidatesResult) {
                    is AuraResult.Success -> candidatesResult.value
                    is AuraResult.Failure -> return candidatesResult
                }

            val ranked = memoryRanker.rank(candidates, RankingContext(query = query))
            return AuraResult.Success(ranked.take(limit))
        }
    }
