package com.aura.ai.core.memory.graph

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.MemoryCategory
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.RelationshipType
import com.aura.ai.core.memory.store.LongTermMemoryStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultKnowledgeGraph
    @Inject
    constructor(
        private val store: LongTermMemoryStore,
    ) : KnowledgeGraph {
        override suspend fun autoLink(memory: MemoryEntry): MemoryEntry {
            // Only Project/Person are ever link *targets* today (see RelationshipLinker) — fetching
            // just those two categories instead of the whole store keeps this cheap regardless of how
            // large long-term memory grows.
            val projects = (store.byCategory(MemoryCategory.Project) as? AuraResult.Success)?.value.orEmpty()
            val people = (store.byCategory(MemoryCategory.Person) as? AuraResult.Success)?.value.orEmpty()
            val suggested = RelationshipLinker.suggestRelationships(memory, projects + people)
            if (suggested.isEmpty()) return memory
            return memory.copy(relationships = (memory.relationships + suggested).distinctBy { it.targetMemoryId to it.type })
        }

        override suspend fun relatedTo(
            memoryId: String,
            type: RelationshipType?,
        ): List<MemoryEntry> {
            val all = store.observeAll().first()
            val self = all.firstOrNull { it.id == memoryId } ?: return emptyList()

            val outgoingIds =
                self.relationships
                    .filter { type == null || it.type == type }
                    .map { it.targetMemoryId }
                    .toSet()
            val incoming =
                all.filter { candidate ->
                    candidate.id != memoryId &&
                        candidate.relationships.any {
                            it.targetMemoryId == memoryId && (type == null || it.type == type)
                        }
                }

            val outgoing = all.filter { it.id in outgoingIds }
            return (outgoing + incoming).distinctBy { it.id }
        }

        override suspend fun neighborhood(
            memoryId: String,
            depth: Int,
        ): List<MemoryEntry> {
            if (depth < 1) return emptyList()
            val visited = linkedSetOf(memoryId)
            var frontier = listOf(memoryId)
            val result = mutableListOf<MemoryEntry>()

            repeat(depth) {
                val next = mutableListOf<MemoryEntry>()
                for (id in frontier) {
                    relatedTo(id).forEach { neighbor ->
                        if (visited.add(neighbor.id)) next += neighbor
                    }
                }
                if (next.isEmpty()) return result
                result += next
                frontier = next.map { it.id }
            }
            return result
        }
    }
