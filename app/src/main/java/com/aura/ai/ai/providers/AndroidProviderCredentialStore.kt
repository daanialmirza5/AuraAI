package com.aura.ai.ai.providers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aura.ai.core.providers.ProviderCredentialStore
import com.aura.ai.core.providers.ProviderId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_FILE_NAME = "aura_provider_credentials"

/**
 * The real implementation of [ProviderCredentialStore] — API keys never touch plaintext storage
 * (unlike [com.aura.ai.data.repository.PreferencesRepositoryImpl], which is correct for
 * non-secret UI prefs but would be the wrong choice here). Backed by Jetpack Security's
 * `EncryptedSharedPreferences`, itself backed by an Android Keystore-generated key — the key
 * material never leaves secure hardware where the device supports it.
 */
@Singleton
class AndroidProviderCredentialStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ProviderCredentialStore {
        private val prefs: SharedPreferences by lazy {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        override suspend fun apiKey(id: ProviderId): String? =
            withContext(Dispatchers.IO) {
                prefs.getString(apiKeyPrefKey(id), null)
            }

        override suspend fun setApiKey(
            id: ProviderId,
            apiKey: String?,
        ) = withContext(Dispatchers.IO) {
            prefs.edit { if (apiKey == null) remove(apiKeyPrefKey(id)) else putString(apiKeyPrefKey(id), apiKey) }
        }

        override suspend fun baseUrlOverride(id: ProviderId): String? =
            withContext(Dispatchers.IO) {
                prefs.getString(baseUrlPrefKey(id), null)
            }

        override suspend fun setBaseUrlOverride(
            id: ProviderId,
            baseUrl: String?,
        ) = withContext(Dispatchers.IO) {
            prefs.edit { if (baseUrl == null) remove(baseUrlPrefKey(id)) else putString(baseUrlPrefKey(id), baseUrl) }
        }

        private fun apiKeyPrefKey(id: ProviderId) = "api_key_${id.key}"

        private fun baseUrlPrefKey(id: ProviderId) = "base_url_${id.key}"
    }
