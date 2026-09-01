package com.aura.ai.core.memory.graph

import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.RelationshipType

/**
 * The relationship-aware query surface over `LongTermMemoryStore` — "graph" describes the shape
 * of the query, not a separate storage layer: [MemoryEntry.relationships] already *is* the edge
 * list (added in Phase 4, unused by any caller until this milestone), so there's no
 * `GraphNode`/`GraphEdge` table to build or keep in sync. `GoalManager`/`ReasoningEngine` can call
 * this alongside the existing `MemoryRetriever` exactly as `docs/TODO_V1.md` §2.5 asked, without
 * either of them needing to know how relationships are stored.
 */
interface KnowledgeGraph {
    /** Returns [memory] with [MemoryEntry.relationships] extended by whatever
     *  [RelationshipLinker] suggests against what's currently stored — called once, right before
     *  a newly-extracted memory is persisted (see `MemoryAgent.remember`), not on every read. */
    suspend fun autoLink(memory: MemoryEntry): MemoryEntry

    /** Every memory connected to [memoryId] in *either* direction — an outgoing edge from
     *  [memoryId] itself, or an incoming edge from some other memory that names [memoryId] as its
     *  target. Callers never need to know or care which side originally created the link. */
    suspend fun relatedTo(
        memoryId: String,
        type: RelationshipType? = null,
    ): List<MemoryEntry>

    /** [relatedTo], expanded transitively up to [depth] hops — depth 1 is exactly [relatedTo].
     *  Never revisits a memory once returned, so a cycle (A relates to B relates to A) terminates
     *  cleanly instead of looping. */
    suspend fun neighborhood(
        memoryId: String,
        depth: Int = 1,
    ): List<MemoryEntry>
}
