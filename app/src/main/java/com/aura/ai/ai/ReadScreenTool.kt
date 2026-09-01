package com.aura.ai.ai

import com.aura.ai.ai.accessibility.AuraAccessibilityService
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import javax.inject.Inject

/** Reads whatever text is visible on screen right now, via [AuraAccessibilityService] — lives in
 *  `:app` (not core-actions) for the same reason [AppAutomationTool] does: nothing generic to
 *  read without an app-owned integration point, here the accessibility service instance itself
 *  rather than a repository. [requiresConfirmation] is `true`: on-screen content can include
 *  anything the user is currently looking at, private messages or passwords included. */
class ReadScreenTool
    @Inject
    constructor() : Tool {
        override val name = "read_screen"
        override val description = "Reads the text currently visible on screen."
        override val parameters = emptyMap<String, com.aura.ai.core.ai.ParameterSchema>()
        override val requiresConfirmation = true

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            if (!AuraAccessibilityService.isEnabled) {
                return AuraResult.Failure(
                    AuraError.NotSupported("AURA's Accessibility Service isn't enabled — turn it on in Settings first."),
                )
            }
            val text = AuraAccessibilityService.latestScreenText.value
            return if (text.isNullOrBlank()) {
                AuraResult.Success(ToolResult(summary = "Nothing readable is currently on screen."))
            } else {
                AuraResult.Success(ToolResult(summary = text, data = mapOf("screenText" to text)))
            }
        }
    }
