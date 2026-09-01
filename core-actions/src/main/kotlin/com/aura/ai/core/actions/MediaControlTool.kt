package com.aura.ai.core.actions

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private val ACTIONS =
    mapOf(
        "play_pause" to KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        "play" to KeyEvent.KEYCODE_MEDIA_PLAY,
        "pause" to KeyEvent.KEYCODE_MEDIA_PAUSE,
        "next" to KeyEvent.KEYCODE_MEDIA_NEXT,
        "previous" to KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        "stop" to KeyEvent.KEYCODE_MEDIA_STOP,
    )

/**
 * Controls whatever app currently holds the active media session, by dispatching a synthetic
 * media-button [KeyEvent] — the same mechanism a real Bluetooth headset's play/pause button uses.
 * Needs no permission and doesn't know or care which app is actually playing; that's
 * [AudioManager]'s job to route.
 */
class MediaControlTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "media_control"
        override val description = "Controls the current media session: ${ACTIONS.keys.joinToString()}."
        override val parameters =
            mapOf(
                "action" to ParameterSchema(ParameterType.String, "One of: ${ACTIONS.keys.joinToString()}", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val action = arguments["action"]?.trim()?.lowercase()
            val keyCode =
                ACTIONS[action]
                    ?: return AuraResult.Failure(AuraError.InvalidRequest("action must be one of: ${ACTIONS.keys.joinToString()}."))

            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    ?: return AuraResult.Failure(AuraError.Unknown("Audio service is unavailable on this device."))

            val eventTime = System.currentTimeMillis()
            audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))

            return AuraResult.Success(ToolResult(summary = "Sent \"$action\" to the current media session."))
        }
    }
