package com.aura.ai.core.memory.lifecycle

import com.aura.ai.core.ai.AuraResult

/**
 * "Memory Forgetting" — the brief's four operations. All of them are policy built on top of
 * `LongTermMemoryStore`'s low-level primitives ([archive]/[delete] are direct passthroughs;
 * [expireStale] and [mergeDuplicates] are the actual decision logic that layer doesn't have).
 *
 * Forgetting here is deliberately conservative: [expireStale] *archives*, never deletes — an
 * expired memory is hidden from normal recall but still recoverable, since a wrong TTL shouldn't
 * mean permanently lost information. Only an explicit [delete] call, or a human, removes data
 * for good.
 */
interface MemoryLifecycleManager {
    /** Marks a memory [MemoryStatus.Archived][com.aura.ai.core.memory.model.MemoryStatus] —
     *  hidden from normal recall, still present and recoverable. */
    suspend fun archive(id: String): AuraResult<Unit>

    /** Permanently removes a memory. Irreversible. */
    suspend fun delete(id: String): AuraResult<Unit>

    /** Scans active long-term memories for [com.aura.ai.core.memory.model.MemoryEntry.isExpired]
     *  and archives every match. Returns the number archived. */
    suspend fun expireStale(): AuraResult<Int>

    /** Finds near-duplicate active memories (Jaccard word-overlap of [content] at or above
     *  [similarityThreshold]) and collapses each duplicate cluster into a single survivor — the
     *  highest-importance entry, with tags and relationships unioned in from the rest and
     *  confidence raised to the cluster's max — deleting the redundant copies. Returns the number
     *  of memories removed this way. */
    suspend fun mergeDuplicates(similarityThreshold: Float = 0.85f): AuraResult<Int>
}
