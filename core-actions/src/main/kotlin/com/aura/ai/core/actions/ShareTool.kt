package com.aura.ai.core.actions

import android.content.Context
import android.content.Intent
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Opens Android's share sheet with plain text — the user still has to pick a target and confirm
 *  in that system UI, so there's nothing for this tool itself to confirm. */
class ShareTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "share"
        override val description = "Opens the share sheet with the given text."
        override val parameters =
            mapOf(
                "text" to ParameterSchema(ParameterType.String, "The text to share", required = true),
                "title" to ParameterSchema(ParameterType.String, "Chooser dialog title"),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val text = arguments["text"]?.trim().orEmpty()
            if (text.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("text is required."))
            }
            val title = arguments["title"]?.trim().orEmpty().ifEmpty { "Share via" }

            val sendIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            val chooser = Intent.createChooser(sendIntent, title)
            return context.launchIntentOrFail(chooser, "Opened the share sheet.")
        }
    }
