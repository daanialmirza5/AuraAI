package com.aura.ai.core.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private val SCREEN_ACTIONS =
    mapOf(
        "wifi" to Settings.ACTION_WIFI_SETTINGS,
        "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
        "display" to Settings.ACTION_DISPLAY_SETTINGS,
        "sound" to Settings.ACTION_SOUND_SETTINGS,
        "battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
        "location" to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
        "notifications" to Settings.ACTION_APP_NOTIFICATION_SETTINGS,
        "accessibility" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
        "date" to Settings.ACTION_DATE_SETTINGS,
        "storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        "apps" to Settings.ACTION_APPLICATION_SETTINGS,
        "wireless" to Settings.ACTION_WIRELESS_SETTINGS,
    )

/** Navigates to a system Settings screen — never changes a setting itself, only opens the screen
 *  for the user to act on, so this needs no special permission and nothing to confirm. */
class OpenSettingsTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "open_settings"
        override val description = "Opens a system Settings screen (Wi-Fi, Bluetooth, display, etc.)."
        override val parameters =
            mapOf(
                "screen" to
                    ParameterSchema(
                        ParameterType.String,
                        "One of: ${SCREEN_ACTIONS.keys.joinToString()}. Falls back to the Settings home screen if omitted or unrecognized.",
                    ),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val screen = arguments["screen"]?.trim()?.lowercase()
            return when {
                screen.isNullOrEmpty() -> context.launchIntentOrFail(Intent(Settings.ACTION_SETTINGS), "Opened Settings.")
                screen == "app" || screen == "this app" || screen == "aura" -> {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                    context.launchIntentOrFail(intent, "Opened AURA's app settings.")
                }
                SCREEN_ACTIONS.containsKey(screen) ->
                    context.launchIntentOrFail(Intent(SCREEN_ACTIONS.getValue(screen)), "Opened $screen settings.")
                else -> AuraResult.Failure(AuraError.InvalidRequest("Unrecognized settings screen \"$screen\"."))
            }
        }
    }
