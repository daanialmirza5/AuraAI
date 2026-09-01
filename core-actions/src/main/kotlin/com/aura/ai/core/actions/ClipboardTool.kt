package com.aura.ai.core.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads or writes the system clipboard. `read` is honest about a real Android platform
 * restriction, not a bug here: since API 29, an app that isn't the default input method or
 * currently focused gets an empty clipboard from [ClipboardManager] — AURA running in the
 * background will usually see nothing, which this tool reports plainly rather than pretending
 * it read something.
 */
class ClipboardTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "clipboard"
        override val description = "Reads or writes the system clipboard."
        override val parameters =
            mapOf(
                "action" to ParameterSchema(ParameterType.String, "\"read\" or \"write\"", required = true),
                "text" to ParameterSchema(ParameterType.String, "Text to write (required when action is \"write\")"),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: return AuraResult.Failure(AuraError.Unknown("Clipboard service is unavailable on this device."))

            return when (arguments["action"]?.trim()?.lowercase()) {
                "write" -> {
                    val text = arguments["text"].orEmpty()
                    if (text.isEmpty()) {
                        return AuraResult.Failure(AuraError.InvalidRequest("text is required for a write."))
                    }
                    clipboard.setPrimaryClip(ClipData.newPlainText("AURA", text))
                    AuraResult.Success(ToolResult(summary = "Copied to clipboard."))
                }
                "read" -> {
                    val text =
                        clipboard.primaryClip
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                            ?.toString()
                    if (text.isNullOrEmpty()) {
                        AuraResult.Success(ToolResult(summary = "The clipboard is empty (or unreadable while AURA is in the background)."))
                    } else {
                        AuraResult.Success(ToolResult(summary = "Clipboard: $text", data = mapOf("text" to text)))
                    }
                }
                else -> AuraResult.Failure(AuraError.InvalidRequest("action must be \"read\" or \"write\"."))
            }
        }
    }
