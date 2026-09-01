package com.aura.ai.ai.runtime

/** What one [ConversationPipeline] run hands back — the reply the UI shows, and everything that
 *  produced it, kept separate on purpose: [responseText] is the only field the normal chat UI
 *  ever reads; [trace] is entirely for developer mode (see `docs/EXECUTION_TRACE.md`). */
data class ExecutionResult(
    val responseText: String,
    val trace: ExecutionTrace,
)
