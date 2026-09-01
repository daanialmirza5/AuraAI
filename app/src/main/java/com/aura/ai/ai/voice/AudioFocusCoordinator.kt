package com.aura.ai.ai.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Requests transient `USAGE_ASSISTANT` audio focus around a listen-or-speak turn and abandons it
 * immediately after — so AURA's voice turn correctly ducks/pauses other audio (music, a podcast)
 * while active and hands focus back cleanly rather than holding it indefinitely. `minSdk = 26`
 * covers the whole [AudioFocusRequest] builder API used here; no version gating needed.
 */
@Singleton
class AudioFocusCoordinator
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        private var activeRequest: AudioFocusRequest? = null

        /** Returns `true` if focus was granted. [onLost] fires if focus is later revoked mid-turn
         *  (e.g. a phone call arrives) — callers use it to stop listening/speaking immediately rather
         *  than continuing to compete for the audio output. */
        fun requestFocus(onLost: () -> Unit): Boolean {
            abandonFocus()
            val request =
                AudioFocusRequest
                    .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    ).setOnAudioFocusChangeListener { change ->
                        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            onLost()
                        }
                    }.build()
            activeRequest = request
            return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        fun abandonFocus() {
            activeRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            activeRequest = null
        }
    }
