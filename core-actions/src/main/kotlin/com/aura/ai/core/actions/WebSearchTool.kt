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

/** Opens a web search in the browser — deliberately `ACTION_VIEW` on a search URL rather than
 *  `ACTION_WEB_SEARCH`, since every device with any browser resolves the former, while the
 *  latter depends on a specific search-handling app being present. Covers the Research intent
 *  today; a connected provider would eventually read and synthesize results instead of just
 *  opening them. */
class WebSearchTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "web_search"
        override val description = "Opens a web search for the given query."
        override val parameters =
            mapOf(
                "query" to ParameterSchema(ParameterType.String, "What to search for", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val query = arguments["query"]?.trim().orEmpty()
            if (query.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("query is required."))
            }
            val url = "https://www.google.com/search?q=" + Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            return context.launchIntentOrFail(intent, "Searching for \"$query\".")
        }
    }
