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

/** Opens a shopping search in the browser — same rationale as [WebSearchTool], scoped to
 *  Google's shopping results tab. No purchase is ever made without the user leaving AURA and
 *  acting in the destination site themselves. */
class ShoppingSearchTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "shopping_search"
        override val description = "Opens a shopping search for the given item."
        override val parameters =
            mapOf(
                "query" to ParameterSchema(ParameterType.String, "What to shop for", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val query = arguments["query"]?.trim().orEmpty()
            if (query.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("query is required."))
            }
            val url = "https://www.google.com/search?tbm=shop&q=" + Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            return context.launchIntentOrFail(intent, "Searching shopping results for \"$query\".")
        }
    }
