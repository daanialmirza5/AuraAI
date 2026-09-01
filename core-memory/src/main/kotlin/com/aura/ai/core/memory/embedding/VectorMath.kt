package com.aura.ai.core.memory.embedding

import kotlin.math.sqrt

/** The one piece of vector math this module needs, kept provider-agnostic and dependency-free
 *  (no linear-algebra library) since it only ever runs on the small vectors a text embedding
 *  produces. */
object VectorMath {
    fun cosineSimilarity(
        a: List<Float>,
        b: List<Float>,
    ): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f

        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0f) 0f else (dot / denominator).coerceIn(-1f, 1f)
    }
}
