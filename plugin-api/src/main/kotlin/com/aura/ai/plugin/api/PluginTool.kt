package com.aura.ai.plugin.api

/** JSON-Schema-ish primitive types, mirroring `core-ai.ParameterType`'s vocabulary but declared
 *  independently — see `PluginResult`'s doc for why plugin-api never imports a core module, even
 *  for a shape this simple. */
enum class PluginParameterType { String, Number, Boolean, Object, Array }

data class PluginParameterSchema(
    val type: PluginParameterType,
    val description: String,
    val required: Boolean = false,
)

/** What a plugin's tool handler hands back — the plugin-facing analogue of `core-tools.ToolResult`. */
data class PluginToolResult(
    val summary: String,
    val data: Map<String, String> = emptyMap(),
)

/** What a plugin declares about one tool it offers. [handler] receives plain string arguments
 *  (the same convention `core-tools.Tool.execute` uses) and returns a [PluginResult] — a plugin's
 *  tool can fail exactly the way any other part of the SDK can. */
data class PluginToolDescriptor(
    val name: String,
    val description: String,
    val parameters: Map<String, PluginParameterSchema> = emptyMap(),
    val handler: suspend (Map<String, String>) -> PluginResult<PluginToolResult>,
)

/**
 * "Dynamic Tool Registration" — the plugin-facing half. `core-plugin`'s real implementation
 * wraps each [register]ed [PluginToolDescriptor] in an adapter implementing
 * `core-tools.Tool` and registers *that* into the host's real `core-tools.ToolRegistry`, so a
 * plugin's tool is genuinely invokable through the exact same path every built-in tool is —
 * `core-actions`' `ActionEngine`, `core-agents`' `ToolBackedAgent`, and
 * `core-orchestrator`'s dispatch all see it as an ordinary registered tool, unaware it came from
 * a plugin at all.
 */
interface PluginToolRegistrar {
    /** Fails with [PluginError.PermissionDenied] rather than silently no-op-ing if
     *  [PluginPermission.RegisterTools] wasn't granted — a plugin should always be able to tell
     *  whether its tool is actually live. */
    fun register(descriptor: PluginToolDescriptor): PluginResult<Unit>

    fun unregister(name: String): PluginResult<Unit>
}
