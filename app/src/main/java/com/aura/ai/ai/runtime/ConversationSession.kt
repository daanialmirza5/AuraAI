package com.aura.ai.ai.runtime

import java.util.UUID

/**
 * One ongoing conversation with AURA — the "Conversation Session" stage of the runtime pipeline.
 * Deliberately minimal today (this app has exactly one continuous conversation, not
 * multiple parallel ones), but a real, distinct type rather than a bare string id: the natural
 * place to hang session-scoped state a future turn might want (active goal ids, a current task
 * id — the same `activeGoalIds`/`currentTaskId` shape `core-memory.ranking.RankingContext`
 * already carries) without changing every call site that only cares about the id.
 */
data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    val startedAtMillis: Long = System.currentTimeMillis(),
)
