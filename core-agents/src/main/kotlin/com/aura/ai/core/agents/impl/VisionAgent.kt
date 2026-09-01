package com.aura.ai.core.agents.impl

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentHealth
import com.aura.ai.core.agents.AgentHealthStatus
import com.aura.ai.core.agents.AgentResult
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.SharedContext
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.events.EventBus
import com.aura.ai.core.events.TaskFailedEvent
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.providers.AIProviderManager
import javax.inject.Inject

/**
 * Still non-functional, but for a narrower reason than when this class was first scaffolded
 * (Phase 6). Both original gaps are now closed elsewhere: `AIProvider`s can be genuinely connected
 * (`docs/AI_PROVIDER_INTEGRATION.md`), and `com.aura.ai.core.ai.AiMessage` now carries an optional
 * image (`docs/VISION_RUNTIME.md`). What's still missing is specific to *this* class: `core-agents`
 * is pure Kotlin/JVM and [AgentTask.parameters] is `Map<String, String>` — there is no way for an
 * `Agent` running here to turn a `content://` URI into image bytes without an Android `Context`.
 * `com.aura.ai.ai.ImageUnderstandingTool` (`:app`) is where vision actually landed instead — it
 * has the `Context` this class structurally can't, and reaching it through the `Tool` system
 * (`docs/ANDROID_AUTOMATION.md`) rather than the `Agent`/`Capability.VisionAnalysis` path is why
 * [supportedIntents] staying empty is still correct: nothing should route a vision request here
 * only to have it fail. Making this agent genuinely work would mean either giving `core-agents`
 * an Android-aware image-loading seam or pre-decoding images before a task reaches it — real
 * future work, not a regression from this milestone.
 */
class VisionAgent
    @Inject
    constructor(
        private val providerManager: AIProviderManager,
        private val eventBus: EventBus,
    ) : Agent {
        override val name = "VisionAgent"
        override val description = "Analyzes images. Requires a connected, vision-capable AI provider."
        override val capabilities = setOf(Capability.VisionAnalysis)
        override val requiredPermissions = emptyList<String>()
        override val requiredTools = emptyList<String>()
        override val supportedIntents = emptySet<IntentType>()
        override val priority = 30

        override suspend fun execute(
            task: AgentTask,
            context: SharedContext,
        ): AuraResult<AgentResult> {
            val reason =
                "This agent has no way to load image bytes without an Android Context — " +
                    "use the \"understand_image\"/\"ocr\"/\"scan_barcode\" tools instead, which do."
            eventBus.publish(TaskFailedEvent(taskId = task.id, reason = reason, agentName = name))
            return AuraResult.Failure(AuraError.NotSupported(reason))
        }

        override suspend fun health(): AgentHealth {
            val provider = providerManager.activeProvider.value
            val ready = provider.isAvailable() && provider.capabilities.supportsVision
            return if (ready) {
                AgentHealth(AgentHealthStatus.Healthy, "A vision-capable AI provider is connected.")
            } else {
                AgentHealth(AgentHealthStatus.Unavailable, "No vision-capable AI provider is connected — this agent cannot function yet.")
            }
        }
    }
