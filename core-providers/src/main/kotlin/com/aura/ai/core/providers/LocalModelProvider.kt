package com.aura.ai.core.providers

import com.aura.ai.core.ai.ProviderCapabilities
import javax.inject.Inject
import javax.inject.Singleton

/** Scaffolded for a model bundled with or downloaded into the app itself (e.g. via MediaPipe
 *  LLM Inference or ONNX Runtime), running entirely on-device with no server at all — the only
 *  provider that would work with zero connectivity. Small context window and no tool-calling
 *  are the honest defaults for what a phone-sized on-device model can do today. */
@Singleton
class LocalModelProvider
    @Inject
    constructor() : ScaffoldAIProvider(
            id = ProviderId.LocalModel.key,
            displayName = "On-device model",
            capabilities =
                ProviderCapabilities(
                    supportsStreaming = true,
                    supportsTools = false,
                    supportsVision = false,
                    isLocal = true,
                    maxContextTokens = 4_096,
                ),
        ) {
        val config = ProviderConfig(id = ProviderId.LocalModel, modelName = "gemma-2b-it")
    }
