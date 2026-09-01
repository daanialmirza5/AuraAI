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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClaudeProviderTest {
    private lateinit var server: MockWebServer
    private lateinit var credentialStore: FakeProviderCredentialStore
    private lateinit var provider: ClaudeProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        credentialStore = FakeProviderCredentialStore()
        provider =
            ClaudeProvider(
                httpClient = OkHttpClient(),
                credentialStore = credentialStore,
                ioDispatcher = kotlinx.coroutines.Dispatchers.Default,
            ).apply { apiBaseUrl = server.url("/v1/messages").toString() }
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
            assertTrue(result is AuraResult.Failure)
            assertTrue((result as AuraResult.Failure).error is AuraError.ProviderNotConnected)
        }

    @Test
    fun `isAvailable reflects whether a key is configured`() =
        runBlocking {
            assertTrue(!provider.isAvailable())
            credentialStore.setApiKey(ProviderId.Claude, "sk-ant-test")
            assertTrue(provider.isAvailable())
        }

    @Test
    fun `generate parses a successful text response`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.Claude, "sk-ant-test")
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {
                          "content": [{"type": "text", "text": "Hello there!"}],
                          "stop_reason": "end_turn",
                          "usage": {"input_tokens": 10, "output_tokens": 5}
                        }
                        """.trimIndent(),
                    ).setResponseCode(200),
            )

            val result = provider.generate(request)

            assertTrue(result is AuraResult.Success)
            val response = (result as AuraResult.Success).value
            assertEquals("Hello there!", response.text)
            assertEquals(FinishReason.Stop, response.finishReason)
            assertEquals(10, response.usage?.promptTokens)
            assertEquals(5, response.usage?.completionTokens)
        }

    @Test
    fun `generate sends the api key and anthropic version headers`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.Claude, "sk-ant-test")
            server.enqueue(MockResponse().setBody("""{"content": []}""").setResponseCode(200))

            provider.generate(request)

            val recorded = server.takeRequest()
            assertEquals("sk-ant-test", recorded.getHeader("x-api-key"))
            assertNull(recorded.getHeader("Authorization"))
            assertTrue(recorded.getHeader("anthropic-version")!!.isNotBlank())
            assertTrue(recorded.body.readUtf8().contains("\"role\":\"user\""))
        }

    @Test
    fun `generate maps HTTP 401 to Authentication`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.Claude, "sk-ant-bad")
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error": "invalid api key"}"""))

            val result = provider.generate(request)

            assertTrue(result is AuraResult.Failure)
            assertTrue((result as AuraResult.Failure).error is AuraError.Authentication)
        }

    @Test
    fun `generate maps HTTP 429 to RateLimited with retry-after`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.Claude, "sk-ant-test")
            server.enqueue(
                MockResponse().setResponseCode(429).setHeader("retry-after", "5").setBody("""{"error": "rate limited"}"""),
            )

            val result = provider.generate(request)

            assertTrue(result is AuraResult.Failure)
            val error = (result as AuraResult.Failure).error
            assertTrue(error is AuraError.RateLimited)
            assertEquals(5000L, (error as AuraError.RateLimited).retryAfterMillis)
        }

    @Test
    fun `generate maps a 500 to ProviderUnavailable`() =
        runBlocking {
            credentialStore.setApiKey(ProviderId.Claude, "sk-ant-test")
            server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))

            val result = provider.generate(request)

            assertTrue(result is AuraResult.Failure)
            assertTrue((result as AuraResult.Failure).error is AuraError.ProviderUnavailable)
        }
}
