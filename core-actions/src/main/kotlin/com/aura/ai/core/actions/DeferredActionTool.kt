package com.aura.ai.core.actions

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Schedules any other registered tool to run later, unattended, via WorkManager — the "WorkManager
 * automation" primitive Milestone 2 asked for. Deliberately narrow: this is "run one tool once,
 * later," not a recurring/triggered/conditional workflow — that's a distinct, larger subsystem
 * (`docs/TODO_V1.md` §2.6, Milestone 4), which can build on this same WorkManager plumbing rather
 * than duplicating it. [requiresConfirmation] is `true`: scheduling something to happen later with
 * no one watching is more sensitive than an immediate action the user sees and can undo — see
 * `docs/ANDROID_AUTOMATION.md` §3.
 */
class DeferredActionTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "schedule_action"
        override val description = "Runs another tool once, after a delay."
        override val parameters =
            mapOf(
                "toolName" to ParameterSchema(ParameterType.String, "The tool to run later, e.g. \"notify\"", required = true),
                "delayMinutes" to ParameterSchema(ParameterType.Number, "Minutes from now to run it (default 15)"),
                "arg_" to
                    ParameterSchema(ParameterType.String, "Any argument the target tool needs, prefixed \"arg_\" (e.g. \"arg_message\")"),
            )

        override val requiresConfirmation = true

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val toolName = arguments["toolName"]?.trim().orEmpty()
            if (toolName.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("toolName is required."))
            }
            val delayMinutes = (arguments["delayMinutes"]?.toLongOrNull() ?: 15L).coerceAtLeast(1L)
            val targetArguments =
                arguments
                    .filterKeys { it.startsWith("arg_") }
                    .mapKeys { (key, _) -> key.removePrefix("arg_") }

            val inputData =
                Data
                    .Builder()
                    .putString(DeferredActionWorker.KEY_TOOL_NAME, toolName)
                    .putStringArray(DeferredActionWorker.KEY_ARG_KEYS, targetArguments.keys.toTypedArray())
                    .putStringArray(DeferredActionWorker.KEY_ARG_VALUES, targetArguments.values.toTypedArray())
                    .build()

            val request =
                OneTimeWorkRequestBuilder<DeferredActionWorker>()
                    .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                    .setInputData(inputData)
                    .build()

            WorkManager.getInstance(context).enqueue(request)

            return AuraResult.Success(
                ToolResult(summary = "Scheduled \"$toolName\" to run in $delayMinutes minute(s)."),
            )
        }
    }
