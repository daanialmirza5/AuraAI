package com.aura.ai.core.memory

/**
 * Jaccard word-overlap similarity — the keyword-based fallback every embedding-aware component
 * in this module (`MemoryRanker`'s similarity/context factors, `SemanticSearch` when no
 * `EmbeddingProvider` is connected) uses today. Deliberately named to make that fallback
 * relationship explicit rather than burying a copy of this logic in each consumer.
 */
internal object TextOverlap {
    private val WORD_SPLIT = Regex("""\W+""")

    fun jaccardSimilarity(
        a: String,
        b: String,
    ): Float {
        val wordsA = tokenize(a)
        val wordsB = tokenize(b)
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0f
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    private fun tokenize(text: String): Set<String> =
        text
            .lowercase()
            .split(WORD_SPLIT)
            .filter { it.length > 2 }
            .toSet()
}
