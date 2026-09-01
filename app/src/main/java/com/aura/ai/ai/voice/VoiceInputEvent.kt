package com.aura.ai.ai.voice

/** One event from an in-progress [VoiceInputController] recognition session — the app-level
 *  translation of [android.speech.RecognitionListener]'s callback shape into something a
 *  [kotlinx.coroutines.flow.Flow] consumer can react to sequentially. */
sealed interface VoiceInputEvent {
    /** The recognizer detected the user has started talking — the one signal
     *  [VoiceRuntime.speakInterruptibly] would need for true acoustic barge-in; see
     *  `docs/VOICE_RUNTIME.md` for why that mode isn't wired up by default. */
    data object BeginningOfSpeech : VoiceInputEvent

    data class Partial(
        val text: String,
    ) : VoiceInputEvent

    data class Final(
        val text: String,
    ) : VoiceInputEvent

    /** [code] is one of [android.speech.SpeechRecognizer]'s `ERROR_*` constants — kept alongside
     *  the human-readable [message] so callers can distinguish "genuinely wrong" from "the user
     *  just didn't say anything" ([android.speech.SpeechRecognizer.ERROR_NO_MATCH]/
     *  [android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT], not worth surfacing as an error). */
    data class Error(
        val code: Int,
        val message: String,
    ) : VoiceInputEvent
}
