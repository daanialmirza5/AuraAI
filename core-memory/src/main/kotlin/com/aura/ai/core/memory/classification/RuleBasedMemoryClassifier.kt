package com.aura.ai.core.memory.classification

import com.aura.ai.core.ai.ScoredKeywordMatcher
import com.aura.ai.core.memory.model.MemoryCategory
import javax.inject.Inject
import javax.inject.Singleton

private data class CategoryRule(
    val category: MemoryCategory,
    val triggers: List<String>,
    val tag: String,
)

private val WEEKDAYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

/**
 * The default, local, offline [MemoryClassifier]. Rules are ordered most-specific-first and
 * scored (not just first-match) for the same reason `KeywordIntentRecognizer` is: a couple of
 * phrases genuinely overlap ("I need to finish my project" touches both Task and Project), and
 * counting hits resolves ties more sensibly than an arbitrary rule order would.
 */
@Singleton
class RuleBasedMemoryClassifier
    @Inject
    constructor() : MemoryClassifier {
        private val rules =
            listOf(
                CategoryRule(
                    MemoryCategory.Preference,
                    listOf("i prefer", "i like", "i love", "i hate", "i dislike", "i enjoy", "my favorite", "my birthday"),
                    tag = "preference",
                ),
                CategoryRule(
                    MemoryCategory.Project,
                    listOf("i am working on", "i'm working on", "working on", "my project", "i am building", "i'm building"),
                    tag = "project",
                ),
                CategoryRule(
                    MemoryCategory.Goal,
                    listOf("deadline", "due", "my exam", "my goal", "i plan to", "i'm planning to", "next week") + WEEKDAYS,
                    tag = "goal",
                ),
                CategoryRule(
                    MemoryCategory.Task,
                    listOf("i have to", "i need to", "todo", "remind me to", "don't forget to", "do not forget to"),
                    tag = "task",
                ),
                CategoryRule(
                    MemoryCategory.Knowledge,
                    listOf("remember that", "fyi", "just so you know", "the fact is", "for reference"),
                    tag = "knowledge",
                ),
                CategoryRule(
                    MemoryCategory.Episodic,
                    listOf("yesterday i", "earlier today", "i just", "we just", "last night i"),
                    tag = "episodic",
                ),
                CategoryRule(
                    MemoryCategory.Person,
                    listOf(
                        "is my manager",
                        "is my boss",
                        "is my colleague",
                        "is my friend",
                        "is my teammate",
                        "works with me",
                        "reports to me",
                        "i report to",
                    ),
                    tag = "person",
                ),
                CategoryRule(
                    MemoryCategory.Meeting,
                    listOf("meeting with", "call with", "sync with", "meeting at", "scheduled a meeting", "1:1 with", "catch up with"),
                    tag = "meeting",
                ),
                CategoryRule(
                    MemoryCategory.Assignment,
                    listOf("assignment", "homework", "assigned to me", "was assigned", "due for class", "due for the course"),
                    tag = "assignment",
                ),
                CategoryRule(
                    MemoryCategory.Research,
                    listOf("researching", "looking into", "investigating", "reading about", "reading up on"),
                    tag = "research",
                ),
                CategoryRule(
                    MemoryCategory.Document,
                    listOf("wrote a doc", "the document", "shared a document", "drafted a report", "the report", "my notes on"),
                    tag = "document",
                ),
                CategoryRule(
                    MemoryCategory.File,
                    listOf("saved a file", "the file", "attached", "screenshot of", "uploaded", "downloaded"),
                    tag = "file",
                ),
            )

        override fun classify(text: String): MemoryClassification {
            val normalized = text.trim().lowercase()
            if (normalized.isEmpty()) {
                return MemoryClassification(MemoryCategory.General, confidence = 0f)
            }

            val scored = ScoredKeywordMatcher.match(text, rules.map { it to it.triggers })

            val winner =
                scored.firstOrNull()
                    ?: return MemoryClassification(MemoryCategory.General, confidence = 0.3f)

            return MemoryClassification(
                category = winner.value.category,
                confidence = ScoredKeywordMatcher.confidence(winner.hits, base = 0.55f),
                suggestedTags = listOf(winner.value.tag),
            )
        }
    }
