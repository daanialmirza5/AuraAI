package com.aura.ai.ai.di

import android.util.Log
import com.aura.ai.BuildConfig
import com.aura.ai.core.plugin.compatibility.DefaultPluginCompatibilityChecker
import com.aura.ai.core.plugin.compatibility.PluginCompatibilityChecker
import com.aura.ai.core.plugin.context.PluginLogSink
import com.aura.ai.core.plugin.health.DefaultPluginHealthMonitor
import com.aura.ai.core.plugin.health.PluginHealthMonitor
import com.aura.ai.core.plugin.permission.DefaultPluginPermissionPolicy
import com.aura.ai.core.plugin.permission.PluginPermissionPolicy
import com.aura.ai.core.plugin.registry.DefaultPluginRegistry
import com.aura.ai.core.plugin.registry.PluginRegistry
import com.aura.ai.plugin.api.Plugin
import com.aura.ai.plugin.loader.DefaultPluginLoader
import com.aura.ai.plugin.loader.InMemoryPluginSource
import com.aura.ai.plugin.loader.PluginLoader
import com.aura.ai.plugin.loader.PluginSource
import com.aura.ai.plugin.marketplace.LocalPluginMarketplaceClient
import com.aura.ai.plugin.marketplace.PluginMarketplaceClient
import com.aura.ai.plugin.runtime.DefaultPluginRuntime
import com.aura.ai.plugin.runtime.DefaultPluginUpdateManager
import com.aura.ai.plugin.runtime.PluginRuntime
import com.aura.ai.plugin.runtime.PluginUpdateManager
import com.aura.ai.plugins.DiceRollerPlugin
import com.aura.ai.plugins.WordCounterPlugin
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Phase 7 bindings — the plugin SDK's host-side wiring, plus the two example plugins
 * ([DiceRollerPlugin], [WordCounterPlugin]) that prove it actually works. `Set<Plugin>` and
 * `Set<PluginSource>` follow the exact same multibinding pattern [AiToolsModule]/[AiAgentsModule]
 * already established for `Tool`/`Agent`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiPluginModule {
    @Binds
    @Singleton
    abstract fun bindPluginCompatibilityChecker(impl: DefaultPluginCompatibilityChecker): PluginCompatibilityChecker

    @Binds
    @Singleton
    abstract fun bindPluginPermissionPolicy(impl: DefaultPluginPermissionPolicy): PluginPermissionPolicy

    @Binds
    @Singleton
    abstract fun bindPluginRegistry(impl: DefaultPluginRegistry): PluginRegistry

    @Binds
    @Singleton
    abstract fun bindPluginHealthMonitor(impl: DefaultPluginHealthMonitor): PluginHealthMonitor

    @Binds
    @Singleton
    abstract fun bindPluginRuntime(impl: DefaultPluginRuntime): PluginRuntime

    @Binds
    @Singleton
    abstract fun bindPluginUpdateManager(impl: DefaultPluginUpdateManager): PluginUpdateManager

    @Binds
    @Singleton
    abstract fun bindPluginLoader(impl: DefaultPluginLoader): PluginLoader

    @Binds
    @Singleton
    abstract fun bindPluginMarketplaceClient(impl: LocalPluginMarketplaceClient): PluginMarketplaceClient

    @Binds
    @IntoSet
    abstract fun bindInMemoryPluginSource(impl: InMemoryPluginSource): PluginSource

    @Binds
    @IntoSet
    abstract fun bindDiceRollerPlugin(impl: DiceRollerPlugin): Plugin

    @Binds
    @IntoSet
    abstract fun bindWordCounterPlugin(impl: WordCounterPlugin): Plugin

    companion object {
        /** A real no-op in release builds, not just a filtered log level — no plugin-authored
         *  string ever reaches the release APK's logcat at all. See `docs/SECURITY.md` §3. */
        @Provides
        @Singleton
        @PluginLogSink
        fun providePluginLogSink(): (pluginId: String, message: String) -> Unit =
            if (BuildConfig.DEBUG) {
                { pluginId, message -> Log.d("Plugin:$pluginId", message) }
            } else {
                { _, _ -> }
            }
    }
}
