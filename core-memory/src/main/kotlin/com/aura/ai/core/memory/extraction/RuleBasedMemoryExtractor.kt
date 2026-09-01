package com.aura.ai.core.memory.extraction

import com.aura.ai.core.memory.classification.MemoryClassifier
import com.aura.ai.core.memory.model.MemoryCategory
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.MemorySource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The default [MemoryExtractor]. Splits an utterance into clauses (sentence/conjunction
 * boundaries), classifies each independently, and skips any clause the classifier isn't at
 * least somewhat confident is memory-worthy — a low-signal fragment like "well" or "okay so"
 * shouldn't become a permanent memory just because it technically fell through to
 * [MemoryCategory.General].
 */
@Singleton
class RuleBasedMemoryExtractor
    @Inject
    constructor(
        private val classifier: MemoryClassifier,
    ) : MemoryExtractor {
        private val clauseBoundary = Regex("""(?i)[.!?]+\s+|,?\s+and\s+""")

        override fun extract(
            text: String,
            source: MemorySource,
        ): List<MemoryExtractionResult> = splitIntoClauses(text).mapNotNull { clause -> extractFromClause(clause, source) }

        private fun splitIntoClauses(text: String): List<String> = text.split(clauseBoundary).map { it.trim() }.filter { it.isNotEmpty() }

        private fun extractFromClause(
            clause: String,
            source: MemorySource,
        ): MemoryExtractionResult? {
            val classification = classifier.classify(clause)
            if (classification.category == MemoryCategory.General && classification.confidence < 0.5f) {
                return null
            }

            val memory =
                MemoryEntry(
                    content = clause,
                    category = classification.category,
                    importance = defaultImportance(classification.category),
                    confidence = classification.confidence,
                    tags = classification.suggestedTags,
                    source = source,
                )

            if (classification.category != MemoryCategory.Goal) {
                return MemoryExtractionResult(memory = memory)
            }

            val dateMillis = DateExpressionParser.parseEpochMillis(clause) ?: return MemoryExtractionResult(memory = memory)
            return MemoryExtractionResult(
                memory = memory.copy(tags = memory.tags + "calendar"),
                suggestedActionToolName = "create_calendar_event",
                suggestedActionParameters = mapOf("title" to clause, "dateMillis" to dateMillis.toString()),
            )
        }

        private fun defaultImportance(category: MemoryCategory): Float =
            when (category) {
                MemoryCategory.Goal, MemoryCategory.Project -> 0.75f
                MemoryCategory.Person -> 0.7f
                MemoryCategory.Assignment -> 0.65f
                MemoryCategory.Preference -> 0.6f
                MemoryCategory.Task, MemoryCategory.Meeting -> 0.55f
                MemoryCategory.Knowledge, MemoryCategory.Document -> 0.5f
                MemoryCategory.Research -> 0.45f
                MemoryCategory.Episodic, MemoryCategory.File -> 0.35f
                MemoryCategory.General -> 0.3f
            }
    }
