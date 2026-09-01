package com.aura.ai.core.ai

/** A single request to an [AIProvider]. `availableTools` is how core-planner/core-tools advertise
 *  callable capabilities to a model that supports function-calling — a provider that doesn't
 *  (`capabilities.supportsTools == false`) is expected to just ignore it. */
data class GenerationRequest(
    val messages: List<AiMessage>,
    val systemPrompt: String? = null,
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int? = null,
    val availableTools: List<ToolDescriptor> = emptyList(),
)

/** A model's request to invoke a tool — core-tools' `ToolRegistry` resolves [toolName] and
 *  core-actions' `ActionEngine` performs it; the result is fed back as a [MessageRole.Tool] turn. */
data class ToolCallRequest(
    val id: String,
    val toolName: String,
    val arguments: Map<String, String>,
)

enum class FinishReason { Stop, ToolCall, Length, ContentFilter, Error }

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
)

data class GenerationResponse(
    val text: String,
    val toolCalls: List<ToolCallRequest> = emptyList(),
    val finishReason: FinishReason = FinishReason.Stop,
    val usage: TokenUsage? = null,
)

/** One piece of a streamed [GenerationResponse], for [AIProvider.generateStream]. */
data class GenerationChunk(
    val delta: String,
    val isFinal: Boolean = false,
)
