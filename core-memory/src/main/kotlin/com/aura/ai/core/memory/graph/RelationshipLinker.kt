package com.aura.ai.core.memory.graph

import com.aura.ai.core.memory.model.MemoryCategory
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.MemoryRelationship
import com.aura.ai.core.memory.model.RelationshipType

/**
 * Suggests [MemoryRelationship]s for a newly-extracted memory against the memories that already
 * exist — honestly heuristic word-overlap matching, not real entity resolution/coreference (which
 * would need far more than this codebase's offline, on-device classifier can do). Two rules:
 *
 * 1. A structural memory (a [MemoryCategory.Task]/[MemoryCategory.Assignment]/
 *    [MemoryCategory.Meeting]/[MemoryCategory.Document]/[MemoryCategory.Research]/
 *    [MemoryCategory.File]) that shares a significant word with an existing [MemoryCategory.Project]
 *    links to it as [RelationshipType.PartOf] — "my assignment for RootAI" links to the existing
 *    "I am working on RootAI" memory.
 * 2. Any memory that mentions a name matching an existing [MemoryCategory.Person] memory links to
 *    it as [RelationshipType.RelatesTo] — "meeting with Sarah" links to "Sarah is my manager."
 *
 * Pure function — no store, no coroutines — so it's directly unit-testable; see
 * `RelationshipLinkerTest`. [com.aura.ai.core.memory.graph.DefaultKnowledgeGraph] is what actually
 * fetches the candidate list and applies this.
 */
object RelationshipLinker {
    private val STRUCTURAL_CATEGORIES =
        setOf(
            MemoryCategory.Task,
            MemoryCategory.Assignment,
            MemoryCategory.Meeting,
            MemoryCategory.Document,
            MemoryCategory.Research,
            MemoryCategory.File,
        )

    private val STOPWORDS =
        setOf(
            "the",
            "and",
            "for",
            "with",
            "that",
            "this",
            "have",
            "from",
            "about",
            "into",
            "your",
            "just",
            "what",
            "when",
            "where",
            "there",
            "will",
            "would",
            "should",
            "could",
            "been",
        )

    fun suggestRelationships(
        newMemory: MemoryEntry,
        existing: List<MemoryEntry>,
    ): List<MemoryRelationship> {
        val newWords = significantWords(newMemory.content)
        if (newWords.isEmpty()) return emptyList()

        val relationships = mutableListOf<MemoryRelationship>()

        if (newMemory.category in STRUCTURAL_CATEGORIES) {
            existing
                .filter { it.category == MemoryCategory.Project }
                .firstOrNull { candidate -> significantWords(candidate.content).any { it in newWords } }
                ?.let { relationships += MemoryRelationship(it.id, RelationshipType.PartOf) }
        }

        if (newMemory.category != MemoryCategory.Person) {
            existing
                .filter { it.category == MemoryCategory.Person && it.id != newMemory.id }
                .filter { person -> significantWords(person.content).any { it in newWords } }
                .forEach { relationships += MemoryRelationship(it.id, RelationshipType.RelatesTo) }
        }

        return relationships.distinctBy { it.targetMemoryId to it.type }
    }

    private fun significantWords(text: String): Set<String> =
        text
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.length >= 4 && it !in STOPWORDS }
            .toSet()
}
