package com.aura.ai.core.ai

/**
 * The scored-keyword-matching algorithm shared by every "guess a category from trigger phrases"
 * component in this codebase — `core-intent.KeywordIntentRecognizer` and
 * `core-memory.classification.RuleBasedMemoryClassifier` both independently reimplemented this
 * exact scoring/sorting/confidence shape before it was pulled out here. Lives in `core-ai`
 * specifically because it's the one module every other core module already depends on, and this
 * utility has zero business logic of its own — it just counts, sorts, and scores.
 *
 * Not first-match: a candidate is scored by *how many* of its trigger phrases appear, so a phrase
 * that legitimately overlaps two categories resolves to whichever one actually matches more
 * strongly, rather than an arbitrary declaration order winning.
 */
object ScoredKeywordMatcher {
    data class Scored<T>(
        val value: T,
        val hits: Int,
    )

    /** [candidates] is (value, trigger phrases) pairs; returns only candidates with at least one
     *  hit, ranked highest-hit-count first. */
    fun <T> match(
        text: String,
        candidates: List<Pair<T, List<String>>>,
    ): List<Scored<T>> {
        val normalized = text.trim().lowercase()
        return candidates
            .map { (value, triggers) -> Scored(value, triggers.count { trigger -> normalized.contains(trigger) }) }
            .filter { it.hits > 0 }
            .sortedByDescending { it.hits }
    }

    /** The shared confidence curve: a heuristic ("how many rules fired"), not a calibrated
     *  probability — every caller of [match] already documents that distinction on its own
     *  output type. [base] is the confidence of a single hit; each additional hit adds [slope],
     *  capped at [ceiling] so a heuristic never claims near-certainty. */
    fun confidence(
        hits: Int,
        base: Float = 0.5f,
        slope: Float = 0.15f,
        ceiling: Float = 0.95f,
    ): Float = (base + hits * slope).coerceAtMost(ceiling)
}
