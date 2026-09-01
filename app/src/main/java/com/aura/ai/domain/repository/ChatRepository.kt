package com.aura.ai.domain.repository

import com.aura.ai.domain.model.ChatMessage
import com.aura.ai.domain.model.MessageSender
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(): Flow<List<ChatMessage>>

    /** Returns the persisted message (with its real, DB-assigned [ChatMessage.id]) — callers that
     *  need to correlate a follow-up (e.g. a debug trace) with the exact message it belongs to
     *  need this back, not just confirmation the insert happened. */
    suspend fun appendMessage(
        sender: MessageSender,
        text: String,
    ): ChatMessage
}
