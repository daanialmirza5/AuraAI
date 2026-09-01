package com.aura.ai.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.aura.ai.domain.model.AssistantVoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Thin wrapper around [TextToSpeech] — mirrors [VoiceInputController]'s role for the output side.
 * There is no per-voice synthesized model behind [AssistantVoice] (`Jarvis`/`Friday`/`Neutral`) —
 * the platform TTS engine only ever speaks in whatever system voice is installed. Each persona
 * instead maps to a distinguishable pitch/rate combination on that one engine voice; see
 * `docs/VOICE_RUNTIME.md` for why a "real" distinct synthesized voice per persona was out of scope
 * for this milestone (it would mean bundling or downloading a model, the same kind of work
 * `LocalModelProvider` was deferred for in `docs/AI_PROVIDER_INTEGRATION.md`).
 */
@Singleton
class VoiceOutputController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private var tts: TextToSpeech? = null
        private var initialized = false

        private val _state = MutableStateFlow(VoiceOutputState.Idle)
        val state: StateFlow<VoiceOutputState> = _state.asStateFlow()

        /** Speaks [text] and suspends until playback finishes, errors, or the calling coroutine is
         *  cancelled — cancellation (e.g. [VoiceRuntime.interruptSpeaking]) stops the engine
         *  immediately via [kotlinx.coroutines.CancellableContinuation.invokeOnCancellation], the same
         *  way `PlanExecutor`/every other cancellable operation in this codebase relies on structured
         *  concurrency rather than a manual "is this still relevant" flag. */
        suspend fun speak(
            text: String,
            voice: AssistantVoice,
        ) {
            if (text.isBlank()) return
            ensureInitialized()
            val engine = tts ?: return
            suspendCancellableCoroutine<Unit> { cont ->
                val utteranceId = UUID.randomUUID().toString()
                engine.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _state.value = VoiceOutputState.Speaking
                        }

                        override fun onDone(utteranceId: String?) {
                            _state.value = VoiceOutputState.Idle
                            if (cont.isActive) cont.resume(Unit)
                        }

                        @Deprecated("Deprecated in TextToSpeech", ReplaceWith(""))
                        override fun onError(utteranceId: String?) = finishWithError()

                        override fun onError(
                            utteranceId: String?,
                            errorCode: Int,
                        ) = finishWithError()

                        private fun finishWithError() {
                            _state.value = VoiceOutputState.Idle
                            if (cont.isActive) cont.resume(Unit)
                        }
                    },
                )
                cont.invokeOnCancellation {
                    engine.stop()
                    _state.value = VoiceOutputState.Idle
                }
                engine.setLanguage(voice.locale())
                engine.setPitch(voice.pitch())
                engine.setSpeechRate(voice.rate())
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
        }

        fun stop() {
            tts?.stop()
            _state.value = VoiceOutputState.Idle
        }

        private suspend fun ensureInitialized() {
            if (initialized) return
            suspendCancellableCoroutine<Unit> { cont ->
                tts =
                    TextToSpeech(context) { status ->
                        initialized = status == TextToSpeech.SUCCESS
                        if (cont.isActive) cont.resume(Unit)
                    }
            }
        }
    }

private fun AssistantVoice.locale() = java.util.Locale.getDefault()

/** `internal` (not `private`) so it's directly unit-testable — see `VoiceOutputControllerTest`.
 *  Deeper, slightly slower — the "authoritative assistant" persona. */
internal fun AssistantVoice.pitch(): Float =
    when (this) {
        AssistantVoice.Jarvis -> 0.85f
        AssistantVoice.Friday -> 1.15f
        AssistantVoice.Neutral -> 1.0f
    }

internal fun AssistantVoice.rate(): Float =
    when (this) {
        AssistantVoice.Jarvis -> 0.95f
        AssistantVoice.Friday -> 1.08f
        AssistantVoice.Neutral -> 1.0f
    }
