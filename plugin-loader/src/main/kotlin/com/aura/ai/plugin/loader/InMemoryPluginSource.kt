package com.aura.ai.plugin.loader

import com.aura.ai.plugin.api.Plugin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryPluginSource
    @Inject
    constructor(
        private val plugins: Set<@JvmSuppressWildcards Plugin>,
    ) : PluginSource {
        override fun discover(): List<Plugin> = plugins.toList()
    }
