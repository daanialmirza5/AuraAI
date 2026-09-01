package com.aura.ai.core.reasoning.capability

import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeToolRegistry(
    private val toolDescriptors: List<ToolDescriptor>,
) : ToolRegistry {
    override fun register(tool: Tool) = error("not needed for this test")

    override fun unregister(name: String) = error("not needed for this test")

    override fun get(name: String): Tool? = error("not needed for this test")

    override fun all(): List<Tool> = error("not needed for this test")

    override fun descriptors(): List<ToolDescriptor> = toolDescriptors
}

private fun descriptor(name: String) = ToolDescriptor(name = name, description = "")

class DefaultCapabilityResolverTest {
    @Test
    fun `availableTools delegates straight through to the tool registry's descriptors`() {
        val descriptors = listOf(descriptor("open_app"), descriptor("notify"))
        val resolver = DefaultCapabilityResolver(FakeToolRegistry(descriptors))

        assertEquals(descriptors, resolver.availableTools())
    }

    @Test
    fun `isAvailable is true only for a tool actually present in the registry`() {
        val resolver = DefaultCapabilityResolver(FakeToolRegistry(listOf(descriptor("open_app"))))

        assertTrue(resolver.isAvailable("open_app"))
        assertFalse(resolver.isAvailable("nonexistent_tool"))
    }

    @Test
    fun `notify is the one tool documented and modeled as needing a runtime permission`() {
        val resolver = DefaultCapabilityResolver(FakeToolRegistry(emptyList()))

        assertEquals(listOf("android.permission.POST_NOTIFICATIONS"), resolver.requiredPermissions("notify"))
        assertTrue(resolver.requiredPermissions("open_app").isEmpty())
    }

    @Test
    fun `web search, shopping search and navigate are the only tools requiring network`() {
        val resolver = DefaultCapabilityResolver(FakeToolRegistry(emptyList()))

        assertTrue(resolver.requiresNetwork("web_search"))
        assertTrue(resolver.requiresNetwork("shopping_search"))
        assertTrue(resolver.requiresNetwork("navigate"))
        assertFalse(resolver.requiresNetwork("open_app"))
    }

    @Test
    fun `an unknown tool name is assumed to need nothing, the documented safe default`() {
        val resolver = DefaultCapabilityResolver(FakeToolRegistry(emptyList()))

        assertTrue(resolver.requiredPermissions("some_future_tool").isEmpty())
        assertFalse(resolver.requiresNetwork("some_future_tool"))
    }
}
