package com.aura.ai.ai.voice

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** [VoiceInputEvent.Error.toListenResult] is what decides "silent, expected" vs. "worth telling
 *  the user about" for every recognition error — the one piece of [VoiceRuntime]'s own decision
 *  logic that doesn't need a real [android.speech.SpeechRecognizer] to verify. */
class VoiceRuntimeClassificationTest {
    @Test
    fun `no-match and timeout classify as silent NoSpeech, not a surfaced error`() {
        assertEquals(
            VoiceListenResult.NoSpeech,
            VoiceInputEvent.Error(SpeechRecognizer.ERROR_NO_MATCH, "unused").toListenResult(),
        )
        assertEquals(
            VoiceListenResult.NoSpeech,
            VoiceInputEvent.Error(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, "unused").toListenResult(),
        )
    }

    @Test
    fun `every other error code surfaces as a real, user-visible Error`() {
        val genuineErrors =
            listOf(
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_AUDIO,
                SpeechRecognizer.ERROR_CLIENT,
                SpeechRecognizer.ERROR_SERVER,
            )
        genuineErrors.forEach { code ->
            val result = VoiceInputEvent.Error(code, code.toReadableMessage()).toListenResult()
            assertTrue(result is VoiceListenResult.Error)
        }
    }

    @Test
    fun `the surfaced Error carries the original human-readable message through unchanged`() {
        val message = "Network error during speech recognition."
        val result = VoiceInputEvent.Error(SpeechRecognizer.ERROR_NETWORK, message).toListenResult()
        assertEquals(VoiceListenResult.Error(message), result)
    }
}
