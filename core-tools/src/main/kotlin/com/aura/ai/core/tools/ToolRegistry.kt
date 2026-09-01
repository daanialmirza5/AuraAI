package com.aura.ai.core.tools

import com.aura.ai.core.ai.ToolDescriptor

/**
 * The single, app-wide catalog of every [Tool] currently available — populated at startup by
 * core-actions' built-in tools and by any app-specific tools the app layer chooses to register.
 * core-planner reads [descriptors] to know what it can plan steps around; a future
 * provider-facing orchestrator reads it to advertise tool-calling to a model that supports it.
 */
interface ToolRegistry {
    fun register(tool: Tool)

    fun unregister(name: String)

    fun get(name: String): Tool?

    fun all(): List<Tool>

    fun descriptors(): List<ToolDescriptor>
}
