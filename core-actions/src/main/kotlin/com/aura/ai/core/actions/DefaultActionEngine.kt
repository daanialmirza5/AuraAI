package com.aura.ai.core.actions

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.tools.ToolRegistry
import com.aura.ai.core.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultActionEngine
    @Inject
    constructor(
        private val toolRegistry: ToolRegistry,
    ) : ActionEngine {
        override suspend fun execute(
            toolName: String,
            arguments: Map<String, String>,
        ): AuraResult<ToolResult> {
            val tool =
                toolRegistry.get(toolName)
                    ?: return AuraResult.Failure(AuraError.NotSupported("No tool registered for '$toolName'."))
            if (tool.requiresConfirmation) {
                return AuraResult.Failure(
                    AuraError.NotSupported(
                        "'$toolName' requires explicit user confirmation and cannot run autonomously — run it directly from the Automate tab instead.",
                    ),
                )
            }
            return tool.execute(arguments)
        }

        override suspend fun executeConfirmed(
            toolName: String,
            arguments: Map<String, String>,
        ): AuraResult<ToolResult> {
            val tool =
                toolRegistry.get(toolName)
                    ?: return AuraResult.Failure(AuraError.NotSupported("No tool registered for '$toolName'."))
            return tool.execute(arguments)
        }
    }
