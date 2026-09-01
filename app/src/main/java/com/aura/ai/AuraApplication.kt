package com.aura.ai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.registry.AgentRegistry
import com.aura.ai.core.crash.LocalCrashLogger
import com.aura.ai.core.di.ApplicationScope
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolRegistry
import com.aura.ai.data.local.DatabaseSeeder
import com.aura.ai.plugin.loader.PluginLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AuraApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var databaseSeeder: DatabaseSeeder

    @Inject lateinit var toolRegistry: ToolRegistry

    @Inject lateinit var tools: Set<@JvmSuppressWildcards Tool>

    @Inject lateinit var agentRegistry: AgentRegistry

    @Inject lateinit var agents: Set<@JvmSuppressWildcards Agent>

    @Inject lateinit var pluginLoader: PluginLoader

    // DeferredActionWorker (core-actions) is @HiltWorker-injected, e.g. it takes a Dagger-provided
    // ActionEngine rather than constructing one — WorkManager needs this factory instead of its
    // own default one to know how to build it. The default WorkManagerInitializer is removed in
    // the manifest specifically so this Configuration is the one actually used, not raced against.
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        LocalCrashLogger.install(this)
        applicationScope.launch { databaseSeeder.seedIfNeeded() }
        tools.forEach(toolRegistry::register)
        agents.forEach(agentRegistry::register)
        applicationScope.launch { pluginLoader.loadAll() }
    }
}
