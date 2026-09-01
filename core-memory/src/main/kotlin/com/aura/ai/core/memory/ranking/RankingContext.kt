package com.aura.ai.core.memory.ranking

import com.aura.ai.core.ai.AiMessage

/** Everything a [MemoryRanker] needs beyond the candidate memories themselves — the 4 of the
 *  brief's 6 ranking factors that aren't intrinsic to a memory (Recency and Importance are;
 *  Semantic Similarity needs [query], Conversation Context needs [conversationContext], User
 *  Goals need [activeGoalIds], Current Task needs [currentTaskId]). */
data class RankingContext(
    val query: String? = null,
    val conversationContext: List<AiMessage> = emptyList(),
    val activeGoalIds: Set<String> = emptySet(),
    val currentTaskId: String? = null,
)
