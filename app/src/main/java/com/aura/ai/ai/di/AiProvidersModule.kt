package com.aura.ai.ai.di

import com.aura.ai.ai.providers.AndroidProviderCredentialStore
import com.aura.ai.core.ai.AIProvider
import com.aura.ai.core.providers.ClaudeProvider
import com.aura.ai.core.providers.GeminiProvider
import com.aura.ai.core.providers.LocalModelProvider
import com.aura.ai.core.providers.OllamaProvider
import com.aura.ai.core.providers.OpenAIProvider
import com.aura.ai.core.providers.ProviderCredentialStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Every supported model backend is bound into the same `Set<AIProvider>`, which is all
 * [com.aura.ai.core.providers.DefaultAIProviderManager] ever sees — it has no idea these are 5
 * distinct classes, let alone which ones. Adding a 6th provider (or removing one) is exactly one
 * `@Binds @IntoSet` method here; nothing in core-planner, core-intent, or the app's ViewModels
 * changes, which is the whole point of item 6 in the brief ("switch providers without changing
 * business logic"). Four of the five now make real network calls (see [OkHttpClient] below);
 * [LocalModelProvider] remains scaffolded on purpose — see docs/TODO_V1.md §3.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiProvidersModule {
    @Binds
    @IntoSet
    abstract fun bindGeminiProvider(impl: GeminiProvider): AIProvider

    @Binds
    @IntoSet
    abstract fun bindOpenAIProvider(impl: OpenAIProvider): AIProvider

    @Binds
    @IntoSet
    abstract fun bindClaudeProvider(impl: ClaudeProvider): AIProvider

    @Binds
    @IntoSet
    abstract fun bindOllamaProvider(impl: OllamaProvider): AIProvider

    @Binds
    @IntoSet
    abstract fun bindLocalModelProvider(impl: LocalModelProvider): AIProvider

    @Binds
    abstract fun bindProviderCredentialStore(impl: AndroidProviderCredentialStore): ProviderCredentialStore

    companion object {
        /** Shared by all four HTTP-backed providers — one connection pool, one set of timeouts.
         *  Generous read timeout since a non-streamed [com.aura.ai.core.ai.AIProvider.generate]
         *  call can legitimately take tens of seconds for a long completion. */
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient =
            OkHttpClient
                .Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
    }
}
