package com.aura.ai.core.memory.store

import com.aura.ai.core.memory.model.MemoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * The session-scoped "what's currently salient" store — distinct from both `ConversationBuffer`
 * (raw dialogue turns) and `LongTermMemoryStore` (durable facts). A working-memory entry is
 * something like "the user just mentioned they're stressed about Monday's exam" — true right
 * now, worth having in context for the next few turns, and not necessarily worth keeping
 * forever. Expiry is driven by [MemoryEntry.ttlMillis]; entries without a TTL persist for the
 * process's lifetime (there's no session boundary shorter than that in this phase).
 */
interface WorkingMemoryStore {
    fun setActive(entry: MemoryEntry)

    /** Expired entries (see [MemoryEntry.isExpired]) are silently excluded, not just marked. */
    fun getActive(): List<MemoryEntry>

    fun remove(id: String)

    fun clear()

    fun observeActive(): Flow<List<MemoryEntry>>
}
