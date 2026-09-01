package com.aura.ai.core.intent

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ScoredKeywordMatcher
import javax.inject.Inject
import javax.inject.Singleton

/** One rule set for a single [IntentType]: phrases that count as evidence, plus an optional
 *  slot extractor run against the matched utterance when this type wins. */
private data class IntentRule(
    val type: IntentType,
    val triggers: List<String>,
    val extractSlots: (String) -> Map<String, String> = { emptyMap() },
)

/**
 * The default, local, offline [IntentRecognizer] — plain keyword/phrase matching, no network,
 * no model. Every rule set below is intentionally small and readable rather than exhaustive;
 * this is a fast triage layer, not a substitute for a real NLU model.
 */
@Singleton
class KeywordIntentRecognizer
    @Inject
    constructor() : IntentRecognizer {
        private val rules =
            listOf(
                IntentRule(
                    type = IntentType.OpenApp,
                    triggers = listOf("open ", "launch ", "start the ", "start app"),
                    extractSlots = { text -> mapOf("appName" to afterFirstTrigger(text, "open ", "launch ", "start the ")) },
                ),
                IntentRule(
                    type = IntentType.Reminder,
                    triggers = listOf("remind me", "reminder", "don't forget", "do not forget"),
                    extractSlots = { text -> mapOf("reminderText" to text) },
                ),
                IntentRule(
                    type = IntentType.Calendar,
                    triggers = listOf("calendar", "schedule a", "meeting", "appointment", "book a slot"),
                    extractSlots = { text -> mapOf("eventText" to text) },
                ),
                IntentRule(
                    type = IntentType.Research,
                    triggers = listOf("research", "look up", "find out about", "learn about", "what is", "who is"),
                    extractSlots = { text -> mapOf("topic" to text) },
                ),
                IntentRule(
                    type = IntentType.Coding,
                    triggers = listOf("code", "function", "debug", "write a script", "fix this bug", "refactor"),
                ),
                IntentRule(
                    type = IntentType.Shopping,
                    triggers = listOf("buy ", "purchase ", "order ", "shop for", "add to cart"),
                    extractSlots = { text -> mapOf("item" to afterFirstTrigger(text, "buy ", "purchase ", "order ")) },
                ),
                IntentRule(
                    type = IntentType.Navigation,
                    triggers = listOf("navigate to", "directions to", "drive to", "way to", "route to"),
                    extractSlots = { text -> mapOf("destination" to afterFirstTrigger(text, "navigate to", "directions to", "drive to")) },
                ),
                IntentRule(
                    type = IntentType.Email,
                    triggers = listOf("email ", "send a message to", "compose an email", "mail to"),
                    extractSlots = { text -> mapOf("recipient" to afterFirstTrigger(text, "email ", "mail to")) },
                ),
                IntentRule(
                    type = IntentType.Automation,
                    triggers = listOf("turn on", "turn off", "toggle", "lights", "thermostat", "lock the door", "automation"),
                ),
                IntentRule(
                    type = IntentType.Conversation,
                    triggers = listOf("hello", "hi ", "hey", "how are you", "thanks", "thank you", "good morning", "good night"),
                ),
            )

        override suspend fun recognize(utterance: String): AuraResult<RecognizedIntent> {
            val normalized = utterance.trim().lowercase()
            if (normalized.isEmpty()) {
                return AuraResult.Success(RecognizedIntent(IntentType.Unknown, confidence = 0f, rawText = utterance))
            }

            val scored = ScoredKeywordMatcher.match(utterance, rules.map { it to it.triggers })

            val winner =
                scored.firstOrNull()
                    ?: return AuraResult.Success(
                        RecognizedIntent(IntentType.Conversation, confidence = 0.3f, rawText = utterance),
                    )

            return AuraResult.Success(
                RecognizedIntent(
                    type = winner.value.type,
                    confidence = ScoredKeywordMatcher.confidence(winner.hits),
                    slots = winner.value.extractSlots(utterance.trim()),
                    rawText = utterance,
                ),
            )
        }

        private fun afterFirstTrigger(
            text: String,
            vararg triggers: String,
        ): String {
            val lower = text.lowercase()
            for (trigger in triggers) {
                val index = lower.indexOf(trigger)
                if (index >= 0) {
                    return text.substring(index + trigger.length).trim().trimEnd('.', '!', '?')
                }
            }
            return text
        }
    }
