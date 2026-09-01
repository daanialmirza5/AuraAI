package com.aura.ai.core.reasoning.validation

import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.planner.ExecutionPlan
import com.aura.ai.core.planner.PlanStep
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.context.ConnectivityState
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.reasoning.model.ConstraintType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeCapabilityResolver(
    private val permissionsByTool: Map<String, List<String>> = emptyMap(),
) : CapabilityResolver {
    override fun availableTools(): List<ToolDescriptor> = emptyList()

    override fun requiredPermissions(toolName: String): List<String> = permissionsByTool[toolName].orEmpty()

    override fun requiresNetwork(toolName: String): Boolean = false
}

private fun context(
    availableTools: List<String> = emptyList(),
    permissionState: Map<String, PermissionState> = emptyMap(),
    aiProviderAvailable: Boolean = true,
) = ExecutionContext(
    availableTools = availableTools.map { ToolDescriptor(name = it, description = "") },
    connectivity = ConnectivityState.Online,
    battery = null,
    permissionState = permissionState,
    aiProviderAvailable = aiProviderAvailable,
    capturedAtMillis = 0L,
)

private fun plan(vararg steps: PlanStep) = ExecutionPlan(id = "p1", goal = "goal", steps = steps.toList(), createdAtMillis = 0L)

class DefaultActionValidatorTest {
    @Test
    fun `a step with no tool name is validated against AI provider availability`() {
        val validator = DefaultActionValidator(FakeCapabilityResolver())
        val step = PlanStep(id = "1", description = "generate content", toolName = null)

        val withProvider = validator.validate(plan(step), context(aiProviderAvailable = true))
        assertTrue(withProvider.single().satisfied)
        assertTrue(withProvider.single().type == ConstraintType.AiProvider)

        val withoutProvider = validator.validate(plan(step), context(aiProviderAvailable = false))
        assertFalse(withoutProvider.single().satisfied)
    }

    @Test
    fun `a step with an unregistered tool fails tool-availability and produces no permission checks`() {
        val resolver = FakeCapabilityResolver(permissionsByTool = mapOf("save_file" to listOf("android.permission.WRITE")))
        val validator = DefaultActionValidator(resolver)
        val step = PlanStep(id = "1", description = "save it", toolName = "save_file")

        val results = validator.validate(plan(step), context(availableTools = emptyList()))

        assertFalse(results.single { it.type == ConstraintType.ToolAvailability }.satisfied)
    }

    @Test
    fun `a registered step checks every required permission individually`() {
        val resolver = FakeCapabilityResolver(permissionsByTool = mapOf("record_audio_tool" to listOf("android.permission.RECORD_AUDIO")))
        val validator = DefaultActionValidator(resolver)
        val step = PlanStep(id = "1", description = "record", toolName = "record_audio_tool")

        val granted =
            validator.validate(
                plan(step),
                context(
                    availableTools = listOf("record_audio_tool"),
                    permissionState =
                        mapOf(
                            "android.permission.RECORD_AUDIO" to PermissionState.Granted,
                        ),
                ),
            )
        assertTrue(granted.single { it.type == ConstraintType.ToolAvailability }.satisfied)
        assertTrue(granted.single { it.type == ConstraintType.Permission }.satisfied)

        val denied =
            validator.validate(
                plan(step),
                context(
                    availableTools = listOf("record_audio_tool"),
                    permissionState =
                        mapOf(
                            "android.permission.RECORD_AUDIO" to PermissionState.Denied,
                        ),
                ),
            )
        assertFalse(denied.single { it.type == ConstraintType.Permission }.satisfied)
    }

    @Test
    fun `validation flattens results across every step in the plan, in order`() {
        val validator = DefaultActionValidator(FakeCapabilityResolver())
        val steps =
            plan(
                PlanStep(id = "1", description = "step one", toolName = "open_app"),
                PlanStep(id = "2", description = "step two", toolName = null),
            )

        val results = validator.validate(steps, context(availableTools = listOf("open_app"), aiProviderAvailable = true))

        assertTrue(results.any { it.description.startsWith("Step 1") })
        assertTrue(results.any { it.description.startsWith("Step 2") })
    }
}
