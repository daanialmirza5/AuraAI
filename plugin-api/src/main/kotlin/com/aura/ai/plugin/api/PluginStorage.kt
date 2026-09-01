package com.aura.ai.plugin.api

/**
 * "Plugin Storage" — a plain, async key-value store scoped to exactly one plugin. A plugin never
 * sees another plugin's keys, and never sees the host's own Room database or DataStore directly —
 * `core-plugin`'s real implementation namespaces every call by plugin id underneath this
 * interface. Requires [PluginPermission.AccessStorage].
 */
interface PluginStorage {
    suspend fun get(key: String): String?

    suspend fun put(
        key: String,
        value: String,
    )

    suspend fun remove(key: String)

    suspend fun keys(): Set<String>

    suspend fun clear()
}
