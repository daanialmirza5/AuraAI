package com.aura.ai.core.memory.context

import com.aura.ai.core.ai.AiMessage
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.ScoredMemory

data class ContextRequest(
    val query: String,
    val maxMemories: Int = 10,
    /** The practical proxy for a token budget until a real tokenizer is wired — roughly 4
     *  characters per token for English text is the commonly-cited rule of thumb, deliberately
     *  named "characters" rather than "tokens" so the API never claims precision it doesn't have. */
    val maxContextCharacters: Int = 8_000,
    val conversationContext: List<AiMessage> = emptyList(),
    val activeGoalIds: Set<String> = emptySet(),
    val currentTaskId: String? = null,
)

data class BuiltContext(
    val memories: List<ScoredMemory>,
    val conversationTurns: List<AiMessage>,
    val estimatedCharacterCount: Int,
    /** True if candidates existed that didn't make it in — because of [ContextRequest.maxMemories],
     *  [ContextRequest.maxContextCharacters], or both. */
    val truncated: Boolean,
)

/**
 * "Before every AI request: retrieve only relevant memories, construct optimized context, limit
 * context size, avoid duplicate memories" — this interface is exactly that sentence.
 * `DefaultContextBuilder` retrieves via `MemoryRetriever`, ranks via `MemoryRanker`, deduplicates
 * near-identical memories, and truncates to both [ContextRequest.maxMemories] and
 * [ContextRequest.maxContextCharacters] before anything reaches a provider request.
 */
interface ContextBuilder {
    suspend fun build(request: ContextRequest): AuraResult<BuiltContext>
}
