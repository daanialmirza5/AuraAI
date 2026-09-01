package com.aura.ai.core.ai

/** Mirrors the role taxonomy every major chat-completion API (OpenAI/Gemini/Claude/Ollama) shares. */
enum class MessageRole { System, User, Assistant, Tool }

/**
 * A single turn in a conversation, provider-agnostic. This is distinct from the app's own
 * `domain.model.ChatMessage` (which is a persisted, UI-facing chat bubble) — this type is the
 * wire-level unit the AI core passes to [AIProvider.generate]; the app layer maps between them.
 */
data class AiMessage(
    val role: MessageRole,
    val content: String,
    /** The tool name this message is reporting the result of, when [role] is [MessageRole.Tool]. */
    val name: String? = null,
    val toolCallId: String? = null,
    /** Base64-encoded image bytes, for a vision-capable provider (`capabilities.supportsVision`).
     *  `null` for every message before Vision Runtime (Version 1.0 Critical item 4) and for every
     *  provider that doesn't support vision (`OllamaProvider`, `LocalModelProvider`) — fully
     *  additive, no existing caller changes. See `docs/VISION_RUNTIME.md`. */
    val imageBase64: String? = null,
    val imageMimeType: String? = null,
)
