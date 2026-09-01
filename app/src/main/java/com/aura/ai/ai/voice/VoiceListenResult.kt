package com.aura.ai.ai.voice

/** The outcome of one [VoiceRuntime.listenOnce] call — a closed set so
 *  [com.aura.ai.presentation.aura.AuraTabViewModel] can tell "the user said nothing" (silent,
 *  expected — happens every time a listening window ends naturally) apart from "something is
 *  actually wrong" (worth surfacing) without inspecting error codes itself. */
sealed interface VoiceListenResult {
    data class Transcript(
        val text: String,
    ) : VoiceListenResult

    data object NoSpeech : VoiceListenResult

    data object Unsupported : VoiceListenResult

    data class Error(
        val message: String,
    ) : VoiceListenResult
}
