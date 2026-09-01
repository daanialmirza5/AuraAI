package com.aura.ai.core.providers

/**
 * Where a provider's API key (and, for a self-hosted provider like Ollama, its base URL) actually
 * comes from — never a source literal (see [ProviderConfig]). core-providers is pure Kotlin/JVM
 * and cannot read Android's encrypted storage directly, so this interface is the seam: the app
 * supplies a real, `EncryptedSharedPreferences`-backed implementation at DI time (see
 * `AndroidProviderCredentialStore` in the `:app` module), and every cloud provider here only ever
 * depends on this interface. Base URLs aren't secret, but they live alongside credentials here
 * rather than in a second abstraction, since both are "how do I reach this provider" concerns for
 * exactly the same 5 provider ids.
 */
interface ProviderCredentialStore {
    suspend fun apiKey(id: ProviderId): String?

    suspend fun setApiKey(
        id: ProviderId,
        apiKey: String?,
    )

    suspend fun baseUrlOverride(id: ProviderId): String? = null

    suspend fun setBaseUrlOverride(
        id: ProviderId,
        baseUrl: String?,
    ) {}
}

/** The honest default when no real credential store is wired — every key is absent, always. */
object NoOpProviderCredentialStore : ProviderCredentialStore {
    override suspend fun apiKey(id: ProviderId): String? = null

    override suspend fun setApiKey(
        id: ProviderId,
        apiKey: String?,
    ) = Unit
}
