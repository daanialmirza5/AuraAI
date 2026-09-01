package com.aura.ai.core.tools

import com.aura.ai.core.ai.ToolDescriptor
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultToolRegistry
    @Inject
    constructor() : ToolRegistry {
        private val tools = ConcurrentHashMap<String, Tool>()

        override fun register(tool: Tool) {
            tools[tool.name] = tool
        }

        override fun unregister(name: String) {
            tools.remove(name)
        }

        override fun get(name: String): Tool? = tools[name]

        override fun all(): List<Tool> = tools.values.toList()

        override fun descriptors(): List<ToolDescriptor> = tools.values.map { it.toDescriptor() }
    }
