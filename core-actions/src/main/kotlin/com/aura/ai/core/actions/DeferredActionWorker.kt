package com.aura.ai.core.actions

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aura.ai.core.ai.AuraResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs one [Tool] call, later, via WorkManager — the execution engine [DeferredActionTool]
 * schedules onto. Calls [ActionEngine.execute] (the confirmation-gated path), not
 * [ActionEngine.executeConfirmed] — deferring a tool is not the same as a user directly confirming
 * it; if the deferred tool itself [com.aura.ai.core.tools.Tool.requiresConfirmation], it correctly
 * fails here too, exactly as it would if invoked immediately. Deferring cannot be used to bypass
 * that gate.
 */
@HiltWorker
class DeferredActionWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val actionEngine: ActionEngine,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val toolName = inputData.getString(KEY_TOOL_NAME) ?: return Result.failure()
            val keys = inputData.getStringArray(KEY_ARG_KEYS).orEmpty()
            val values = inputData.getStringArray(KEY_ARG_VALUES).orEmpty()
            val arguments = keys.zip(values).toMap()

            return when (actionEngine.execute(toolName, arguments)) {
                is AuraResult.Success -> Result.success()
                is AuraResult.Failure -> Result.failure()
            }
        }

        companion object {
            const val KEY_TOOL_NAME = "tool_name"
            const val KEY_ARG_KEYS = "arg_keys"
            const val KEY_ARG_VALUES = "arg_values"
        }
    }
