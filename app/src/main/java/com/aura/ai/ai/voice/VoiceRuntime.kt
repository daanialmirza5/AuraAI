package com.aura.ai.ai.voice

import android.speech.SpeechRecognizer
import com.aura.ai.domain.model.AssistantVoice
import kotlinx.coroutines.flow.takeWhile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one thing [com.aura.ai.presentation.aura.AuraTabViewModel] depends on for voice — composes
 * [VoiceInputController], [VoiceOutputController], and [AudioFocusCoordinator] into two
 * suspend-based operations a voice turn actually needs, the same "one facade, several composed
 * collaborators" shape [com.aura.ai.ai.runtime.AuraRuntimeFacade] already uses for text. Neither
 * controller is ever exposed directly to the ViewModel — only through this class — so audio-focus
 * and cleanup bookkeeping can never be forgotten at a call site.
 */
@Singleton
class VoiceRuntime
    @Inject
    constructor(
        private val input: VoiceInputController,
        private val output: VoiceOutputController,
        private val audioFocus: AudioFocusCoordinator,
    ) {
        val isSupported: Boolean get() = input.isSupported

        val outputState get() = output.state

        /**
         * Listens for exactly one utterance. Suspends until a final result, a real error, or the
         * device's own silence timeout ends the session — there is no separate hand-rolled timer.
         * [onPartial] fires live as interim transcripts arrive, for showing streaming STT text in the
         * UI (`AuraTabViewModel` routes it into the existing chat-input field rather than adding a new
         * one). Cancelling the calling coroutine cancels the recognition session cleanly.
         */
        suspend fun listenOnce(onPartial: (String) -> Unit = {}): VoiceListenResult {
            if (!isSupported) return VoiceListenResult.Unsupported
            audioFocus.requestFocus(onLost = { input.stopListening() })
            try {
                input.startListening()
                var result: VoiceListenResult = VoiceListenResult.NoSpeech
                input.events
                    .takeWhile { event ->
                        when (event) {
                            is VoiceInputEvent.Partial -> {
                                onPartial(event.text)
                                true
                            }
                            is VoiceInputEvent.Final -> {
                                result = event.text
                                    .takeIf { it.isNotBlank() }
                                    ?.let(VoiceListenResult::Transcript)
                                    ?: VoiceListenResult.NoSpeech
                                false
                            }
                            is VoiceInputEvent.Error -> {
                                result = event.toListenResult()
                                false
                            }
                            VoiceInputEvent.BeginningOfSpeech -> true
                        }
                    }.collect {}
                return result
            } finally {
                input.cancel()
                audioFocus.abandonFocus()
            }
        }

        /** Speaks [text] in [voice]'s persona and suspends until done. See
         *  [VoiceOutputController.speak] for how interruption (barge-in) is just cancelling this
         *  suspend call, not a separate code path. */
        suspend fun speak(
            text: String,
            voice: AssistantVoice,
        ) {
            audioFocus.requestFocus(onLost = { output.stop() })
            try {
                output.speak(text, voice)
            } finally {
                audioFocus.abandonFocus()
            }
        }

        /** Manual, tap-triggered barge-in — see `docs/VOICE_RUNTIME.md` for why acoustic
         *  (speech-triggered) barge-in isn't wired up: it would mean listening and speaking through
         *  the same device microphone/speaker simultaneously, whose false-positive risk from picking
         *  up AURA's own voice can't be verified without physical hardware this environment doesn't
         *  have. Cancelling the coroutine that's suspended inside [speak] is what actually stops
         *  playback — this just makes that call site explicit and named. */
        fun interruptSpeaking() {
            output.stop()
        }
    }

/** `internal` (not `private`) so it's directly unit-testable without instantiating [VoiceRuntime]
 *  and its three Android-framework-backed collaborators — see `VoiceRuntimeClassificationTest`. */
internal fun VoiceInputEvent.Error.toListenResult(): VoiceListenResult =
    when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceListenResult.NoSpeech
        else -> VoiceListenResult.Error(message)
    }
