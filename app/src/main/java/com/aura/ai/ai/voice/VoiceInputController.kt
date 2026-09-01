package com.aura.ai.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [SpeechRecognizer] — the only thing this class does is turn its
 * callback-based [RecognitionListener] API into a [SharedFlow] of [VoiceInputEvent], so
 * [VoiceRuntime] can consume it with normal `Flow` operators instead of nested callbacks. No
 * transcription logic, no conversation awareness — that's [VoiceRuntime]'s job, one layer up,
 * exactly the same split [com.aura.ai.ai.runtime.ConversationPipeline] already uses relative to
 * the individual core-* engines it composes.
 *
 * [SpeechRecognizer] must be created and driven from the main thread — every public method here
 * is expected to be called from a coroutine on `Dispatchers.Main` (true by default for
 * `viewModelScope`, which is the only caller). Lazily created on first [startListening] rather
 * than at injection time, since Hilt singleton construction isn't guaranteed to happen on the
 * main thread.
 */
@Singleton
class VoiceInputController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private var recognizer: SpeechRecognizer? = null

        private val _events = MutableSharedFlow<VoiceInputEvent>(extraBufferCapacity = 16)
        val events: SharedFlow<VoiceInputEvent> = _events.asSharedFlow()

        val isSupported: Boolean
            get() = SpeechRecognizer.isRecognitionAvailable(context)

        fun startListening() {
            if (!isSupported) return
            val active = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
            active.setRecognitionListener(listener)
            active.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                },
            )
        }

        fun stopListening() {
            recognizer?.stopListening()
        }

        /** Cancels immediately, discarding any in-flight result — used for both explicit
         *  user-cancellation and cleanup after [VoiceRuntime.listenOnce] returns. */
        fun cancel() {
            recognizer?.cancel()
        }

        /** Releases the underlying recognizer entirely. Not called during normal use — a
         *  process-lifetime [Singleton] recognizer avoids repeated create/destroy overhead on every
         *  orb tap — but available for a future settings "reset voice engine" action or test teardown. */
        fun destroy() {
            recognizer?.destroy()
            recognizer = null
        }

        private val listener =
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit

                override fun onBeginningOfSpeech() {
                    _events.tryEmit(VoiceInputEvent.BeginningOfSpeech)
                }

                override fun onPartialResults(partialResults: Bundle) {
                    partialResults.bestMatch()?.let { _events.tryEmit(VoiceInputEvent.Partial(it)) }
                }

                override fun onResults(results: Bundle) {
                    _events.tryEmit(VoiceInputEvent.Final(results.bestMatch().orEmpty()))
                }

                override fun onError(error: Int) {
                    _events.tryEmit(VoiceInputEvent.Error(error, error.toReadableMessage()))
                }

                override fun onEndOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?,
                ) = Unit
            }
    }

private fun Bundle.bestMatch(): String? = getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull { it.isNotBlank() }

internal fun Int.toReadableMessage(): String =
    when (this) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error during speech recognition."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy — try again in a moment."
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was cancelled."
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
        else -> "Speech recognition error."
    }
