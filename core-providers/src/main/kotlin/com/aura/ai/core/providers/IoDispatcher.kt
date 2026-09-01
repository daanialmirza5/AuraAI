package com.aura.ai.core.providers

import javax.inject.Qualifier

/**
 * Qualifies the [kotlinx.coroutines.CoroutineDispatcher] the four HTTP-backed providers run
 * network calls on. Defined here (not in `:app`) so [ClaudeProvider]/[OpenAIProvider]/
 * [GeminiProvider]/[OllamaProvider] can depend on it directly without depending on the app module
 * — `:app`'s `CoroutineModule` is what actually satisfies it with [kotlinx.coroutines.Dispatchers.IO].
 * An unqualified `CoroutineDispatcher` binding would be ambiguous the moment a second one is ever
 * needed anywhere in the graph, so every dispatcher this codebase injects gets its own qualifier.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
