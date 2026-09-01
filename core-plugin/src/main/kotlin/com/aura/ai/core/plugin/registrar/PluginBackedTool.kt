package com.aura.ai.core.plugin.registrar

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import com.aura.ai.plugin.api.PluginParameterType
import com.aura.ai.plugin.api.PluginResult
import com.aura.ai.plugin.api.PluginToolDescriptor

/**
 * The adapter that makes "Dynamic Tool Registration" real: wraps one plugin's
 * [PluginToolDescriptor] as an ordinary `core-tools.Tool`, translating the plugin SDK's own
 * result/parameter vocabulary into the host's at exactly one seam. Once registered into
 * `core-tools.ToolRegistry`, nothing downstream — `core-actions.ActionEngine`,
 * `core-agents.ToolBackedAgent`, `core-orchestrator`'s dispatch — can tell this apart from a
 * built-in tool.
 */
class PluginBackedTool(
    private val descriptor: PluginToolDescriptor,
) : Tool {
    override val name: String = descriptor.name
    override val description: String = descriptor.description
    override val parameters: Map<String, ParameterSchema> =
        descriptor.parameters.mapValues { (_, schema) ->
            ParameterSchema(
                type = schema.type.toHostType(),
                description = schema.description,
                required = schema.required,
            )
        }

    override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> =
        when (val result = descriptor.handler(arguments)) {
            is PluginResult.Success -> AuraResult.Success(ToolResult(result.value.summary, result.value.data))
            is PluginResult.Failure -> AuraResult.Failure(AuraError.Unknown(result.error.message))
        }

    private fun PluginParameterType.toHostType(): ParameterType =
        when (this) {
            PluginParameterType.String -> ParameterType.String
            PluginParameterType.Number -> ParameterType.Number
            PluginParameterType.Boolean -> ParameterType.Boolean
            PluginParameterType.Object -> ParameterType.Object
            PluginParameterType.Array -> ParameterType.Array
        }
}
