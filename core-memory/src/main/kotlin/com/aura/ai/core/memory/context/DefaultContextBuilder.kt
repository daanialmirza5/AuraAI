package com.aura.ai.core.memory.context

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.TextOverlap
import com.aura.ai.core.memory.model.ScoredMemory
import com.aura.ai.core.memory.ranking.MemoryRanker
import com.aura.ai.core.memory.ranking.RankingContext
import com.aura.ai.core.memory.retrieval.MemoryRetriever
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultContextBuilder
    @Inject
    constructor(
        private val memoryRetriever: MemoryRetriever,
        private val memoryRanker: MemoryRanker,
    ) : ContextBuilder {
        private companion object {
            const val CANDIDATE_POOL_SIZE = 50

            /** Above this word-overlap ratio, two memories are treated as saying the same thing —
             *  keep the higher-ranked one, drop the rest. Deliberately conservative (high threshold)
             *  since a false "duplicate" silently drops real information; a missed near-duplicate
             *  just costs a little context budget. */
            const val DUPLICATE_SIMILARITY_THRESHOLD = 0.85f
        }

        override suspend fun build(request: ContextRequest): AuraResult<BuiltContext> {
            val candidatesResult = memoryRetriever.retrieve(request.query, limit = CANDIDATE_POOL_SIZE)
            val candidates =
                when (candidatesResult) {
                    is AuraResult.Success -> candidatesResult.value
                    is AuraResult.Failure -> return candidatesResult
                }

            val ranked =
                memoryRanker.rank(
                    candidates,
                    RankingContext(
                        query = request.query,
                        conversationContext = request.conversationContext,
                        activeGoalIds = request.activeGoalIds,
                        currentTaskId = request.currentTaskId,
                    ),
                )

            val deduplicated = deduplicate(ranked)
            val (selected, wasTruncatedBySize) = selectWithinBudget(deduplicated, request.maxContextCharacters)
            val final = selected.take(request.maxMemories)

            // Deliberately doesn't count deduplication as truncation — dropping a near-duplicate is
            // a quality improvement, not a budget-forced loss of relevant information.
            val truncated = wasTruncatedBySize || deduplicated.size > request.maxMemories

            return AuraResult.Success(
                BuiltContext(
                    memories = final,
                    conversationTurns = request.conversationContext,
                    estimatedCharacterCount = final.sumOf { it.memory.content.length },
                    truncated = truncated,
                ),
            )
        }

        /** Greedy: walk memories in ranked (best-first) order, keep any that isn't a near-duplicate
         *  of something already kept. Best-first order means when two near-duplicates exist, the
         *  better-ranked one is always the one that survives. */
        private fun deduplicate(ranked: List<ScoredMemory>): List<ScoredMemory> {
            val kept = mutableListOf<ScoredMemory>()
            for (candidate in ranked) {
                val isDuplicate =
                    kept.any { existing ->
                        TextOverlap.jaccardSimilarity(existing.memory.content, candidate.memory.content) >= DUPLICATE_SIMILARITY_THRESHOLD
                    }
                if (!isDuplicate) kept += candidate
            }
            return kept
        }

        private fun selectWithinBudget(
            ranked: List<ScoredMemory>,
            maxCharacters: Int,
        ): Pair<List<ScoredMemory>, Boolean> {
            val selected = mutableListOf<ScoredMemory>()
            var runningCharacters = 0
            for (scored in ranked) {
                val cost = scored.memory.content.length
                if (runningCharacters + cost > maxCharacters && selected.isNotEmpty()) {
                    return selected to true
                }
                selected += scored
                runningCharacters += cost
            }
            return selected to false
        }
    }
