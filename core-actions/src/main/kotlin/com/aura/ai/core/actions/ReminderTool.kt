package com.aura.ai.core.actions

import android.app.AlarmManager
import android.app.PendingIntent
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

/**
 * Schedules a local reminder notification via [AlarmManager]. Deliberately uses the plain,
 * inexact `set()` rather than `setExactAndAllowWhileIdle()` — inexact alarms need no special
 * permission on any API level, and "fires within a few minutes of the requested time" is an
 * acceptable trade-off for a reminder (unlike, say, a real alarm clock).
 */
class ReminderTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "set_reminder"
        override val description = "Schedules a local reminder notification."
        override val parameters =
            mapOf(
                "message" to ParameterSchema(ParameterType.String, "What to remind the user about", required = true),
                "delayMinutes" to ParameterSchema(ParameterType.Number, "Minutes from now to fire (default 60)"),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val message = arguments["message"]?.trim().orEmpty()
            if (message.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("message is required."))
            }
            val delayMinutes = (arguments["delayMinutes"]?.toLongOrNull() ?: 60L).coerceAtLeast(1L)
            val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60_000L
            val requestCode = message.hashCode()

            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return AuraResult.Failure(AuraError.Unknown("AlarmManager is unavailable on this device."))

            val receiverIntent =
                Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    putExtra(ReminderBroadcastReceiver.EXTRA_MESSAGE, message)
                    putExtra(ReminderBroadcastReceiver.EXTRA_REQUEST_CODE, requestCode)
                }
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    receiverIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

            return AuraResult.Success(
                ToolResult(
                    summary = "Reminder set for $delayMinutes minute(s) from now.",
                    data = mapOf("triggerAtMillis" to triggerAtMillis.toString()),
                ),
            )
        }
    }
