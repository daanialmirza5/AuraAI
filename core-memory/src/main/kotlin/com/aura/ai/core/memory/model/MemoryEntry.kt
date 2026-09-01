package com.aura.ai.core.memory.model

import java.util.UUID

/**
 * The one record shape every piece of AURA's memory system — retrieval, ranking, classification,
 * extraction, the context builder, forgetting — reads and writes. Every field the brief asked
 * for is here; two of them ([importanceScore]/"Recency Score") deserve a note:
 *
 * - **Importance** is stored directly ([importance]) — set at classification/extraction time,
 *   nothing decays it automatically.
 * - **Recency** is deliberately *not* a stored field. A stored recency number goes stale the
 *   instant time passes; instead [lastAccessedAtMillis] is the raw data, and [recencyScore]
 *   computes the actual score fresh, from elapsed time, whenever it's asked for (by the ranking
 *   module, typically). The memory still "contains" a recency score in every sense that matters
 *   — it's just computed, not cached-and-lying.
 */
data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val category: MemoryCategory,
    val tier: MemoryTier = MemoryTier.LongTerm,
    val status: MemoryStatus = MemoryStatus.Active,
    val importance: Float = 0.5f,
    val confidence: Float = 1f,
    val tags: List<String> = emptyList(),
    val relationships: List<MemoryRelationship> = emptyList(),
    val embedding: EmbeddingState = EmbeddingState.NotComputed,
    val source: MemorySource = MemorySource(MemorySourceType.Conversation),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastAccessedAtMillis: Long = createdAtMillis,
    /** Null means "never expires." When set and elapsed, `MemoryLifecycleManager.expireStale()`
     *  archives (not deletes — see [MemoryStatus]) this entry. */
    val ttlMillis: Long? = null,
) {
    val isExpired: Boolean
        get() {
            val ttl = ttlMillis ?: return false
            return System.currentTimeMillis() >= createdAtMillis + ttl
        }
}

/** Exponential recency decay: a memory accessed [nowMillis] loses roughly half its recency
 *  score every [halfLifeMillis] (default 3 days) since it was last touched. Deliberately simple
 *  and tunable rather than a fixed formula — different categories may eventually want different
 *  half-lives (a [MemoryCategory.Goal] should probably decay slower than [MemoryCategory.Episodic]),
 *  which is exactly why this is a function, not a stored value. */
fun MemoryEntry.recencyScore(
    nowMillis: Long = System.currentTimeMillis(),
    halfLifeMillis: Long = 3L * 24 * 60 * 60 * 1000,
): Float {
    val ageMillis = (nowMillis - lastAccessedAtMillis).coerceAtLeast(0)
    val halfLives = ageMillis.toDouble() / halfLifeMillis.toDouble()
    return Math.pow(0.5, halfLives).toFloat().coerceIn(0f, 1f)
}
