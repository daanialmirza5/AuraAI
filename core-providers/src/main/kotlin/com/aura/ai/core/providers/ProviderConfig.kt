package com.aura.ai.core.providers

/**
 * The shape of a provider's configuration — not its content. No real API key ever lives in this
 * codebase; when a provider is actually connected (a later phase), a value here would come from
 * encrypted storage supplied by the app at DI time, never a literal in source.
 */
data class ProviderConfig(
    val id: ProviderId,
    val modelName: String,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val extra: Map<String, String> = emptyMap(),
)
