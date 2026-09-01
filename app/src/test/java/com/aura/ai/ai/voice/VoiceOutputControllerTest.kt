package com.aura.ai.ai.voice

import com.aura.ai.domain.model.AssistantVoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** [AssistantVoice.pitch]/[AssistantVoice.rate] are the one piece of persona logic that's pure
 *  Kotlin (no [android.speech.tts.TextToSpeech] instance needed) — see
 *  `docs/VOICE_RUNTIME.md` for why there's no per-persona synthesized voice behind these numbers. */
class VoiceOutputControllerTest {
    @Test
    fun `every persona maps to a distinct pitch, so they're actually distinguishable`() {
        val pitches = AssistantVoice.entries.map { it.pitch() }
        assertEquals(pitches.size, pitches.toSet().size)
    }

    @Test
    fun `neutral is unmodified — pitch and rate both 1x`() {
        assertEquals(1.0f, AssistantVoice.Neutral.pitch())
        assertEquals(1.0f, AssistantVoice.Neutral.rate())
    }

    @Test
    fun `jarvis is deeper and slower than friday`() {
        assertNotEquals(AssistantVoice.Jarvis.pitch(), AssistantVoice.Friday.pitch())
        assert(AssistantVoice.Jarvis.pitch() < AssistantVoice.Friday.pitch())
        assert(AssistantVoice.Jarvis.rate() < AssistantVoice.Friday.rate())
    }
}
