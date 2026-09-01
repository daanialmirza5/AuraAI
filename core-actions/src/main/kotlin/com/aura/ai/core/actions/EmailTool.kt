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

/** Hands off to the user's email app via `ACTION_SENDTO` — no permission required, and no
 *  email is ever sent without the user seeing and confirming it in their own mail app. */
class EmailTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "compose_email"
        override val description = "Opens an email app with a pre-filled recipient, subject, and body."
        override val parameters =
            mapOf(
                "recipient" to ParameterSchema(ParameterType.String, "Recipient email address"),
                "subject" to ParameterSchema(ParameterType.String, "Email subject"),
                "body" to ParameterSchema(ParameterType.String, "Email body"),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val recipient = arguments["recipient"].orEmpty()
            val subject = arguments["subject"].orEmpty()
            val body = arguments["body"].orEmpty()
            if (recipient.isEmpty() && subject.isEmpty() && body.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("At least one of recipient/subject/body is required."))
            }
            val intent =
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                    if (recipient.isNotEmpty()) putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    if (subject.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, subject)
                    if (body.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, body)
                }
            return context.launchIntentOrFail(intent, "Opened an email draft.")
        }
    }
