package com.aura.ai.core.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Hands off to any installed maps app via the standard `geo:` URI — no permission required. */
class NavigationTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "navigate"
        override val description = "Opens a maps app with directions to a destination."
        override val parameters =
            mapOf(
                "destination" to ParameterSchema(ParameterType.String, "Where to navigate to", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val destination = arguments["destination"]?.trim().orEmpty()
            if (destination.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("destination is required."))
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(destination)))
            return context.launchIntentOrFail(intent, "Navigating to $destination.")
        }
    }
