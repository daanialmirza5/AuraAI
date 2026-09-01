package com.aura.ai.ai.runtime

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The session-aware engine: owns which [ConversationSession]s exist and runs each request through
 * [ConversationPipeline]. Distinct from the pipeline itself so the *algorithm* (stateless,
 * testable on its own) stays separate from *session lifecycle* (which conversation this request
 * belongs to) — the same separation `core-agents.Agent` (stateless logic) and
 * `core-orchestrator`'s per-call `SharedContext` (session-scoped state) already draw one layer
 * down.
 */
@Singleton
class AuraRuntime
    @Inject
    constructor(
        private val pipeline: ConversationPipeline,
    ) {
        private val sessions = ConcurrentHashMap<String, ConversationSession>()

        fun session(sessionId: String): ConversationSession = sessions.getOrPut(sessionId) { ConversationSession(id = sessionId) }

        suspend fun process(
            sessionId: String,
            userMessage: String,
        ): ExecutionResult {
            val session = session(sessionId)
            return pipeline.run(session, userMessage)
        }
    }
