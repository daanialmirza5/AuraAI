package com.aura.ai.core.plugin.storage

import com.aura.ai.plugin.api.PluginStorage
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one process-wide, namespaced-by-plugin-id store behind every plugin's
 * [com.aura.ai.plugin.api.PluginContext.storage] — in-memory only this phase, same honesty as
 * every other "not yet persisted to disk" store in this codebase
 * (`core-memory.InMemoryLongTermMemoryStore` and friends). [scopedTo] hands back a
 * [PluginStorage] view that can only ever see [pluginId]'s own keys — a plugin can never read or
 * overwrite another plugin's data, even by guessing its plugin id, since the view has no
 * parameter through which to name a different one.
 */
@Singleton
class PluginStorageHost
    @Inject
    constructor() {
        private val byPlugin = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

        fun scopedTo(pluginId: String): PluginStorage =
            object : PluginStorage {
                private fun bucket() = byPlugin.computeIfAbsent(pluginId) { ConcurrentHashMap() }

                override suspend fun get(key: String): String? = bucket()[key]

                override suspend fun put(
                    key: String,
                    value: String,
                ) {
                    bucket()[key] = value
                }

                override suspend fun remove(key: String) {
                    bucket().remove(key)
                }

                override suspend fun keys(): Set<String> = bucket().keys.toSet()

                override suspend fun clear() {
                    bucket().clear()
                }
            }
    }
