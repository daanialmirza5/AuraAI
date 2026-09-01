package com.aura.ai.core.memory

import com.aura.ai.core.ai.AiMessage
import com.aura.ai.core.ai.MessageRole
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps only the most recent [maxTurns] non-system turns — a system prompt (if present) is
 *  always kept regardless of age, since trimming it would silently change the model's behavior
 *  rather than just its short-term recall. */
@Singleton
class SlidingWindowConversationBuffer
    @Inject
    constructor() : ConversationBuffer {
        companion object {
            private const val DEFAULT_MAX_TURNS = 20
        }

        private val turns = CopyOnWriteArrayList<AiMessage>()

        override fun append(turn: AiMessage) {
            turns.add(turn)
            val nonSystem = turns.filter { it.role != MessageRole.System }
            if (nonSystem.size > DEFAULT_MAX_TURNS) {
                val overflow = nonSystem.size - DEFAULT_MAX_TURNS
                val toDrop = nonSystem.take(overflow).toSet()
                turns.removeAll(toDrop)
            }
        }

        override fun current(): List<AiMessage> = turns.toList()

        override fun clear() {
            turns.removeAll(turns.filter { it.role != MessageRole.System }.toSet())
        }
    }
