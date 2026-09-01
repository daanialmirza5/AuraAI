package com.aura.ai.core.memory.lifecycle

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.TextOverlap
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.store.LongTermMemoryStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMemoryLifecycleManager
    @Inject
    constructor(
        private val longTermMemoryStore: LongTermMemoryStore,
    ) : MemoryLifecycleManager {
        override suspend fun archive(id: String): AuraResult<Unit> = longTermMemoryStore.archive(id)

        override suspend fun delete(id: String): AuraResult<Unit> = longTermMemoryStore.delete(id)

        override suspend fun expireStale(): AuraResult<Int> {
            val active = longTermMemoryStore.observeAll(includeArchived = false).first()
            val expired = active.filter { it.isExpired }

            for (entry in expired) {
                val result = longTermMemoryStore.archive(entry.id)
                if (result is AuraResult.Failure) return result
            }
            return AuraResult.Success(expired.size)
        }

        override suspend fun mergeDuplicates(similarityThreshold: Float): AuraResult<Int> {
            val active = longTermMemoryStore.observeAll(includeArchived = false).first()

            // Highest importance first, so within each cluster the first entry — the one every
            // other member is compared against and the one that survives — is already the best pick.
            val byImportance = active.sortedByDescending { it.importance }
            val clusters = clusterByContentSimilarity(byImportance, similarityThreshold)

            var removedCount = 0
            for (cluster in clusters) {
                if (cluster.size <= 1) continue

                val survivor = cluster.first()
                val duplicates = cluster.drop(1)

                val mergedEntry =
                    survivor.copy(
                        tags = (survivor.tags + duplicates.flatMap { it.tags }).distinct(),
                        relationships =
                            (survivor.relationships + duplicates.flatMap { it.relationships })
                                .distinctBy { it.targetMemoryId to it.type },
                        confidence = cluster.maxOf { it.confidence },
                    )
                val updateResult = longTermMemoryStore.update(mergedEntry)
                if (updateResult is AuraResult.Failure) return updateResult

                for (duplicate in duplicates) {
                    val deleteResult = longTermMemoryStore.delete(duplicate.id)
                    if (deleteResult is AuraResult.Failure) return deleteResult
                }
                removedCount += duplicates.size
            }
            return AuraResult.Success(removedCount)
        }

        /** Greedy single-pass clustering: walk entries in order, join the first cluster whose
         *  representative (its first, best-ranked member) is similar enough, otherwise start a new
         *  one. Simple rather than exhaustive pairwise comparison — appropriate for the moderate
         *  memory counts this in-memory store is designed for. */
        private fun clusterByContentSimilarity(
            entries: List<MemoryEntry>,
            similarityThreshold: Float,
        ): List<List<MemoryEntry>> {
            val clusters = mutableListOf<MutableList<MemoryEntry>>()
            for (entry in entries) {
                val cluster =
                    clusters.firstOrNull { cluster ->
                        TextOverlap.jaccardSimilarity(cluster.first().content, entry.content) >= similarityThreshold
                    }
                if (cluster != null) cluster += entry else clusters += mutableListOf(entry)
            }
            return clusters
        }
    }
