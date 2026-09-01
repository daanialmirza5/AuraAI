package com.aura.ai.core.intent

/**
 * The output of [IntentRecognizer.recognize]. [slots] are the free-form entities pulled out of
 * the utterance (e.g. `{"appName": "Spotify"}` for an [IntentType.OpenApp] match) — core-planner
 * reads these to fill in a [com.aura.ai.core.ai.ToolDescriptor]'s arguments without re-parsing
 * [rawText] itself.
 */
data class RecognizedIntent(
    val type: IntentType,
    /** 0f–1f. [KeywordIntentRecognizer] is a heuristic, not a model — treat this as "how many
     *  rules fired," not a calibrated probability. */
    val confidence: Float,
    val slots: Map<String, String> = emptyMap(),
    val rawText: String,
)
