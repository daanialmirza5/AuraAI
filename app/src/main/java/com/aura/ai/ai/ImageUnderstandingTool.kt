package com.aura.ai.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import com.aura.ai.core.ai.AiMessage
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.MessageRole
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.providers.AIProviderManager
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Open-ended "what is this?" image questions — the one vision capability that genuinely needs a
 * real model rather than a fixed on-device task like [com.aura.ai.core.actions.OcrTool]. Routes
 * through [AIProviderManager] exactly like [CodingAgent] already does for text, so this is
 * "another Tool integrated into the existing runtime," not a second way to reach a provider.
 * Honestly inherits [AIProviderManager]'s own connected/not-connected state — see
 * `docs/AI_PROVIDER_INTEGRATION.md`; no new fallback logic was invented here.
 */
class ImageUnderstandingTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val providerManager: AIProviderManager,
    ) : Tool {
        override val name = "understand_image"
        override val description = "Answers a question about an image using a connected AI provider's vision capability."
        override val parameters =
            mapOf(
                "imageUri" to ParameterSchema(ParameterType.String, "content:// or file:// URI of the image", required = true),
                "question" to ParameterSchema(ParameterType.String, "What to ask about the image (default: \"Describe this image.\")"),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val uri =
                arguments.parseImageUri()
                    ?: return AuraResult.Failure(AuraError.InvalidRequest("imageUri is required and must be a valid URI."))
            if (!providerManager.activeProvider.value.capabilities.supportsVision) {
                return AuraResult.Failure(
                    AuraError.NotSupported(
                        "${providerManager.activeProvider.value.displayName} doesn't support image understanding — switch to a vision-capable provider in Settings.",
                    ),
                )
            }
            val question = arguments["question"]?.trim().orEmpty().ifEmpty { "Describe this image." }

            val bytes =
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return AuraResult.Failure(AuraError.InvalidRequest("Couldn't open that image."))
                } catch (e: Exception) {
                    return AuraResult.Failure(AuraError.InvalidRequest("Couldn't open that image."))
                }
            val mimeType =
                context.contentResolver.getType(uri)
                    ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(uri.lastPathSegment?.substringAfterLast('.'))
                    ?: "image/jpeg"
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val request =
                GenerationRequest(
                    messages =
                        listOf(
                            AiMessage(role = MessageRole.User, content = question, imageBase64 = base64, imageMimeType = mimeType),
                        ),
                )
            return when (val result = providerManager.generate(request)) {
                is AuraResult.Success -> AuraResult.Success(ToolResult(summary = result.value.text))
                is AuraResult.Failure -> AuraResult.Failure(result.error)
            }
        }
    }

private fun Map<String, String>.parseImageUri(key: String = "imageUri"): Uri? =
    this[key]?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
