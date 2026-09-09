package com.aura.ai.core.reasoning.constraint

import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.context.BatteryStatus
import com.aura.ai.core.reasoning.context.ConnectivityState
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.reasoning.model.ConstraintCheckResult
import com.aura.ai.core.reasoning.model.ConstraintType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class MockEdgeCaseCapabilityResolver(
    private val toolPermissions: Map<String, List<String>> = emptyMap(),
    private val networkTools: Set<String> = emptySet(),
) : CapabilityResolver {
    override fun availableTools(): List<ToolDescriptor> = toolPermissions.keys.map { ToolDescriptor(it, "") }
    override fun requiredPermissions(toolName: String): List<String> = toolPermissions[toolName].orEmpty()
    override fun requiresNetwork(toolName: String): Boolean = toolName in networkTools
}

class ConstraintEngineEdgeCasesTest {

    private fun buildContext(
        tools: List<String> = emptyList(),
        connectivity: ConnectivityState = ConnectivityState.Online,
        battery: BatteryStatus? = null,
        permissions: Map<String, PermissionState> = emptyMap(),
        aiProvider: Boolean = true
    ) = ExecutionContext(
        availableTools = tools.map { ToolDescriptor(name = it, description = "") },
        connectivity = connectivity,
        battery = battery,
        permissionState = permissions,
        aiProviderAvailable = aiProvider,
        capturedAtMillis = 1000L
    )

    @Test
    fun `evaluates multi-permission tool when only partial permissions are granted`() {
        val resolver = MockEdgeCaseCapabilityResolver(
            toolPermissions = mapOf(
                "calendar_scheduler" to listOf(
                    "android.permission.READ_CALENDAR",
                    "android.permission.WRITE_CALENDAR"
                )
            )
        )
        val engine = DefaultConstraintEngine(resolver)

        // READ granted, WRITE denied
        val context = buildContext(
            tools = listOf("calendar_scheduler"),
            permissions = mapOf(
                "android.permission.READ_CALENDAR" to PermissionState.Granted,
                "android.permission.WRITE_CALENDAR" to PermissionState.Denied
            )
        )

        val results = engine.evaluate(
            listOf("calendar_scheduler"),
            requiresAI = false,
            requiresInternet = false,
            context = context
        )

        val permResults = results.filter { it.type == ConstraintType.Permission }
        assertEquals(2, permResults.size)
        assertTrue("READ_CALENDAR must be satisfied", permResults.single { it.description == "READ_CALENDAR" }.satisfied)
        assertFalse("WRITE_CALENDAR must be unsatisfied", permResults.single { it.description == "WRITE_CALENDAR" }.satisfied)
        assertTrue(permResults.single { it.description == "WRITE_CALENDAR" }.detail.contains("denied"))
    }

    @Test
    fun `boundary test - battery low threshold at 15 percent when charging vs discharging`() {
        val engine = DefaultConstraintEngine(MockEdgeCaseCapabilityResolver())

        // 15% and discharging -> isLow = true -> Battery constraint unsatisfied
        val dischargingContext = buildContext(
            battery = BatteryStatus(percent = 15, isCharging = false, isLow = true)
        )
        val dischargingResults = engine.evaluate(emptyList(), false, false, dischargingContext)
        assertFalse(dischargingResults.single { it.type == ConstraintType.Battery }.satisfied)

        // 15% and charging -> isLow = true, isCharging = true -> Battery constraint SATISFIED (charging overrides low battery blocker)
        val chargingContext = buildContext(
            battery = BatteryStatus(percent = 15, isCharging = true, isLow = true)
        )
        val chargingResults = engine.evaluate(emptyList(), false, false, chargingContext)
        assertTrue(chargingResults.single { it.type == ConstraintType.Battery }.satisfied)
    }

    @Test
    fun `composite multi-failure scenario correctly surfaces all individual blockers and warnings`() {
        val resolver = MockEdgeCaseCapabilityResolver(
            toolPermissions = mapOf(
                "camera_scanner" to listOf("android.permission.CAMERA")
            )
        )
        val engine = DefaultConstraintEngine(resolver)

        // Extreme degraded context: offline, no AI provider, battery low (5%), camera tool missing, permission denied
        val extremeContext = buildContext(
            tools = emptyList(), // camera_scanner NOT registered in runtime
            connectivity = ConnectivityState.Offline,
            battery = BatteryStatus(percent = 5, isCharging = false, isLow = true),
            permissions = mapOf("android.permission.CAMERA" to PermissionState.Denied),
            aiProvider = false
        )

        val results = engine.evaluate(
            requiredToolNames = listOf("camera_scanner"),
            requiresAI = true,
            requiresInternet = true,
            context = extremeContext
        )

        assertEquals(5, results.size) // Tool + Permission + Network + Battery + AI Provider
        val unsatisfied = results.filterNot { it.satisfied }
        assertEquals("All 5 constraints must fail in extreme degraded state", 5, unsatisfied.size)

        assertTrue(unsatisfied.any { it.type == ConstraintType.ToolAvailability })
        assertTrue(unsatisfied.any { it.type == ConstraintType.Permission })
        assertTrue(unsatisfied.any { it.type == ConstraintType.Network })
        assertTrue(unsatisfied.any { it.type == ConstraintType.Battery })
        assertTrue(unsatisfied.any { it.type == ConstraintType.AiProvider })
    }

    @Test
    fun `evaluates empty tool list gracefully without unnecessary permission checks`() {
        val engine = DefaultConstraintEngine(MockEdgeCaseCapabilityResolver())
        val context = buildContext(connectivity = ConnectivityState.Online)

        val results = engine.evaluate(emptyList(), requiresAI = false, requiresInternet = false, context = context)

        assertEquals(2, results.size) // Only Network and Battery checks
        assertTrue(results.all { it.satisfied })
    }
}
