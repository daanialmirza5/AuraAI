package com.aura.ai.core.memory.search

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.ScoredMemory

/**
 * Search memories by meaning, not just keyword — the explicit "Semantic Search Interface" the
 * brief asks for. `HybridSemanticSearch` is the only implementation this phase ships: it always
 * *tries* embedding-based cosine similarity first and transparently falls back to keyword
 * overlap when no `EmbeddingProvider` is connected (today, always) — callers never see the
 * difference in the shape of the result, only (honestly) in its quality.
 */
interface SemanticSearch {
    suspend fun search(
        query: String,
        limit: Int = 10,
    ): AuraResult<List<ScoredMemory>>
}
