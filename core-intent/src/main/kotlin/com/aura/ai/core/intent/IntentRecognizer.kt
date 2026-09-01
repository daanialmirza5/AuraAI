package com.aura.ai.core.intent

import com.aura.ai.core.ai.AuraResult

/**
 * Classifies a raw utterance into a [RecognizedIntent]. [KeywordIntentRecognizer] is the only
 * implementation this phase ships — a local, offline, zero-latency heuristic. It is deliberately
 * not a placeholder: it's the real, permanent *fast path* production AURA will keep even after a
 * cloud provider is connected (cheap local triage before ever spending a model call), not a stand-in
 * that gets deleted later. A future `LlmIntentRecognizer` backed by `AIProviderManager` can be
 * swapped in behind this same interface for the cases the keyword pass is unsure about.
 */
interface IntentRecognizer {
    suspend fun recognize(utterance: String): AuraResult<RecognizedIntent>
}
