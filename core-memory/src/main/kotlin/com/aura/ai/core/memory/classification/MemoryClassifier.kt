package com.aura.ai.core.memory.classification

import com.aura.ai.core.memory.model.MemoryCategory

data class MemoryClassification(
    val category: MemoryCategory,
    /** 0f–1f. A heuristic ("how many rules fired"), not a calibrated probability — see
     *  `RuleBasedMemoryClassifier`. Feeds directly into `MemoryEntry.confidence` at extraction time. */
    val confidence: Float,
    val suggestedTags: List<String> = emptyList(),
)

/**
 * Decides *what kind* of memory a piece of text is. `RuleBasedMemoryClassifier` is the only
 * implementation this phase ships — local, offline, keyword/pattern-based, the same fast-path
 * philosophy as core-intent's `KeywordIntentRecognizer`. A future LLM-backed classifier slots in
 * behind this same interface for the statements the rules are genuinely unsure about.
 */
interface MemoryClassifier {
    fun classify(text: String): MemoryClassification
}
