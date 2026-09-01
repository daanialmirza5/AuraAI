package com.aura.ai.core.providers

import com.aura.ai.core.ai.AIProvider
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.GenerationChunk
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.GenerationResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The single point of contact between "business logic that wants a completion" and "whichever
 * model backend is currently selected." core-planner, core-intent, and (once written) the app's
 * own ViewModels talk to this — never to a concrete [AIProvider] — which is what makes
 * [selectProvider] a pure configuration change: nothing that calls [generate] needs to change
 * when the user switches from Gemini to a local Ollama model.
 */
interface AIProviderManager {
    val activeProvider: StateFlow<AIProvider>

    fun availableProviders(): List<AIProvider>

    fun selectProvider(id: ProviderId): AuraResult<Unit>

    suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse>

    fun generateStream(request: GenerationRequest): Flow<AuraResult<GenerationChunk>>
}
