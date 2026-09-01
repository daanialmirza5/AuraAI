package com.aura.ai.ai

import android.accessibilityservice.AccessibilityService
import com.aura.ai.ai.accessibility.AuraAccessibilityService
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import javax.inject.Inject

private val ACTIONS =
    mapOf(
        "back" to AccessibilityService.GLOBAL_ACTION_BACK,
        "home" to AccessibilityService.GLOBAL_ACTION_HOME,
        "recents" to AccessibilityService.GLOBAL_ACTION_RECENTS,
    )

/** Presses the system back/home/recents button on the user's behalf, via
 *  [AuraAccessibilityService]'s global actions — plain navigation, not destructive, so this needs
 *  no confirmation. See [ReadScreenTool] for the same service, gated for a much more sensitive
 *  capability. */
class DeviceNavigationTool
    @Inject
    constructor() : Tool {
        override val name = "device_navigate"
        override val description = "Presses back, home, or recents."
        override val parameters =
            mapOf(
                "action" to ParameterSchema(ParameterType.String, "One of: ${ACTIONS.keys.joinToString()}", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val action = arguments["action"]?.trim()?.lowercase()
            val globalAction =
                ACTIONS[action]
                    ?: return AuraResult.Failure(AuraError.InvalidRequest("action must be one of: ${ACTIONS.keys.joinToString()}."))

            if (!AuraAccessibilityService.isEnabled) {
                return AuraResult.Failure(
                    AuraError.NotSupported("AURA's Accessibility Service isn't enabled — turn it on in Settings first."),
                )
            }
            return if (AuraAccessibilityService.performGlobalAction(globalAction)) {
                AuraResult.Success(ToolResult(summary = "Pressed $action."))
            } else {
                AuraResult.Failure(AuraError.Unknown("Could not perform \"$action\" right now."))
            }
        }
    }
