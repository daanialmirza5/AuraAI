package com.aura.ai.core.reasoning.capability

import com.aura.ai.core.ai.ToolDescriptor

/**
 * "Which tools are available?" and "which permissions are missing?" both start here: this is
 * the one place that knows both what's currently registered ([availableTools], reading straight
 * from `com.aura.ai.core.tools.ToolRegistry`) and what each tool actually needs to run
 * ([requiredPermissions], [requiresNetwork]) to answer them.
 */
interface CapabilityResolver {
    fun availableTools(): List<ToolDescriptor>

    fun isAvailable(toolName: String): Boolean = availableTools().any { it.name == toolName }

    /** Android permission names [toolName] needs. Empty for a tool that needs none (true for
     *  every registered tool but one — see `DefaultCapabilityResolver`'s doc) or one that isn't
     *  registered at all. */
    fun requiredPermissions(toolName: String): List<String>

    /** The union of [requiredPermissions] across every name in [toolNames], deduplicated. */
    fun requiredPermissions(toolNames: Collection<String>): List<String> = toolNames.flatMap { requiredPermissions(it) }.distinct()

    /** Whether [toolName] needs live connectivity to do anything useful. */
    fun requiresNetwork(toolName: String): Boolean

    fun requiresNetwork(toolNames: Collection<String>): Boolean = toolNames.any { requiresNetwork(it) }
}
