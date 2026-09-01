package com.aura.ai.core.reasoning.constraint

import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.context.BatteryStatus
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
    connectivity: ConnectivityState = ConnectivityState.Online,
    battery: BatteryStatus? = null,
    permissionState: Map<String, PermissionState> = emptyMap(),
    aiProviderAvailable: Boolean = true,
) = ExecutionContext(
    availableTools = availableTools.map { ToolDescriptor(name = it, description = "") },
    connectivity = connectivity,
    battery = battery,
    permissionState = permissionState,
    aiProviderAvailable = aiProviderAvailable,
    capturedAtMillis = 0L,
)

class DefaultConstraintEngineTest {
    @Test
    fun `tool availability check is satisfied only when the context actually has the tool`() {
        val engine = DefaultConstraintEngine(FakeCapabilityResolver())

        val registered =
            engine.evaluate(
                listOf("save_file"),
                requiresAI = false,
                requiresInternet = false,
                context(availableTools = listOf("save_file")),
            )
        assertTrue(registered.single { it.type == ConstraintType.ToolAvailability }.satisfied)

        val missing = engine.evaluate(listOf("save_file"), requiresAI = false, requiresInternet = false, context())
        assertFalse(missing.single { it.type == ConstraintType.ToolAvailability }.satisfied)
    }

    @Test
    fun `a tool needing no permissions produces one satisfied permission result`() {
        val engine = DefaultConstraintEngine(FakeCapabilityResolver())

        val results = engine.evaluate(listOf("save_file"), requiresAI = false, requiresInternet = false, context())

        val permissionResults = results.filter { it.type == ConstraintType.Permission }
        assertTrue(permissionResults.size == 1 && permissionResults.single().satisfied)
    }

    @Test
    fun `each required permission is checked individually against the granted state`() {
        val resolver =
            FakeCapabilityResolver(
                permissionsByTool =
                    mapOf(
                        "record_audio_tool" to listOf("android.permission.RECORD_AUDIO", "android.permission.POST_NOTIFICATIONS"),
                    ),
            )
        val engine = DefaultConstraintEngine(resolver)
        val ctx =
            context(
                permissionState =
                    mapOf(
                        "android.permission.RECORD_AUDIO" to PermissionState.Granted,
                        "android.permission.POST_NOTIFICATIONS" to PermissionState.Denied,
                    ),
            )

        val results = engine.evaluate(listOf("record_audio_tool"), requiresAI = false, requiresInternet = false, ctx)

        val permissionResults = results.filter { it.type == ConstraintType.Permission }
        assertTrue(permissionResults.size == 2)
        assertTrue(permissionResults.single { it.description == "RECORD_AUDIO" }.satisfied)
        assertFalse(permissionResults.single { it.description == "POST_NOTIFICATIONS" }.satisfied)
    }

    @Test
    fun `an unconfirmed permission state is treated as not satisfied`() {
        val resolver = FakeCapabilityResolver(permissionsByTool = mapOf("t" to listOf("android.permission.CAMERA")))
        val engine = DefaultConstraintEngine(resolver)

        val results = engine.evaluate(listOf("t"), requiresAI = false, requiresInternet = false, context())

        assertFalse(results.single { it.type == ConstraintType.Permission }.satisfied)
    }

    @Test
    fun `network check is satisfied unconditionally when the goal does not require internet`() {
        val engine = DefaultConstraintEngine(FakeCapabilityResolver())

        val results =
            engine.evaluate(
                emptyList(),
                requiresAI = false,
                requiresInternet = false,
                context(connectivity = ConnectivityState.Offline),
            )

        assertTrue(results.single { it.type == ConstraintType.Network }.satisfied)
    }

    @Test
    fun `network check reflects actual connectivity when the goal requires internet`() {
        val engine = DefaultConstraintEngine(FakeCapabilityResolver())

        val online =
            engine.evaluate(
                emptyList(),
                requiresAI = false,
                requiresInternet = true,
                context(connectivity = ConnectivityState.Online),
            )
        assertTrue(online.single { it.type == ConstraintType.Network }.satisfied)

        val offline =
            engine.evaluate(
                emptyList(),
                requiresAI = false,
                requiresInternet = true,
                context(connectivity = ConnectivityState.Offline),
            )
        assertFalse(offline.single { it.type == ConstraintType.Network }.satisfied)
    }

    @Test
    fun `battery check fails only when low and not charging`() {
        val engine = DefaultConstraintEngine(FakeCapabilityResolver())

        val lowNotCharging =
            engine.evaluate(
                emptyList(),
                requiresAI = false,
                requiresInternet = false,
                context(battery = BatteryStatus(percent = 5, isCharging = false, isLow = true)),
            )
        assertFalse(lowNotCharging.single { it.type == ConstraintType.Battery }.satisfied)

        val lowButCharging =
            engine.evaluate(
                emptyList(),
                requiresAI = false,
                requiresInternet = false,
                context(battery = BatteryStatus(percent = 5, isCharging = true, isLow = true)),
            )
        assertTrue(lowButCharging.single { it.type == ConstraintType.Battery }.satisfied)

        val unknownBattery = engine.evaluate(emptyList(), requiresAI = false, requiresInternet = false, context(battery = null))
        assertTrue(unknownBattery.single { it.type == ConstraintType.Battery }.satisfied)
    }

    @Test
    fun `AI provider check only appears when the goal requires AI`() {
        val engine = DefaultConstraintEngine(FakeCapabilityResolver())

        val withoutAi = engine.evaluate(emptyList(), requiresAI = false, requiresInternet = false, context())
        assertTrue(withoutAi.none { it.type == ConstraintType.AiProvider })

        val withAi = engine.evaluate(emptyList(), requiresAI = true, requiresInternet = false, context(aiProviderAvailable = false))
        assertFalse(withAi.single { it.type == ConstraintType.AiProvider }.satisfied)
    }

    @Test
    fun `duplicate tool names are only evaluated once`() {
        val engine = DefaultConstraintEngine(FakeCapabilityResolver())

        val results =
            engine.evaluate(
                listOf("save_file", "save_file"),
                requiresAI = false,
                requiresInternet = false,
                context(availableTools = listOf("save_file")),
            )

        assertTrue(results.count { it.type == ConstraintType.ToolAvailability } == 1)
    }
}
