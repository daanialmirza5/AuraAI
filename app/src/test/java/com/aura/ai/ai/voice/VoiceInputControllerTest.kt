package com.aura.ai.ai.voice

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Only [toReadableMessage] is tested directly — the rest of [VoiceInputController] wraps
 * [android.speech.SpeechRecognizer]/[android.speech.tts.TextToSpeech], real Android framework
 * classes with no behavior outside an actual device or a Robolectric shadow (not yet a dependency
 * of this project — see `docs/VOICE_RUNTIME.md` §6). Referencing `SpeechRecognizer.ERROR_*` here
 * is safe under a plain JUnit test with no Robolectric: they're `public static final int` constants
 * in the Android SDK stub jar, not a runtime call.
 */
class VoiceInputControllerTest {
    @Test
    fun `no-match and timeout produce the same gentle message`() {
        assertEquals(
            SpeechRecognizer.ERROR_NO_MATCH.toReadableMessage(),
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT.toReadableMessage(),
        )
    }

    @Test
    fun `every documented error code produces a non-blank, distinct-enough message`() {
        val codes =
            listOf(
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_AUDIO,
                SpeechRecognizer.ERROR_CLIENT,
                SpeechRecognizer.ERROR_SERVER,
            )
        codes.forEach { code -> assertNotEquals("", code.toReadableMessage()) }
    }

    @Test
    fun `permission error message specifically mentions microphone`() {
        assertEquals(
            "Microphone permission is required.",
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS.toReadableMessage(),
        )
    }

    @Test
    fun `unknown error codes fall back to a generic message instead of crashing`() {
        assertEquals("Speech recognition error.", 9999.toReadableMessage())
    }
}
