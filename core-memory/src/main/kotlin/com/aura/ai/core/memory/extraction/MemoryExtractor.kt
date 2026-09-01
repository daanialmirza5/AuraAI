package com.aura.ai.core.memory.extraction

import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.MemorySource
import com.aura.ai.core.memory.model.MemorySourceType

/**
 * What [MemoryExtractor.extract] hands back for one extracted clause: the [memory] itself, plus
 * an optional bridge into core-actions' tool vocabulary when the memory implies a real-world
 * action worth taking — e.g. a dated [com.aura.ai.core.memory.model.MemoryCategory.Goal]
 * suggesting `create_calendar_event`. Deliberately just a tool *name* and *parameters* (plain
 * strings), never an actual core-actions/core-tools type — core-memory has no dependency on
 * either, and extraction only ever *observes and suggests*; something else (a future
 * conversation orchestrator) decides whether to actually run it.
 */
data class MemoryExtractionResult(
    val memory: MemoryEntry,
    val suggestedActionToolName: String? = null,
    val suggestedActionParameters: Map<String, String> = emptyMap(),
)

/**
 * Turns raw user text into zero or more [MemoryExtractionResult]s. `RuleBasedMemoryExtractor` is
 * the only implementation this phase ships, built on top of [com.aura.ai.core.memory.classification.MemoryClassifier].
 */
interface MemoryExtractor {
    fun extract(
        text: String,
        source: MemorySource = MemorySource(MemorySourceType.Conversation),
    ): List<MemoryExtractionResult>
}
