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

private const val CLAUDE_API_BASE = "https://api.anthropic.com/v1/messages"
private const val CLAUDE_API_VERSION = "2023-06-01"
private const val CLAUDE_MODEL = "claude-sonnet-5"

/**
 * The real Anthropic Messages API integration — the scaffold this replaces is documented in
 * [docs/AI_PROVIDER_INTEGRATION.md]. Every call still honestly fails with
 * [AuraError.ProviderNotConnected] when no API key has been configured; once one is, this talks
 * to `api.anthropic.com` for real, on the calling coroutine's dispatcher via OkHttp.
 */
@Singleton
class ClaudeProvider
    @Inject
    constructor(
        private val httpClient: OkHttpClient,
        private val credentialStore: ProviderCredentialStore,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : AIProvider {
        /** Overridable only for tests (e.g. pointing at a [okhttp3.mockwebserver.MockWebServer]) — kept
         *  as a settable property rather than a constructor parameter because Dagger's generated
         *  constructor call ignores Kotlin default argument values and would otherwise demand an
         *  (unqualified, dangerously ambiguous) `String` binding for it. No Settings UI exposes this
         *  for a cloud provider; see [OllamaProvider] for the one provider where a base-url override is
         *  a real, user-facing feature. */
        internal var apiBaseUrl: String = CLAUDE_API_BASE

        override val id: String = ProviderId.Claude.key
        override val displayName: String = "Anthropic Claude"
        override val capabilities: ProviderCapabilities =
            ProviderCapabilities(
                supportsStreaming = true,
                supportsTools = true,
                supportsVision = true,
                isLocal = false,
                maxContextTokens = 200_000,
            )

        override suspend fun isAvailable(): Boolean = credentialStore.apiKey(ProviderId.Claude) != null

        override suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse> {
            val apiKey =
                credentialStore.apiKey(ProviderId.Claude)
                    ?: return AuraResult.Failure(
                        AuraError.ProviderNotConnected(id, "$displayName has no API key configured yet."),
                    )
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
                val apiKey = credentialStore.apiKey(ProviderId.Claude)
                if (apiKey == null) {
                    trySend(AuraResult.Failure(AuraError.ProviderNotConnected(id, "$displayName has no API key configured yet.")))
                    close()
                    return@callbackFlow
                }
                val httpRequest = buildRequest(apiKey, request, stream = true)
                var sawFinalChunk = false
                val listener =
                    object : EventSourceListener() {
                        override fun onEvent(
                            eventSource: EventSource,
                            id: String?,
                            type: String?,
                            data: String,
                        ) {
                            val event = providerJson.parseToJsonElement(data).jsonObject
                            when (event["type"]?.jsonPrimitive?.content) {
                                "content_block_delta" -> {
                                    val text =
                                        event["delta"]
                                            ?.jsonObject
                                            ?.get("text")
                                            ?.jsonPrimitive
                                            ?.content
                                    if (!text.isNullOrEmpty()) trySend(AuraResult.Success(GenerationChunk(delta = text)))
                                }
                                "message_stop" -> {
                                    sawFinalChunk = true
                                    trySend(AuraResult.Success(GenerationChunk(delta = "", isFinal = true)))
                                }
                            }
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
                            if (!sawFinalChunk) trySend(AuraResult.Success(GenerationChunk(delta = "", isFinal = true)))
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
            val systemText =
                buildString {
                    request.systemPrompt?.let { append(it) }
                    request.messages.filter { it.role == MessageRole.System }.forEach {
                        if (isNotEmpty()) append("\n\n")
                        append(it.content)
                    }
                }
            val body =
                buildJsonObject {
                    put("model", CLAUDE_MODEL)
                    put("max_tokens", request.maxOutputTokens ?: 4096)
                    put("temperature", request.temperature.toDouble())
                    if (systemText.isNotBlank()) put("system", systemText)
                    put("stream", stream)
                    put(
                        "messages",
                        buildJsonArray {
                            request.messages.filter { it.role != MessageRole.System }.forEach { add(it.toClaudeMessage()) }
                        },
                    )
                    if (request.availableTools.isNotEmpty()) {
                        put(
                            "tools",
                            buildJsonArray {
                                request.availableTools.forEach { tool ->
                                    add(
                                        buildJsonObject {
                                            put("name", tool.name)
                                            put("description", tool.description)
                                            put("input_schema", tool.toJsonSchemaParameters())
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
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", CLAUDE_API_VERSION)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
        }

        private fun AiMessage.toClaudeMessage(): JsonObject =
            when (role) {
                MessageRole.Tool ->
                    buildJsonObject {
                        put("role", "user")
                        put(
                            "content",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("type", "tool_result")
                                        put("tool_use_id", toolCallId.orEmpty())
                                        put("content", content)
                                    },
                                )
                            },
                        )
                    }
                MessageRole.Assistant ->
                    buildJsonObject {
                        put("role", "assistant")
                        put("content", content)
                    }
                else ->
                    buildJsonObject {
                        put("role", "user")
                        if (imageBase64 != null) {
                            put(
                                "content",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("type", "image")
                                            put(
                                                "source",
                                                buildJsonObject {
                                                    put("type", "base64")
                                                    put("media_type", imageMimeType ?: "image/jpeg")
                                                    put("data", imageBase64)
                                                },
                                            )
                                        },
                                    )
                                    add(
                                        buildJsonObject {
                                            put("type", "text")
                                            put("text", content)
                                        },
                                    )
                                },
                            )
                        } else {
                            put("content", content)
                        }
                    }
            }

        private fun parseGenerateResponse(bodyText: String): GenerationResponse {
            val root = providerJson.parseToJsonElement(bodyText).jsonObject
            val blocks = root["content"]?.jsonArray.orEmpty()
            val text =
                blocks.joinToString("") { block ->
                    block.jsonObject
                        .takeIf { it["type"]?.jsonPrimitive?.content == "text" }
                        ?.get("text")
                        ?.jsonPrimitive
                        ?.content
                        .orEmpty()
                }
            val toolCalls =
                blocks.mapNotNull { block ->
                    val obj = block.jsonObject
                    if (obj["type"]?.jsonPrimitive?.content != "tool_use") return@mapNotNull null
                    ToolCallRequest(
                        id = obj["id"]?.jsonPrimitive?.content.orEmpty(),
                        toolName = obj["name"]?.jsonPrimitive?.content.orEmpty(),
                        arguments = obj["input"]?.jsonObject?.toStringArgumentMap().orEmpty(),
                    )
                }
            val stopReason = root["stop_reason"]?.jsonPrimitive?.content
            val usage = root["usage"]?.jsonObject
            return GenerationResponse(
                text = text,
                toolCalls = toolCalls,
                finishReason =
                    when (stopReason) {
                        "tool_use" -> FinishReason.ToolCall
                        "max_tokens" -> FinishReason.Length
                        "end_turn", "stop_sequence" -> FinishReason.Stop
                        else -> FinishReason.Stop
                    },
                usage =
                    usage?.let {
                        TokenUsage(
                            promptTokens = it["input_tokens"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                            completionTokens = it["output_tokens"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        )
                    },
            )
        }

        private fun retryAfterHeaderMillis(response: okhttp3.Response): Long? = response.header("retry-after")?.toLongOrNull()?.times(1000)
    }
