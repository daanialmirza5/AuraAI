package com.aura.ai.core.plugin.intent

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.intent.IntentRecognizer
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.intent.KeywordIntentRecognizer
import com.aura.ai.core.intent.RecognizedIntent
import com.aura.ai.core.plugin.registrar.PluginIntentRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Dynamic Intent Registration," made real. `core-intent.IntentType` is closed — this recognizer
 * doesn't extend it, it *decorates* [KeywordIntentRecognizer]: the built-in recognizer always
 * gets first attempt, and only when it falls all the way through to its own honest
 * `IntentType.Conversation` fallback (confidence ≤ 0.3 — "I genuinely don't know") does this class
 * check whether any plugin's [com.aura.ai.plugin.api.PluginIntentDescriptor.triggerPhrases]
 * matches instead. A match is reported as `IntentType.Automation` — the closest honest fit for
 * "a plugin, not a built-in capability, handles this" — with `RecognizedIntent.slots` carrying
 * which plugin and which of its intents matched, so a caller can route accordingly.
 *
 * This ordering is deliberate: a plugin can add new recognized phrases, but can never *shadow* a
 * phrase the host already understands.
 */
@Singleton
class PluginAwareIntentRecognizer
    @Inject
    constructor(
        private val delegate: KeywordIntentRecognizer,
        private val pluginIntentRegistry: PluginIntentRegistry,
    ) : IntentRecognizer {
        private companion object {
            const val FALLBACK_CONFIDENCE_CEILING = 0.3f
        }

        override suspend fun recognize(utterance: String): AuraResult<RecognizedIntent> {
            val delegateResult = delegate.recognize(utterance)
            val delegateIntent = (delegateResult as? AuraResult.Success)?.value
            val delegateFoundNothing =
                delegateIntent != null &&
                    delegateIntent.type == IntentType.Conversation &&
                    delegateIntent.confidence <= FALLBACK_CONFIDENCE_CEILING

            if (!delegateFoundNothing) return delegateResult

            val normalized = utterance.trim().lowercase()
            if (normalized.isEmpty()) return delegateResult

            for ((pluginId, descriptor) in pluginIntentRegistry.all()) {
                val hits = descriptor.triggerPhrases.count { normalized.contains(it.lowercase()) }
                if (hits == 0) continue

                val confidence = (0.5f + hits * 0.15f).coerceAtMost(0.95f)
                val slots =
                    buildMap {
                        put("pluginId", pluginId)
                        put("pluginIntentId", descriptor.id)
                        descriptor.impliedToolName?.let { put("pluginToolName", it) }
                    }
                return AuraResult.Success(
                    RecognizedIntent(type = IntentType.Automation, confidence = confidence, slots = slots, rawText = utterance),
                )
            }

            return delegateResult
        }
    }
