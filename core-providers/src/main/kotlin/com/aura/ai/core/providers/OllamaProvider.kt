package com.aura.ai.core.providers

import com.aura.ai.core.ai.AIProvider
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.FinishReason
import com.aura.ai.core.ai.GenerationChunk
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.GenerationResponse
import com.aura.ai.core.ai.MessageRole
import com.aura.ai.core.ai.ProviderCapabilities
import com.aura.ai.core.ai.TokenUsage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val OLLAMA_DEFAULT_BASE_URL = "http://localhost:11434"
private const val OLLAMA_MODEL = "llama3.2"

/**
 * The real Ollama integration — a self-hosted server, so there is no API key at all, only a base
 * URL (configurable in Settings; defaults to the emulator/local-machine loopback address). Ollama
 * streams newline-delimited JSON, not Server-Sent Events, which is why this provider reads the
 * response body as raw lines instead of using [okhttp3.sse.EventSource] like the three cloud
 * providers. See [docs/AI_PROVIDER_INTEGRATION.md] for the "`localhost` means the phone itself,
 * not your computer" caveat this provider's base-url override exists to solve.
 */
@Singleton
class OllamaProvider
    @Inject
    constructor(
        private val httpClient: OkHttpClient,
        private val credentialStore: ProviderCredentialStore,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : AIProvider {
        override val id: String = ProviderId.Ollama.key
        override val displayName: String = "Ollama (local)"
        override val capabilities: ProviderCapabilities =
            ProviderCapabilities(
                supportsStreaming = true,
                supportsTools = false,
                supportsVision = false,
                isLocal = true,
                maxContextTokens = 8_192,
            )

        override suspend fun isAvailable(): Boolean =
            withContext(ioDispatcher) {
                try {
                    val probe =
                        httpClient
                            .newBuilder()
                            .callTimeout(1500, TimeUnit.MILLISECONDS)
                            .build()
                    val request =
                        Request
                            .Builder()
                            .url("${baseUrl()}/api/tags")
                            .get()
                            .build()
                    probe.newCall(request).execute().use { it.isSuccessful }
                } catch (e: IOException) {
                    false
                }
            }

        override suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse> =
            withContext(ioDispatcher) {
                try {
                    val httpRequest = buildRequest(request, stream = false)
                    httpClient.newCall(httpRequest).execute().use { response ->
                        val bodyText = response.body?.string()
                        if (!response.isSuccessful) {
                            AuraResult.Failure(ProviderHttpErrors.fromStatus(displayName, response.code, null, bodyText))
                        } else {
                            AuraResult.Success(parseGenerateResponse(bodyText.orEmpty()))
                        }
                    }
                } catch (e: IOException) {
                    AuraResult.Failure(ProviderHttpErrors.fromException(displayName, e))
                }
            }

        override fun generateStream(request: GenerationRequest): Flow<AuraResult<GenerationChunk>> =
            flow {
                val httpRequest = buildRequest(request, stream = true)
                try {
                    httpClient.newCall(httpRequest).execute().use { response ->
                        if (!response.isSuccessful) {
                            emit(
                                AuraResult.Failure(
                                    ProviderHttpErrors.fromStatus(displayName, response.code, null, response.body?.string()),
                                ),
                            )
                            return@use
                        }
                        val source = response.body?.source() ?: return@use
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.isBlank()) continue
                            val obj = providerJson.parseToJsonElement(line).jsonObject
                            val delta =
                                obj["message"]
                                    ?.jsonObject
                                    ?.get("content")
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                    .orEmpty()
                            val done = obj["done"]?.jsonPrimitive?.contentOrNull.toBoolean()
                            if (delta.isNotEmpty()) emit(AuraResult.Success(GenerationChunk(delta = delta)))
                            if (done) {
                                emit(AuraResult.Success(GenerationChunk(delta = "", isFinal = true)))
                                break
                            }
                        }
                    }
                } catch (e: IOException) {
                    emit(AuraResult.Failure(ProviderHttpErrors.fromException(displayName, e)))
                }
            }.flowOn(ioDispatcher)

        private suspend fun baseUrl(): String = credentialStore.baseUrlOverride(ProviderId.Ollama) ?: OLLAMA_DEFAULT_BASE_URL

        private suspend fun buildRequest(
            request: GenerationRequest,
            stream: Boolean,
        ): Request {
            val body =
                buildJsonObject {
                    put("model", OLLAMA_MODEL)
                    put("stream", stream)
                    put("options", buildJsonObject { put("temperature", request.temperature.toDouble()) })
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
                            request.messages.forEach {
                                add(
                                    buildJsonObject {
                                        put("role", it.role.toOllamaRoleName())
                                        put("content", it.content)
                                    },
                                )
                            }
                        },
                    )
                }
            return Request
                .Builder()
                .url("${baseUrl()}/api/chat")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
        }

        private fun MessageRole.toOllamaRoleName(): String =
            when (this) {
                MessageRole.System -> "system"
                MessageRole.User -> "user"
                MessageRole.Assistant -> "assistant"
                MessageRole.Tool -> "tool"
            }

        private fun parseGenerateResponse(bodyText: String): GenerationResponse {
            val root = providerJson.parseToJsonElement(bodyText).jsonObject
            val text =
                root["message"]
                    ?.jsonObject
                    ?.get("content")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    .orEmpty()
            return GenerationResponse(
                text = text,
                finishReason = FinishReason.Stop,
                usage =
                    TokenUsage(
                        promptTokens = root["prompt_eval_count"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                        completionTokens = root["eval_count"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                    ),
            )
        }
    }

private fun String?.toBoolean(): Boolean = this == "true"
