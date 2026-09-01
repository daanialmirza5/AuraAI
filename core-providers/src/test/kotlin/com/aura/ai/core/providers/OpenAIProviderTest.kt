package com.aura.ai.core.providers

import com.aura.ai.core.ai.AiMessage
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.FinishReason
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.MessageRole
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenAIProviderTest {
    private lateinit var server: MockWebServer
    private lateinit var credentialStore: FakeProviderCredentialStore
    private lateinit var provider: OpenAIProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        credentialStore = FakeProviderCredentialStore()
        provider =
            OpenAIProvider(
                httpClient = OkHttpClient(),
                credentialStore = credentialStore,
                ioDispatcher = kotlinx.coroutines.Dispatchers.Default,
            ).apply { apiBaseUrl = server.url("/v1/chat/completions").toString() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val request = GenerationRequest(messages = listOf(AiMessage(MessageRole.User, "Hi there")))

    @Test
    fun `generate fails honestly when no api key is configured`() =
        runBlocking {
            val result = provider.generate(request)
            assertTrue((result as AuraResult.Failure).error is AuraError.ProviderNotConnected)
        }

    @Test
    fun `generate parses a successful text response`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.OpenAI, "sk-test")
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {
                          "choices": [{"message": {"role": "assistant", "content": "Hello there!"}, "finish_reason": "stop"}],
                          "usage": {"prompt_tokens": 10, "completion_tokens": 5}
                        }
                        """.trimIndent(),
                    ).setResponseCode(200),
            )

            val result = provider.generate(request)

            val response = (result as AuraResult.Success).value
            assertEquals("Hello there!", response.text)
            assertEquals(FinishReason.Stop, response.finishReason)
            assertEquals(10, response.usage?.promptTokens)
        }

    @Test
    fun `generate sends a bearer authorization header`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.OpenAI, "sk-test")
            server.enqueue(MockResponse().setBody("""{"choices": []}""").setResponseCode(200))

            provider.generate(request)

            val recorded = server.takeRequest()
            assertEquals("Bearer sk-test", recorded.getHeader("Authorization"))
        }

    @Test
    fun `generate maps HTTP 401 to Authentication`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.OpenAI, "sk-bad")
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error": "invalid api key"}"""))

            val result = provider.generate(request)

            assertTrue((result as AuraResult.Failure).error is AuraError.Authentication)
        }

    @Test
    fun `generate maps HTTP 429 to RateLimited`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.OpenAI, "sk-test")
            server.enqueue(MockResponse().setResponseCode(429).setBody("""{"error": "rate limited"}"""))

            val result = provider.generate(request)

            assertTrue((result as AuraResult.Failure).error is AuraError.RateLimited)
        }

    @Test
    fun `generate maps tool calls with malformed arguments to an empty map instead of crashing`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.OpenAI, "sk-test")
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {
                          "choices": [{
                            "message": {
                              "role": "assistant",
                              "tool_calls": [{"id": "call_1", "function": {"name": "search", "arguments": "not-json"}}]
                            },
                            "finish_reason": "tool_calls"
                          }]
                        }
                        """.trimIndent(),
                    ).setResponseCode(200),
            )

            val result = provider.generate(request)

            val response = (result as AuraResult.Success).value
            assertEquals(FinishReason.ToolCall, response.finishReason)
            assertEquals("search", response.toolCalls.single().toolName)
            assertTrue(
                response.toolCalls
                    .single()
                    .arguments
                    .isEmpty(),
            )
        }
}
