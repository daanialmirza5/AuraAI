package com.aura.ai.core.ai

/** What a given [AIProvider] can do — queried by core-planner/core-providers to decide routing
 *  (e.g. don't offer tool-calling to a provider that can't do it; prefer an [isLocal] provider
 *  when privacy or offline behavior matters) without any caller needing to know which concrete
 *  provider it's talking to. */
data class ProviderCapabilities(
    val supportsStreaming: Boolean = false,
    val supportsTools: Boolean = false,
    val supportsVision: Boolean = false,
    /** True for on-device/self-hosted providers (Ollama, LocalModel) — false for cloud APIs. */
    val isLocal: Boolean = false,
    val maxContextTokens: Int = 0,
)
