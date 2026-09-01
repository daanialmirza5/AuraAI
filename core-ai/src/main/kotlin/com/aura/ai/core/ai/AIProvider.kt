package com.aura.ai.core.ai

import kotlinx.coroutines.flow.Flow

/**
 * The one contract every model backend implements — Gemini, OpenAI, Claude, Ollama, and a future
 * on-device LocalModel are all just an `AIProvider` to everything above core-providers. Nothing in
 * core-intent, core-planner, core-actions, or the app's business logic is ever allowed to depend
 * on a concrete provider type; they depend on this interface (or, in practice, on
 * `AIProviderManager`, which holds the currently-active one). That is what makes switching
 * providers a configuration change rather than a code change.
 *
 * No implementation of this interface makes a network call yet — see core-providers' README.
 */
interface AIProvider {
    /** A stable machine identifier, e.g. "gemini", "openai", "claude", "ollama", "local". */
    val id: String

    /** A human-readable name for settings UI, e.g. "Google Gemini". */
    val displayName: String

    val capabilities: ProviderCapabilities

    /** A single non-streamed completion. */
    suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse>

    /** A streamed completion, one [GenerationChunk] per emission. Providers that don't support
     *  streaming (see [ProviderCapabilities.supportsStreaming]) may implement this by emitting
     *  the whole [generate] result as a single final chunk. */
    fun generateStream(request: GenerationRequest): Flow<AuraResult<GenerationChunk>>

    /** Whether this provider is currently usable (configured, reachable, credentials present).
     *  [AIProviderManager] uses this to skip unavailable providers rather than fail at call time. */
    suspend fun isAvailable(): Boolean
}
