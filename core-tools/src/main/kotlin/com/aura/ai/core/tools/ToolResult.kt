package com.aura.ai.core.tools

/** What a [Tool] hands back after running — [summary] is the human/model-readable outcome
 *  (fed back to the AI provider as a [com.aura.ai.core.ai.MessageRole.Tool] turn once that's
 *  wired up); [data] is structured key/value output a caller can read without re-parsing prose. */
data class ToolResult(
    val summary: String,
    val data: Map<String, String> = emptyMap(),
)
