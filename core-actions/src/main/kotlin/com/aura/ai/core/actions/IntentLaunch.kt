package com.aura.ai.core.actions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.tools.ToolResult

/** The common "hand off to whatever app can do this" pattern shared by every intent-based tool
 *  (open app, calendar, navigation, email, web/shopping search) — each of those needs zero new
 *  Android permissions precisely because they delegate to another app's own UI rather than
 *  performing the action directly. */
internal fun Context.launchIntentOrFail(
    intent: Intent,
    successSummary: String,
): AuraResult<ToolResult> {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        startActivity(intent)
        AuraResult.Success(ToolResult(summary = successSummary))
    } catch (e: ActivityNotFoundException) {
        AuraResult.Failure(AuraError.Unknown("No app on this device can handle that action.", e))
    }
}
