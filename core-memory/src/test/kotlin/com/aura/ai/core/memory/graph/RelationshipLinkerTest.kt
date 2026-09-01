package com.aura.ai.core.memory.graph

import com.aura.ai.core.memory.model.MemoryCategory
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.RelationshipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun memory(
    category: MemoryCategory,
    content: String,
) = MemoryEntry(content = content, category = category)

class RelationshipLinkerTest {
    @Test
    fun `links a task to a project that shares a significant word`() {
        val project = memory(MemoryCategory.Project, "I am working on RootAI")
        val task = memory(MemoryCategory.Task, "Finish the RootAI onboarding flow")

        val suggested = RelationshipLinker.suggestRelationships(task, listOf(project))

        assertEquals(1, suggested.size)
        assertEquals(project.id, suggested.single().targetMemoryId)
        assertEquals(RelationshipType.PartOf, suggested.single().type)
    }

    @Test
    fun `links any memory mentioning a person to that person's memory`() {
        val person = memory(MemoryCategory.Person, "Sarah is my manager")
        val meeting = memory(MemoryCategory.Meeting, "Meeting with Sarah about the roadmap")

        val suggested = RelationshipLinker.suggestRelationships(meeting, listOf(person))

        assertEquals(1, suggested.size)
        assertEquals(person.id, suggested.single().targetMemoryId)
        assertEquals(RelationshipType.RelatesTo, suggested.single().type)
    }

    @Test
    fun `a memory can link to both a project and a person at once`() {
        val project = memory(MemoryCategory.Project, "I am working on RootAI")
        val person = memory(MemoryCategory.Person, "Sarah is my manager")
        val meeting = memory(MemoryCategory.Meeting, "Meeting with Sarah about RootAI")

        val suggested = RelationshipLinker.suggestRelationships(meeting, listOf(project, person))

        assertEquals(2, suggested.size)
        assertTrue(suggested.any { it.targetMemoryId == project.id && it.type == RelationshipType.PartOf })
        assertTrue(suggested.any { it.targetMemoryId == person.id && it.type == RelationshipType.RelatesTo })
    }

    @Test
    fun `no overlap means no suggestions, not a guess`() {
        val project = memory(MemoryCategory.Project, "I am working on RootAI")
        val task = memory(MemoryCategory.Task, "Buy groceries later")

        assertTrue(RelationshipLinker.suggestRelationships(task, listOf(project)).isEmpty())
    }

    @Test
    fun `a Person memory never links to another Person as PartOf`() {
        val personA = memory(MemoryCategory.Person, "Sarah is my manager")
        val personB = memory(MemoryCategory.Person, "Sarah recommended a book")

        // Structural (PartOf) linking only applies to Task/Assignment/Meeting/Document/Research/File —
        // a Person memory should never itself become "PartOf" another Person.
        val suggested = RelationshipLinker.suggestRelationships(personB, listOf(personA))

        assertTrue(suggested.none { it.type == RelationshipType.PartOf })
    }

    @Test
    fun `unrelated content with no shared significant words never triggers a link`() {
        val project = memory(MemoryCategory.Project, "I am working on the app")
        val task = memory(MemoryCategory.Task, "I need to call the bank")

        assertTrue(RelationshipLinker.suggestRelationships(task, listOf(project)).isEmpty())
    }
}
