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

/** Resolves an installed app by its display label (fuzzy, case-insensitive) and launches it. */
class OpenAppTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "open_app"
        override val description = "Opens an installed app by its display name."
        override val parameters =
            mapOf(
                "appName" to ParameterSchema(ParameterType.String, "The app's display name, e.g. \"Spotify\"", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val query = arguments["appName"]?.trim().orEmpty()
            if (query.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("appName is required."))
            }

            val packageManager = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val candidates = packageManager.queryIntentActivities(launcherIntent, 0)

            val match =
                candidates.firstOrNull { resolveInfo ->
                    resolveInfo.loadLabel(packageManager).toString().contains(query, ignoreCase = true)
                } ?: return AuraResult.Failure(AuraError.InvalidRequest("No installed app matches \"$query\"."))

            val packageName = match.activityInfo.packageName
            val launchIntent =
                packageManager.getLaunchIntentForPackage(packageName)
                    ?: return AuraResult.Failure(AuraError.Unknown("Found \"$query\" but it has no launchable activity."))

            val label = match.loadLabel(packageManager).toString()
            return context.launchIntentOrFail(launchIntent, "Opened $label.")
        }
    }
