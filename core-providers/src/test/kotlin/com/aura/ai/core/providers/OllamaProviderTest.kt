package com.aura.ai.core.providers

import com.aura.ai.core.ai.AiMessage
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.MessageRole
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OllamaProviderTest {
    private val request = GenerationRequest(messages = listOf(AiMessage(MessageRole.User, "Hi there")))

    private fun providerFor(
        server: MockWebServer,
        credentialStore: FakeProviderCredentialStore,
    ) = OllamaProvider(
        httpClient = OkHttpClient(),
        credentialStore = credentialStore,
        ioDispatcher = kotlinx.coroutines.Dispatchers.Default,
    )

    @Test
    fun `generate uses the configured base-url override, not the loopback default`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                val credentialStore = FakeProviderCredentialStore()
                credentialStore.setBaseUrlOverride(ProviderId.Ollama, server.url("").toString().removeSuffix("/"))
                val provider = providerFor(server, credentialStore)
                server.enqueue(
                    MockResponse()
                        .setBody(
                            """{"message": {"role": "assistant", "content": "Hello there!"}, "eval_count": 5, "prompt_eval_count": 10}""",
                        ).setResponseCode(200),
                )

                val result = provider.generate(request)

                val response = (result as AuraResult.Success).value
                assertEquals("Hello there!", response.text)
                val recorded = server.takeRequest()
                assertEquals("/api/chat", recorded.path)
            } finally {
                server.shutdown()
            }
        }

    @Test
    fun `generate maps HTTP errors the same way as the cloud providers`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                val credentialStore = FakeProviderCredentialStore()
                credentialStore.setBaseUrlOverride(ProviderId.Ollama, server.url("").toString().removeSuffix("/"))
                val provider = providerFor(server, credentialStore)
                server.enqueue(MockResponse().setResponseCode(500).setBody("model not found"))

                val result = provider.generate(request)

                assertTrue((result as AuraResult.Failure).error is AuraError.ProviderUnavailable)
            } finally {
                server.shutdown()
            }
        }

    @Test
    fun `isAvailable is true when the configured server responds`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                val credentialStore = FakeProviderCredentialStore()
                credentialStore.setBaseUrlOverride(ProviderId.Ollama, server.url("").toString().removeSuffix("/"))
                val provider = providerFor(server, credentialStore)
                server.enqueue(MockResponse().setBody("""{"models": []}""").setResponseCode(200))

                assertTrue(provider.isAvailable())
            } finally {
                server.shutdown()
            }
        }

    @Test
    fun `isAvailable is false when nothing is listening, not a thrown exception`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            val credentialStore = FakeProviderCredentialStore()
            credentialStore.setBaseUrlOverride(ProviderId.Ollama, server.url("").toString().removeSuffix("/"))
            val provider = providerFor(server, credentialStore)
            server.shutdown()

            assertFalse(provider.isAvailable())
        }
}
