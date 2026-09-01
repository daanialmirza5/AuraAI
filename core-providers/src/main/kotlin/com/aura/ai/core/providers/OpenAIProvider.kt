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

private const val OPENAI_API_BASE = "https://api.openai.com/v1/chat/completions"
private const val OPENAI_MODEL = "gpt-4o-mini"
private const val SSE_DONE = "[DONE]"

/** The real OpenAI Chat Completions integration — same shape as [ClaudeProvider], adapted to
 *  OpenAI's request/response schema. See [docs/AI_PROVIDER_INTEGRATION.md]. */
@Singleton
class OpenAIProvider
    @Inject
    constructor(
        private val httpClient: OkHttpClient,
        private val credentialStore: ProviderCredentialStore,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : AIProvider {
        /** Overridable only for tests — see [ClaudeProvider.apiBaseUrl] for why this is a property,
         *  not a constructor parameter. */
        internal var apiBaseUrl: String = OPENAI_API_BASE

        override val id: String = ProviderId.OpenAI.key
        override val displayName: String = "OpenAI"
        override val capabilities: ProviderCapabilities =
            ProviderCapabilities(
                supportsStreaming = true,
                supportsTools = true,
                supportsVision = true,
                isLocal = false,
                maxContextTokens = 128_000,
            )

        override suspend fun isAvailable(): Boolean = credentialStore.apiKey(ProviderId.OpenAI) != null

        override suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse> {
            val apiKey =
                credentialStore.apiKey(ProviderId.OpenAI)
                    ?: return AuraResult.Failure(AuraError.ProviderNotConnected(id, "$displayName has no API key configured yet."))
            return withContext(ioDispatcher) {
                try {
                    val httpRequest = buildRequest(apiKey, request, stream = false)
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
                val apiKey = credentialStore.apiKey(ProviderId.OpenAI)
                if (apiKey == null) {
                    trySend(AuraResult.Failure(AuraError.ProviderNotConnected(id, "$displayName has no API key configured yet.")))
                    close()
                    return@callbackFlow
                }
                val httpRequest = buildRequest(apiKey, request, stream = true)
                val listener =
                    object : EventSourceListener() {
                        override fun onEvent(
                            eventSource: EventSource,
                            id: String?,
                            type: String?,
                            data: String,
                        ) {
                            if (data == SSE_DONE) {
                                trySend(AuraResult.Success(GenerationChunk(delta = "", isFinal = true)))
                                return
                            }
                            val delta =
                                providerJson
                                    .parseToJsonElement(data)
                                    .jsonObject["choices"]
                                    ?.jsonArray
                                    ?.firstOrNull()
                                    ?.jsonObject
                                    ?.get("delta")
                                    ?.jsonObject
                                    ?.get("content")
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                            if (!delta.isNullOrEmpty()) trySend(AuraResult.Success(GenerationChunk(delta = delta)))
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
                            close()
                        }
                    }
                val eventSource = EventSources.createFactory(httpClient).newEventSource(httpRequest, listener)
                awaitClose { eventSource.cancel() }
            }

        private fun buildRequest(
            apiKey: String,
            request: GenerationRequest,
            stream: Boolean,
        ): Request {
            val body =
                buildJsonObject {
                    put("model", OPENAI_MODEL)
                    put("temperature", request.temperature.toDouble())
                    request.maxOutputTokens?.let { put("max_completion_tokens", it) }
                    put("stream", stream)
                    put(
                        "messages",
                        buildJsonArray {
                            request.systemPrompt?.let {
                                add(
                                    buildJsonObject {
                                        put("role", "system")
                                        put("content", it)
                                    },
                                )
                            }
                            request.messages.forEach { add(it.toOpenAiMessage()) }
                        },
                    )
                    if (request.availableTools.isNotEmpty()) {
                        put(
                            "tools",
                            buildJsonArray {
                                request.availableTools.forEach { tool ->
                                    add(
                                        buildJsonObject {
                                            put("type", "function")
                                            put(
                                                "function",
                                                buildJsonObject {
                                                    put("name", tool.name)
                                                    put("description", tool.description)
                                                    put("parameters", tool.toJsonSchemaParameters())
                                                },
                                            )
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            return Request
                .Builder()
                .url(apiBaseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
        }

        private fun AiMessage.toOpenAiMessage(): JsonObject =
            buildJsonObject {
                put("role", role.toOpenAiRoleName())
                if (imageBase64 != null) {
                    put(
                        "content",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "text")
                                    put("text", content)
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("type", "image_url")
                                    put(
                                        "image_url",
                                        buildJsonObject {
                                            put("url", "data:${imageMimeType ?: "image/jpeg"};base64,$imageBase64")
                                        },
                                    )
                                },
                            )
                        },
                    )
                } else {
                    put("content", content)
                }
                if (role == MessageRole.Tool) put("tool_call_id", toolCallId.orEmpty())
            }

        private fun MessageRole.toOpenAiRoleName(): String =
            when (this) {
                MessageRole.System -> "system"
                MessageRole.User -> "user"
                MessageRole.Assistant -> "assistant"
                MessageRole.Tool -> "tool"
            }

        private fun parseGenerateResponse(bodyText: String): GenerationResponse {
            val root = providerJson.parseToJsonElement(bodyText).jsonObject
            val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            val message = choice?.get("message")?.jsonObject
            val text =
                message
                    ?.get("content")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    .orEmpty()
            val toolCalls =
                message?.get("tool_calls")?.jsonArray.orEmpty().map { call ->
                    val obj = call.jsonObject
                    val function = obj["function"]?.jsonObject
                    val argsText =
                        function
                            ?.get("arguments")
                            ?.jsonPrimitive
                            ?.contentOrNull
                            .orEmpty()
                    val args =
                        runCatching { providerJson.parseToJsonElement(argsText).jsonObject.toStringArgumentMap() }
                            .getOrDefault(emptyMap())
                    ToolCallRequest(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        toolName =
                            function
                                ?.get("name")
                                ?.jsonPrimitive
                                ?.contentOrNull
                                .orEmpty(),
                        arguments = args,
                    )
                }
            val finishReason = choice?.get("finish_reason")?.jsonPrimitive?.contentOrNull
            val usage = root["usage"]?.jsonObject
            return GenerationResponse(
                text = text,
                toolCalls = toolCalls,
                finishReason =
                    when (finishReason) {
                        "tool_calls" -> FinishReason.ToolCall
                        "length" -> FinishReason.Length
                        "content_filter" -> FinishReason.ContentFilter
                        else -> FinishReason.Stop
                    },
                usage =
                    usage?.let {
                        TokenUsage(
                            promptTokens = it["prompt_tokens"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                            completionTokens = it["completion_tokens"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                        )
                    },
            )
        }

        private fun retryAfterHeaderMillis(response: okhttp3.Response): Long? = response.header("retry-after")?.toLongOrNull()?.times(1000)
    }
