package com.aura.ai.core.actions

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Hands off to the user's own Calendar app to create an event — `ACTION_INSERT` against
 *  [CalendarContract.Events.CONTENT_URI] needs no `WRITE_CALENDAR` permission because the
 *  Calendar app itself performs the write after the user reviews and confirms it. */
class CalendarTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "create_calendar_event"
        override val description = "Opens the Calendar app to create an event with a pre-filled title."
        override val parameters =
            mapOf(
                "title" to ParameterSchema(ParameterType.String, "The event title", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val title = arguments["title"]?.trim().orEmpty()
            if (title.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("title is required."))
            }
            val intent =
                Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                }
            return context.launchIntentOrFail(intent, "Opened Calendar to create \"$title\".")
        }
    }
