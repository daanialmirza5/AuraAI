package com.aura.ai.core.memory.model

/**
 * The "Embedding Placeholder" field every memory carries. [NotComputed] is what every memory
 * has today — no embedding provider is connected in this phase (see core-memory's `embedding`
 * package). The shape is deliberately provider-agnostic: [Computed.modelId] is what lets a
 * later phase tell a Gemini-embedded vector apart from a local-model one without needing a
 * different field, let alone a different [MemoryEntry] shape, per provider.
 *
 * `List<Float>` rather than `FloatArray` is a deliberate choice despite the extra boxing
 * overhead: `FloatArray` breaks Kotlin data class `equals()`/`hashCode()` (arrays compare by
 * reference, not content), which would silently miscompare two [Computed] states with identical
 * vectors. At the vector sizes real embedding models produce (hundreds to low thousands of
 * dimensions), that correctness is worth far more than the boxing cost.
 */
sealed interface EmbeddingState {
    data object NotComputed : EmbeddingState

    data class Computed(
        val vector: List<Float>,
        val modelId: String,
        val dimensions: Int,
    ) : EmbeddingState
}
