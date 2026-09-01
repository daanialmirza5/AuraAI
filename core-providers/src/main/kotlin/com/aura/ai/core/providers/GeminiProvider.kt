package com.aura.ai.core.providers

import com.aura.ai.core.ai.AIProvider
import com.aura.ai.core.ai.AiMessage
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.FinishReason
import com.aura.ai.core.ai.GenerationChunk
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.GenerationResponse
import com.aura.ai.core.ai.MessageRole
import com.aura.ai.core.ai.ProviderCapabilities
import com.aura.ai.core.ai.TokenUsage
import com.aura.ai.core.ai.ToolCallRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val GEMINI_MODEL = "gemini-2.0-flash"
private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

/** The real Gemini `generateContent`/`streamGenerateContent` integration. Gemini authenticates via
 *  an API-key query parameter rather than a header — the one meaningful transport difference from
 *  [ClaudeProvider]/[OpenAIProvider]. See [docs/AI_PROVIDER_INTEGRATION.md]. */
@Singleton
class GeminiProvider
    @Inject
    constructor(
        private val httpClient: OkHttpClient,
        private val credentialStore: ProviderCredentialStore,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : AIProvider {
        /** Overridable only for tests — see [ClaudeProvider.apiBaseUrl] for why this is a property,
         *  not a constructor parameter. */
        internal var apiBaseUrl: String = GEMINI_API_BASE

        override val id: String = ProviderId.Gemini.key
        override val displayName: String = "Google Gemini"
        override val capabilities: ProviderCapabilities =
            ProviderCapabilities(
                supportsStreaming = true,
                supportsTools = true,
                supportsVision = true,
                isLocal = false,
                maxContextTokens = 1_000_000,
            )

        override suspend fun isAvailable(): Boolean = credentialStore.apiKey(ProviderId.Gemini) != null

        override suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse> {
            val apiKey =
                credentialStore.apiKey(ProviderId.Gemini)
                    ?: return AuraResult.Failure(AuraError.ProviderNotConnected(id, "$displayName has no API key configured yet."))
            return withContext(ioDispatcher) {
                try {
                    val url = "$apiBaseUrl/$GEMINI_MODEL:generateContent?key=$apiKey"
                    val httpRequest = buildRequest(url, request)
                    httpClient.newCall(httpRequest).execute().use { response ->
                        val bodyText = response.body?.string()
                        if (!response.isSuccessful) {
                            AuraResult.Failure(
                                ProviderHttpErrors.fromStatus(displayName, response.code, retryAfterHeaderMillis(response), bodyText),
                            )
                        } else {
                            AuraResult.Success(parseGenerateResponse(bodyText.orEmpty()))
                        }
                    }
                } catch (e: IOException) {
                    AuraResult.Failure(ProviderHttpErrors.fromException(displayName, e))
                }
            }
        }

        override fun generateStream(request: GenerationRequest): Flow<AuraResult<GenerationChunk>> =
            callbackFlow {
                val apiKey = credentialStore.apiKey(ProviderId.Gemini)
                if (apiKey == null) {
                    trySend(AuraResult.Failure(AuraError.ProviderNotConnected(id, "$displayName has no API key configured yet.")))
                    close()
                    return@callbackFlow
                }
                val url = "$apiBaseUrl/$GEMINI_MODEL:streamGenerateContent?alt=sse&key=$apiKey"
                val httpRequest = buildRequest(url, request)
                val listener =
                    object : EventSourceListener() {
                        override fun onEvent(
                            eventSource: EventSource,
                            id: String?,
                            type: String?,
                            data: String,
                        ) {
                            val delta = extractText(providerJson.parseToJsonElement(data).jsonObject)
                            if (delta.isNotEmpty()) trySend(AuraResult.Success(GenerationChunk(delta = delta)))
                        }

                        override fun onFailure(
                            eventSource: EventSource,
                            t: Throwable?,
                            response: okhttp3.Response?,
                        ) {
                            val failure =
                                if (response != null && !response.isSuccessful) {
                                    ProviderHttpErrors.fromStatus(displayName, response.code, retryAfterHeaderMillis(response), null)
                                } else {
                                    ProviderHttpErrors.fromException(displayName, t ?: IOException("Stream closed unexpectedly."))
                                }
                            trySend(AuraResult.Failure(failure))
                            close()
                        }

                        override fun onClosed(eventSource: EventSource) {
                            trySend(AuraResult.Success(GenerationChunk(delta = "", isFinal = true)))
                            close()
                        }
                    }
                val eventSource = EventSources.createFactory(httpClient).newEventSource(httpRequest, listener)
                awaitClose { eventSource.cancel() }
            }

        private fun buildRequest(
            url: String,
            request: GenerationRequest,
        ): Request {
            val body =
                buildJsonObject {
                    put(
                        "contents",
                        buildJsonArray {
                            request.messages.filter { it.role != MessageRole.System }.forEach { add(it.toGeminiContent()) }
                        },
                    )
                    val systemText =
                        buildString {
                            request.systemPrompt?.let { append(it) }
                            request.messages.filter { it.role == MessageRole.System }.forEach {
                                if (isNotEmpty()) append("\n\n")
                                append(it.content)
                            }
                        }
                    if (systemText.isNotBlank()) {
                        put(
                            "systemInstruction",
                            buildJsonObject {
                                put("parts", buildJsonArray { add(buildJsonObject { put("text", systemText) }) })
                            },
                        )
                    }
                    put(
                        "generationConfig",
                        buildJsonObject {
                            put("temperature", request.temperature.toDouble())
                            request.maxOutputTokens?.let { put("maxOutputTokens", it) }
                        },
                    )
                    if (request.availableTools.isNotEmpty()) {
                        put(
                            "tools",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put(
                                            "functionDeclarations",
                                            buildJsonArray {
                                                request.availableTools.forEach { tool ->
                                                    add(
                                                        buildJsonObject {
                                                            put("name", tool.name)
                                                            put("description", tool.description)
                                                            put("parameters", tool.toJsonSchemaParameters())
                                                        },
                                                    )
                                                }
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }
                }
            return Request
                .Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
        }

        private fun AiMessage.toGeminiContent(): JsonObject =
            when (role) {
                MessageRole.Tool ->
                    buildJsonObject {
                        put("role", "function")
                        put(
                            "parts",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put(
                                            "functionResponse",
                                            buildJsonObject {
                                                put("name", name.orEmpty())
                                                put("response", buildJsonObject { put("content", content) })
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }
                else ->
                    buildJsonObject {
                        put("role", if (role == MessageRole.Assistant) "model" else "user")
                        put(
                            "parts",
                            buildJsonArray {
                                if (imageBase64 != null) {
                                    add(
                                        buildJsonObject {
                                            put(
                                                "inlineData",
                                                buildJsonObject {
                                                    put("mimeType", imageMimeType ?: "image/jpeg")
                                                    put("data", imageBase64)
                                                },
                                            )
                                        },
                                    )
                                }
                                add(buildJsonObject { put("text", content) })
                            },
                        )
                    }
            }

        private fun extractText(root: JsonObject): String {
            val parts =
                root["candidates"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("content")
                    ?.jsonObject
                    ?.get("parts")
                    ?.jsonArray
                    .orEmpty()
            return parts.joinToString("") {
                it.jsonObject["text"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    .orEmpty()
            }
        }

        private fun parseGenerateResponse(bodyText: String): GenerationResponse {
            val root = providerJson.parseToJsonElement(bodyText).jsonObject
            val candidate = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            val parts =
                candidate
                    ?.get("content")
                    ?.jsonObject
                    ?.get("parts")
                    ?.jsonArray
                    .orEmpty()
            val text =
                parts.joinToString("") {
                    it.jsonObject["text"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        .orEmpty()
                }
            val toolCalls =
                parts.mapNotNull { part ->
                    val call = part.jsonObject["functionCall"]?.jsonObject ?: return@mapNotNull null
                    ToolCallRequest(
                        id = call["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        toolName = call["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        arguments = call["args"]?.jsonObject?.toStringArgumentMap().orEmpty(),
                    )
                }
            val finishReason = candidate?.get("finishReason")?.jsonPrimitive?.contentOrNull
            val usage = root["usageMetadata"]?.jsonObject
            return GenerationResponse(
                text = text,
                toolCalls = toolCalls,
                finishReason =
                    when (finishReason) {
                        "MAX_TOKENS" -> FinishReason.Length
                        "SAFETY", "RECITATION" -> FinishReason.ContentFilter
                        else -> if (toolCalls.isNotEmpty()) FinishReason.ToolCall else FinishReason.Stop
                    },
                usage =
                    usage?.let {
                        TokenUsage(
                            promptTokens = it["promptTokenCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                            completionTokens = it["candidatesTokenCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                        )
                    },
            )
        }

        private fun retryAfterHeaderMillis(response: okhttp3.Response): Long? = response.header("retry-after")?.toLongOrNull()?.times(1000)
    }
