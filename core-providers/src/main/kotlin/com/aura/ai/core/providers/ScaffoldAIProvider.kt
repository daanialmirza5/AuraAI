package com.aura.ai.core.providers

import com.aura.ai.core.ai.AIProvider
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.GenerationChunk
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.GenerationResponse
import com.aura.ai.core.ai.ProviderCapabilities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Shared behavior for every provider this phase ships: correctly describes what it *would*
 * support once connected (queried today by [AIProviderManager]/core-planner for routing
 * decisions), but every call that would need a network client honestly fails with
 * [AuraError.ProviderNotConnected] instead of silently returning fake data. This is the one
 * seam a future phase touches to go live — implement [generate]/[generateStream]/[isAvailable]
 * for real in a subclass (or replace it outright); nothing above `AIProvider` changes.
 */
abstract class ScaffoldAIProvider(
    final override val id: String,
    final override val displayName: String,
    final override val capabilities: ProviderCapabilities,
) : AIProvider {
    override suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse> =
        AuraResult.Failure(
            AuraError.ProviderNotConnected(
                providerId = id,
                message = "$displayName is scaffolded but not yet connected — see core-providers/README.md.",
            ),
        )

    override fun generateStream(request: GenerationRequest): Flow<AuraResult<GenerationChunk>> =
        flowOf(
            AuraResult.Failure(
                AuraError.ProviderNotConnected(
                    providerId = id,
                    message = "$displayName is scaffolded but not yet connected — see core-providers/README.md.",
                ),
            ),
        )

    override suspend fun isAvailable(): Boolean = false
}
