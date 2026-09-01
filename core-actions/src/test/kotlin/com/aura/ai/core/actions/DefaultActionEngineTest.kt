package com.aura.ai.core.actions

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolRegistry
import com.aura.ai.core.tools.ToolResult
import com.aura.ai.core.tools.toDescriptor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/** A tool that only reports whether it was actually invoked — the assertion in every test here is
 *  "did [Tool.execute] run," not just "what did the engine return." */
private class RecordingTool(
    override val name: String,
    override val requiresConfirmation: Boolean = false,
) : Tool {
    override val description = "test tool"
    override val parameters: Map<String, ParameterSchema> = emptyMap()
    var invoked = false
        private set

    override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
        invoked = true
        return AuraResult.Success(ToolResult(summary = "ran $name"))
    }
}

private class FakeToolRegistry(
    vararg tools: Tool,
) : ToolRegistry {
    private val byName = tools.associateBy { it.name }.toMutableMap()

    override fun register(tool: Tool) {
        byName[tool.name] = tool
    }

    override fun unregister(name: String) {
        byName.remove(name)
    }

    override fun get(name: String): Tool? = byName[name]

    override fun all(): List<Tool> = byName.values.toList()

    override fun descriptors() = all().map { it.toDescriptor() }
}

class DefaultActionEngineTest {
    @Test
    fun `execute runs a tool that does not require confirmation`() =
        runBlocking {
            val tool = RecordingTool("open_app")
            val engine = DefaultActionEngine(FakeToolRegistry(tool))

            val result = engine.execute("open_app", emptyMap())

            assertTrue(tool.invoked)
            assertTrue(result is AuraResult.Success)
        }

    @Test
    fun `execute refuses a tool that requires confirmation without running it`() =
        runBlocking {
            val tool = RecordingTool("schedule_action", requiresConfirmation = true)
            val engine = DefaultActionEngine(FakeToolRegistry(tool))

            val result = engine.execute("schedule_action", emptyMap())

            assertTrue("a confirmation-gated tool must never actually run via execute()", !tool.invoked)
            assertTrue(result is AuraResult.Failure)
            assertTrue((result as AuraResult.Failure).error is AuraError.NotSupported)
        }

    @Test
    fun `executeConfirmed runs a tool that requires confirmation`() =
        runBlocking {
            val tool = RecordingTool("schedule_action", requiresConfirmation = true)
            val engine = DefaultActionEngine(FakeToolRegistry(tool))

            val result = engine.executeConfirmed("schedule_action", emptyMap())

            assertTrue(tool.invoked)
            assertTrue(result is AuraResult.Success)
        }

    @Test
    fun `execute on an unregistered tool fails honestly`() =
        runBlocking {
            val engine = DefaultActionEngine(FakeToolRegistry())

            val result = engine.execute("does_not_exist", emptyMap())

            assertTrue(result is AuraResult.Failure)
            assertTrue((result as AuraResult.Failure).error is AuraError.NotSupported)
        }

    @Test
    fun `executeConfirmed on an unregistered tool also fails honestly, not silently`() =
        runBlocking {
            val engine = DefaultActionEngine(FakeToolRegistry())

            val result = engine.executeConfirmed("does_not_exist", emptyMap())

            assertTrue(result is AuraResult.Failure)
        }
}
