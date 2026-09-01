package com.aura.ai.core.memory.ranking

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.TextOverlap
import com.aura.ai.core.memory.embedding.EmbeddingProvider
import com.aura.ai.core.memory.embedding.VectorMath
import com.aura.ai.core.memory.model.EmbeddingState
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.ScoredMemory
import com.aura.ai.core.memory.model.recencyScore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The default [MemoryRanker]. Each factor is computed independently and combined by
 * [weights] (defaults sum to 1.0, tunable via constructor for callers that want to bias
 * differently — e.g. a "what does the user care about right now" query might want to weight
 * [RankingWeights.contextRelevance] far higher than the defaults do).
 *
 * Semantic similarity tries [embeddingProvider] first and falls back to
 * [TextOverlap.jaccardSimilarity] against the memory's content and tags whenever the provider
 * isn't connected (today, always) or the memory has no computed embedding yet (also, today,
 * always) — see [EmbeddingProvider] for why that's correct rather than a gap.
 */
data class RankingWeights(
    val recency: Float = 0.20f,
    val importance: Float = 0.25f,
    val similarity: Float = 0.25f,
    val contextRelevance: Float = 0.15f,
    val goalRelevance: Float = 0.10f,
    val taskRelevance: Float = 0.05f,
)

@Singleton
class WeightedMemoryRanker
    @Inject
    constructor(
        private val embeddingProvider: EmbeddingProvider,
    ) : MemoryRanker {
        private val weights = RankingWeights()

        override suspend fun rank(
            memories: List<MemoryEntry>,
            context: RankingContext,
        ): List<ScoredMemory> {
            val queryEmbedding =
                context.query
                    ?.let { embeddingProvider.embed(it) }
                    ?.let { (it as? AuraResult.Success)?.value }

            val conversationText = context.conversationContext.joinToString(" ") { it.content }

            return memories
                .map { memory -> score(memory, context, queryEmbedding, conversationText) }
                .sortedByDescending { it.finalScore }
        }

        private fun score(
            memory: MemoryEntry,
            context: RankingContext,
            queryEmbedding: List<Float>?,
            conversationText: String,
        ): ScoredMemory {
            val recency = memory.recencyScore()
            val importance = memory.importance.coerceIn(0f, 1f)
            val similarity = similarityScore(memory, context.query, queryEmbedding)
            val contextRelevance =
                if (conversationText.isBlank()) {
                    0f
                } else {
                    TextOverlap.jaccardSimilarity(memory.content + " " + memory.tags.joinToString(" "), conversationText)
                }
            val goalRelevance = relevanceTo(memory, context.activeGoalIds)
            val taskRelevance = context.currentTaskId?.let { relevanceTo(memory, setOf(it)) } ?: 0f

            val finalScore =
                recency * weights.recency +
                    importance * weights.importance +
                    similarity * weights.similarity +
                    contextRelevance * weights.contextRelevance +
                    goalRelevance * weights.goalRelevance +
                    taskRelevance * weights.taskRelevance

            return ScoredMemory(
                memory = memory,
                recencyScore = recency,
                importanceScore = importance,
                similarityScore = similarity,
                contextRelevanceScore = contextRelevance,
                goalRelevanceScore = goalRelevance,
                taskRelevanceScore = taskRelevance,
                finalScore = finalScore,
            )
        }

        private fun similarityScore(
            memory: MemoryEntry,
            query: String?,
            queryEmbedding: List<Float>?,
        ): Float {
            if (query.isNullOrBlank()) return 0f

            val embeddedMemory = memory.embedding as? EmbeddingState.Computed
            if (queryEmbedding != null && embeddedMemory != null) {
                return VectorMath.cosineSimilarity(queryEmbedding, embeddedMemory.vector)
            }

            return TextOverlap.jaccardSimilarity(memory.content + " " + memory.tags.joinToString(" "), query)
        }

        private fun relevanceTo(
            memory: MemoryEntry,
            targetIds: Set<String>,
        ): Float {
            if (targetIds.isEmpty()) return 0f
            if (memory.id in targetIds) return 1f
            val relatesToTarget = memory.relationships.any { it.targetMemoryId in targetIds }
            return if (relatesToTarget) 0.75f else 0f
        }
    }
