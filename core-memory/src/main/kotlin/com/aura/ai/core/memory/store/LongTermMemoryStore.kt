package com.aura.ai.core.memory.store

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.MemoryCategory
import com.aura.ai.core.memory.model.MemoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * The durable, searchable, rankable memory store — everything AURA has learned that's meant to
 * outlive a single session. Owns only the low-level mechanics (store, fetch, mutate, archive,
 * delete); the *policy* of what should be archived/expired/merged and when belongs to
 * `MemoryLifecycleManager`, one layer up, which is built entirely out of these primitives.
 *
 * `InMemoryLongTermMemoryStore` is this phase's only implementation — process-lifetime, not
 * persisted to disk. Swapping in a Room-backed implementation later is a pure data-layer change;
 * nothing above this interface would need to know.
 */
interface LongTermMemoryStore {
    suspend fun remember(entry: MemoryEntry): AuraResult<MemoryEntry>

    suspend fun update(entry: MemoryEntry): AuraResult<MemoryEntry>

    suspend fun get(id: String): MemoryEntry?

    /** Touches [MemoryEntry.lastAccessedAtMillis] on every entry returned — recall is itself an
     *  access, and recency scoring depends on that being true. Plain substring/tag matching
     *  today (see `SemanticSearch` for the embedding-aware alternative once one is connected). */
    suspend fun recall(
        query: String,
        limit: Int = 20,
        includeArchived: Boolean = false,
    ): AuraResult<List<MemoryEntry>>

    suspend fun byCategory(
        category: MemoryCategory,
        includeArchived: Boolean = false,
    ): AuraResult<List<MemoryEntry>>

    suspend fun archive(id: String): AuraResult<Unit>

    suspend fun delete(id: String): AuraResult<Unit>

    fun observeAll(includeArchived: Boolean = false): Flow<List<MemoryEntry>>
}
