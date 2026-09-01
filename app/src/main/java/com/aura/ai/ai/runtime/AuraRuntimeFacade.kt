package com.aura.ai.ai.runtime

import com.aura.ai.domain.model.MessageSender
import com.aura.ai.domain.repository.ChatRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one thing `AuraTabViewModel` actually depends on for sending a message — every bit of what
 * `sendChat()` used to do inline (append the user's message, wait, append a canned reply) now
 * lives here instead, which is what makes "AuraTabViewModel must no longer contain business
 * logic" true rather than aspirational. Bridges [AuraRuntime] (the platform) to
 * `com.aura.ai.domain.repository.ChatRepository` (the app's existing, Room-backed chat history) —
 * the composition-root-level integration work this whole phase is about, kept in exactly one place.
 *
 * This app has one continuous conversation, not several, so [DEFAULT_SESSION_ID] is a fixed
 * constant rather than something the ViewModel has to manage — one fewer thing for the
 * presentation layer to know about.
 */
@Singleton
class AuraRuntimeFacade
    @Inject
    constructor(
        private val auraRuntime: AuraRuntime,
        private val chatRepository: ChatRepository,
    ) {
        private companion object {
            const val DEFAULT_SESSION_ID = "default"
        }

        /** Keyed by the assistant [com.aura.ai.domain.model.ChatMessage.id] that resulted from a
         *  turn — developer mode looks a trace up by tapping that specific message, never sees one
         *  for a message it didn't ask about. In-memory only, the same "ephemeral debugging aid, not
         *  required to survive process death" choice every other trace-shaped type in this codebase
         *  makes; see `docs/EXECUTION_TRACE.md`. */
        private val traces = ConcurrentHashMap<Long, ExecutionTrace>()

        suspend fun sendMessage(text: String): ExecutionResult {
            chatRepository.appendMessage(MessageSender.User, text)
            val result = auraRuntime.process(DEFAULT_SESSION_ID, text)
            val assistantMessage = chatRepository.appendMessage(MessageSender.Ai, result.responseText)
            traces[assistantMessage.id] = result.trace
            return result
        }

        fun traceFor(messageId: Long): ExecutionTrace? = traces[messageId]
    }
