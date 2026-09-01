package com.aura.ai.core.providers

/** An in-memory [ProviderCredentialStore] test double — no Android, no encryption, just a map. */
class FakeProviderCredentialStore : ProviderCredentialStore {
    private val keys = mutableMapOf<ProviderId, String>()
    private val baseUrls = mutableMapOf<ProviderId, String>()

    override suspend fun apiKey(id: ProviderId): String? = keys[id]

    override suspend fun setApiKey(
        id: ProviderId,
        apiKey: String?,
    ) {
        if (apiKey == null) keys.remove(id) else keys[id] = apiKey
    }

    override suspend fun baseUrlOverride(id: ProviderId): String? = baseUrls[id]

    override suspend fun setBaseUrlOverride(
        id: ProviderId,
        baseUrl: String?,
    ) {
        if (baseUrl == null) baseUrls.remove(id) else baseUrls[id] = baseUrl
    }
}
