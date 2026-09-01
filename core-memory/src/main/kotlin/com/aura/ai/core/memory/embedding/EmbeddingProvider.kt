package com.aura.ai.core.memory.embedding

import com.aura.ai.core.ai.AuraResult

/**
 * Turns text into a vector — provider-agnostic by design, exactly mirroring core-providers'
 * `AIProvider` pattern from Phase 3: one interface, swappable local or cloud implementations,
 * nothing above this layer (`MemoryRanker`, `SemanticSearch`) ever depends on which one is
 * active. [id]/[dimensions] round-trip into `com.aura.ai.core.memory.model.EmbeddingState.Computed`
 * so a stored vector always records which model produced it.
 *
 * `NoOpEmbeddingProvider` is the only implementation this phase ships, and it never succeeds —
 * see its own doc for why that's the honest, correct behavior for this phase rather than a bug.
 */
interface EmbeddingProvider {
    val id: String
    val dimensions: Int

    suspend fun embed(text: String): AuraResult<List<Float>>

    suspend fun embedBatch(texts: List<String>): AuraResult<List<List<Float>>>

    suspend fun isAvailable(): Boolean
}
