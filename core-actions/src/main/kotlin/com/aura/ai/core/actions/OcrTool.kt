package com.aura.ai.core.actions

import android.content.Context
import android.net.Uri
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

/** Extracts text from an image via ML Kit's on-device Latin text recognizer — no network round
 *  trip, no per-call cost, works offline. "Document scanner" in this codebase's scope means
 *  "photograph a document, then run this" (see the camera-capture Quick Action in the Automate
 *  tab); true edge-detection/perspective-correction document scanning is a larger, separate
 *  feature this milestone didn't build — see `docs/VISION_RUNTIME.md` §5. */
class OcrTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "ocr"
        override val description = "Extracts text from an image (also used for document scanning)."
        override val parameters =
            mapOf(
                "imageUri" to ParameterSchema(ParameterType.String, "content:// or file:// URI of the image", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val uri =
                arguments.parseImageUri()
                    ?: return AuraResult.Failure(AuraError.InvalidRequest("imageUri is required and must be a valid URI."))
            return try {
                val text = context.extractTextFromImage(uri)
                if (text.isBlank()) {
                    AuraResult.Success(ToolResult(summary = "No text found in the image."))
                } else {
                    AuraResult.Success(ToolResult(summary = text, data = mapOf("text" to text)))
                }
            } catch (e: IOException) {
                AuraResult.Failure(AuraError.InvalidRequest("Couldn't open that image."))
            } catch (e: Exception) {
                AuraResult.Failure(AuraError.Unknown("Text recognition failed.", e))
            }
        }
    }

internal fun Map<String, String>.parseImageUri(key: String = "imageUri"): Uri? =
    this[key]?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
