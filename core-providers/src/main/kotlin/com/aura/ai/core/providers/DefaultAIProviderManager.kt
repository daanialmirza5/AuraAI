package com.aura.ai.core.providers

import com.aura.ai.core.ai.AIProvider
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.GenerationChunk
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.GenerationResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [providers] is a Hilt `@IntoSet` multibinding — see the app module's `AiProvidersModule` for
 * where the 5 concrete providers are actually bound. Adding a 6th provider is exactly one more
 * `@Binds @IntoSet` line there; nothing here or above needs to change.
 */
@Singleton
class DefaultAIProviderManager
    @Inject
    constructor(
        private val providers: Set<@JvmSuppressWildcards AIProvider>,
    ) : AIProviderManager {
        init {
            require(providers.isNotEmpty()) { "AIProviderManager requires at least one AIProvider to be bound." }
        }

        private val byId: Map<String, AIProvider> = providers.associateBy { it.id }

        private val _activeProvider =
            MutableStateFlow(
                byId[ProviderId.Gemini.key] ?: providers.first(),
            )
        override val activeProvider: StateFlow<AIProvider> = _activeProvider.asStateFlow()

        override fun availableProviders(): List<AIProvider> = providers.toList()

        override fun selectProvider(id: ProviderId): AuraResult<Unit> {
            val provider =
                byId[id.key]
                    ?: return AuraResult.Failure(AuraError.NotSupported("No AIProvider is registered for '${id.key}'."))
            _activeProvider.value = provider
            return AuraResult.Success(Unit)
        }

        override suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse> = activeProvider.value.generate(request)

        override fun generateStream(request: GenerationRequest): Flow<AuraResult<GenerationChunk>> =
            activeProvider.value.generateStream(request)
    }
