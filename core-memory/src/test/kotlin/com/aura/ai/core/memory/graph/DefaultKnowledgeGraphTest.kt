package com.aura.ai.core.memory.graph

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.MemoryCategory
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.MemoryRelationship
import com.aura.ai.core.memory.model.MemoryStatus
import com.aura.ai.core.memory.model.RelationshipType
import com.aura.ai.core.memory.store.LongTermMemoryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeLongTermMemoryStore(
    seed: List<MemoryEntry> = emptyList(),
) : LongTermMemoryStore {
    private val entries = MutableStateFlow(seed.associateBy { it.id })

    override suspend fun remember(entry: MemoryEntry): AuraResult<MemoryEntry> {
        entries.value = entries.value + (entry.id to entry)
        return AuraResult.Success(entry)
    }

    override suspend fun update(entry: MemoryEntry): AuraResult<MemoryEntry> {
        entries.value = entries.value + (entry.id to entry)
        return AuraResult.Success(entry)
    }

    override suspend fun get(id: String): MemoryEntry? = entries.value[id]

    override suspend fun recall(
        query: String,
        limit: Int,
        includeArchived: Boolean,
    ): AuraResult<List<MemoryEntry>> = AuraResult.Success(entries.value.values.take(limit))

    override suspend fun byCategory(
        category: MemoryCategory,
        includeArchived: Boolean,
    ): AuraResult<List<MemoryEntry>> = AuraResult.Success(entries.value.values.filter { it.category == category })

    override suspend fun archive(id: String): AuraResult<Unit> {
        val existing = entries.value[id] ?: return AuraResult.Failure(AuraError.InvalidRequest("missing"))
        entries.value = entries.value + (id to existing.copy(status = MemoryStatus.Archived))
        return AuraResult.Success(Unit)
    }

    override suspend fun delete(id: String): AuraResult<Unit> {
        entries.value = entries.value - id
        return AuraResult.Success(Unit)
    }

    override fun observeAll(includeArchived: Boolean) = entries.map { it.values.toList() }
}

private fun memory(
    category: MemoryCategory,
    content: String,
    relationships: List<MemoryRelationship> = emptyList(),
) = MemoryEntry(content = content, category = category, relationships = relationships)

class DefaultKnowledgeGraphTest {
    @Test
    fun `autoLink attaches a suggested relationship before the memory is ever stored`() =
        runBlocking {
            val project = memory(MemoryCategory.Project, "I am working on RootAI")
            val graph = DefaultKnowledgeGraph(FakeLongTermMemoryStore(listOf(project)))

            val task = memory(MemoryCategory.Task, "Finish the RootAI onboarding flow")
            val linked = graph.autoLink(task)

            assertEquals(1, linked.relationships.size)
            assertEquals(project.id, linked.relationships.single().targetMemoryId)
        }

    @Test
    fun `relatedTo finds an outgoing edge the memory itself declares`() =
        runBlocking {
            val project = memory(MemoryCategory.Project, "RootAI")
            val task =
                memory(MemoryCategory.Task, "A task", relationships = listOf(MemoryRelationship(project.id, RelationshipType.PartOf)))
            val graph = DefaultKnowledgeGraph(FakeLongTermMemoryStore(listOf(project, task)))

            val related = graph.relatedTo(task.id)

            assertEquals(listOf(project.id), related.map { it.id })
        }

    @Test
    fun `relatedTo also finds an incoming edge from a memory it never declared itself`() =
        runBlocking {
            val project = memory(MemoryCategory.Project, "RootAI")
            val task =
                memory(MemoryCategory.Task, "A task", relationships = listOf(MemoryRelationship(project.id, RelationshipType.PartOf)))
            val graph = DefaultKnowledgeGraph(FakeLongTermMemoryStore(listOf(project, task)))

            // The project itself has no outgoing relationships — the edge only exists on the task —
            // but relatedTo(project) must still find the task, searching in both directions.
            val related = graph.relatedTo(project.id)

            assertEquals(listOf(task.id), related.map { it.id })
        }

    @Test
    fun `relatedTo can filter by relationship type`() =
        runBlocking {
            val a = memory(MemoryCategory.Project, "A")
            val b = memory(MemoryCategory.Project, "B")
            val c =
                memory(
                    MemoryCategory.Task,
                    "C",
                    relationships =
                        listOf(
                            MemoryRelationship(a.id, RelationshipType.PartOf),
                            MemoryRelationship(b.id, RelationshipType.Contradicts),
                        ),
                )
            val graph = DefaultKnowledgeGraph(FakeLongTermMemoryStore(listOf(a, b, c)))

            val partOfOnly = graph.relatedTo(c.id, RelationshipType.PartOf)

            assertEquals(listOf(a.id), partOfOnly.map { it.id })
        }

    @Test
    fun `neighborhood expands transitively — depth 1 sees only the direct neighbor, depth 2 sees the chain`() =
        runBlocking {
            val a = memory(MemoryCategory.Project, "A")
            val b = memory(MemoryCategory.Task, "B", relationships = listOf(MemoryRelationship(a.id, RelationshipType.PartOf)))
            val c = memory(MemoryCategory.Task, "C", relationships = listOf(MemoryRelationship(b.id, RelationshipType.PartOf)))
            val graph = DefaultKnowledgeGraph(FakeLongTermMemoryStore(listOf(a, b, c)))

            val depthOne = graph.neighborhood(a.id, depth = 1)
            assertEquals(setOf(b.id), depthOne.map { it.id }.toSet())

            val depthTwo = graph.neighborhood(a.id, depth = 2)
            assertEquals(setOf(b.id, c.id), depthTwo.map { it.id }.toSet())
        }

    @Test
    fun `neighborhood terminates on a direct cycle instead of looping forever`() =
        runBlocking {
            val a = memory(MemoryCategory.Project, "A")
            val b = memory(MemoryCategory.Task, "B")
            val aWithEdge = a.copy(relationships = listOf(MemoryRelationship(b.id, RelationshipType.RelatesTo)))
            val bWithEdge = b.copy(relationships = listOf(MemoryRelationship(a.id, RelationshipType.RelatesTo)))
            val graph = DefaultKnowledgeGraph(FakeLongTermMemoryStore(listOf(aWithEdge, bWithEdge)))

            val result = graph.neighborhood(a.id, depth = 10)

            assertEquals(listOf(b.id), result.map { it.id })
        }
}
