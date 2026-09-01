package com.aura.ai.core.actions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Posts an immediate local notification — used as the last step of the document pipeline
 *  ("Notify the user") and available as a standalone tool. Relies on the `POST_NOTIFICATIONS`
 *  permission the app already requests through its existing Permissions screen; if it hasn't
 *  been granted, this fails honestly rather than silently doing nothing. */
class NotifyTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "notify"
        override val description = "Shows an immediate local notification."
        override val parameters =
            mapOf(
                "title" to ParameterSchema(ParameterType.String, "Notification title"),
                "message" to ParameterSchema(ParameterType.String, "Notification body", required = true),
            )

        // A verified Lint false positive, not a suppressed real risk: the permission is genuinely
        // checked at runtime immediately below, in every shape tried (early-return, positive-guard,
        // inline condition, intermediate variable) — Lint's MissingPermission data-flow analysis for
        // NotificationManagerCompat.notify(POST_NOTIFICATIONS) does not recognize
        // ContextCompat.checkSelfPermission as a valid guard for this specific API in this AGP/Lint
        // version. A known, longstanding upstream limitation, not something this codebase can fix.
        @SuppressLint("MissingPermission")
        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val message = arguments["message"]?.trim().orEmpty()
            if (message.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("message is required."))
            }
            val title = arguments["title"]?.trim().orEmpty().ifEmpty { "AURA" }

            return if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                NotificationChannels.ensureChannel(context)
                val notification =
                    NotificationCompat
                        .Builder(context, NotificationChannels.AURA_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_aura_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                NotificationManagerCompat.from(context).notify(message.hashCode(), notification)
                AuraResult.Success(ToolResult(summary = "Notified: $message"))
            } else {
                AuraResult.Failure(AuraError.NotSupported("Notification permission has not been granted."))
            }
        }
    }
