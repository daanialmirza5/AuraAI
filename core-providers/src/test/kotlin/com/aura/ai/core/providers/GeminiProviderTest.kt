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

class GeminiProviderTest {
    private lateinit var server: MockWebServer
    private lateinit var credentialStore: FakeProviderCredentialStore
    private lateinit var provider: GeminiProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        credentialStore = FakeProviderCredentialStore()
        provider =
            GeminiProvider(
                httpClient = OkHttpClient(),
                credentialStore = credentialStore,
                ioDispatcher = kotlinx.coroutines.Dispatchers.Default,
            ).apply { apiBaseUrl = server.url("/v1beta/models").toString().removeSuffix("/") }
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
            credentialStore.setApiKey(ProviderId.Gemini, "test-key")
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {
                          "candidates": [{"content": {"parts": [{"text": "Hello there!"}]}, "finishReason": "STOP"}],
                          "usageMetadata": {"promptTokenCount": 10, "candidatesTokenCount": 5}
                        }
                        """.trimIndent(),
                    ).setResponseCode(200),
            )

            val result = provider.generate(request)

            val response = (result as AuraResult.Success).value
            assertEquals("Hello there!", response.text)
            assertEquals(FinishReason.Stop, response.finishReason)
            assertEquals(5, response.usage?.completionTokens)
        }

    @Test
    fun `generate authenticates via a key query parameter, not a header`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.Gemini, "test-key")
            server.enqueue(MockResponse().setBody("""{"candidates": []}""").setResponseCode(200))

            provider.generate(request)

            val recorded = server.takeRequest()
            assertTrue(recorded.path!!.contains("key=test-key"))
            assertEquals(null, recorded.getHeader("Authorization"))
        }

    @Test
    fun `generate maps a safety finish reason to ContentFilter`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.Gemini, "test-key")
            server.enqueue(
                MockResponse()
                    .setBody(
                        """{"candidates": [{"content": {"parts": []}, "finishReason": "SAFETY"}]}""",
                    ).setResponseCode(200),
            )

            val result = provider.generate(request)

            assertEquals(FinishReason.ContentFilter, (result as AuraResult.Success).value.finishReason)
        }

    @Test
    fun `generate maps HTTP 429 to RateLimited`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.Gemini, "test-key")
            server.enqueue(MockResponse().setResponseCode(429).setBody("""{"error": "rate limited"}"""))

            val result = provider.generate(request)

            assertTrue((result as AuraResult.Failure).error is AuraError.RateLimited)
        }
}
