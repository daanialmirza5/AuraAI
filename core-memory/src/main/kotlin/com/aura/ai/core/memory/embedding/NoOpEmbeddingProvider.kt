package com.aura.ai.core.memory.embedding

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The default (only) [EmbeddingProvider] this phase ships. Honestly reports "not connected" —
 * the same `AuraError.ProviderNotConnected` shape core-providers' `ScaffoldAIProvider` uses —
 * rather than returning a fake or zero vector that would silently poison every cosine-similarity
 * comparison downstream. Every consumer (`MemoryRanker`, `SemanticSearch`) already has a
 * keyword-based fallback for exactly this case, so nothing breaks; it just doesn't get smarter
 * than keyword matching until a real provider replaces this one.
 *
 * [dimensions] is `0` — there's no vector space to speak of yet.
 */
@Singleton
class NoOpEmbeddingProvider
    @Inject
    constructor() : EmbeddingProvider {
        override val id: String = "none"
        override val dimensions: Int = 0

        override suspend fun embed(text: String): AuraResult<List<Float>> =
            AuraResult.Failure(
                AuraError.ProviderNotConnected(
                    providerId = id,
                    message = "No embedding provider is connected yet — see core-memory/README.md.",
                ),
            )

        override suspend fun embedBatch(texts: List<String>): AuraResult<List<List<Float>>> =
            AuraResult.Failure(
                AuraError.ProviderNotConnected(
                    providerId = id,
                    message = "No embedding provider is connected yet — see core-memory/README.md.",
                ),
            )

        override suspend fun isAvailable(): Boolean = false
    }
