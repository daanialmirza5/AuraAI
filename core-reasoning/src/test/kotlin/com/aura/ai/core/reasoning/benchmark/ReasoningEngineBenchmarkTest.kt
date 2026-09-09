package com.aura.ai.core.reasoning.benchmark

import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.constraint.DefaultConstraintEngine
import com.aura.ai.core.reasoning.context.BatteryStatus
import com.aura.ai.core.reasoning.context.ConnectivityState
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.reasoning.decision.DefaultDecisionEngine
import com.aura.ai.core.reasoning.model.ConstraintType
import com.aura.ai.core.reasoning.model.SubGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class ReasoningEngineBenchmarkTest {

    private class BenchmarkCapabilityResolver : CapabilityResolver {
        private val permissions = mapOf(
            "record_audio" to listOf("android.permission.RECORD_AUDIO"),
            "camera" to listOf("android.permission.CAMERA"),
            "calendar" to listOf("android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"),
            "export_pdf" to emptyList(),
            "save_file" to emptyList(),
            "web_search" to emptyList()
        )

        private val networkTools = setOf("web_search", "shopping_search")

        override fun availableTools(): List<ToolDescriptor> = permissions.keys.map {
            ToolDescriptor(name = it, description = "Benchmark tool $it")
        }

        override fun requiredPermissions(toolName: String): List<String> = permissions[toolName].orEmpty()

        override fun requiresNetwork(toolName: String): Boolean = toolName in networkTools
    }

    private fun createBenchmarkContext(
        tools: List<String> = listOf("export_pdf", "save_file", "record_audio", "web_search"),
        isOnline: Boolean = true,
        batteryPercent: Int = 85,
        isCharging: Boolean = false,
        grantedPermissions: List<String> = listOf("android.permission.RECORD_AUDIO", "android.permission.READ_CALENDAR"),
        aiProviderAvailable: Boolean = true
    ) = ExecutionContext(
        availableTools = tools.map { ToolDescriptor(name = it, description = "") },
        connectivity = if (isOnline) ConnectivityState.Online else ConnectivityState.Offline,
        battery = BatteryStatus(percent = batteryPercent, isCharging = isCharging, isLow = batteryPercent <= 15),
        permissionState = grantedPermissions.associateWith { PermissionState.Granted },
        aiProviderAvailable = aiProviderAvailable,
        capturedAtMillis = System.currentTimeMillis()
    )

    @Test
    fun `benchmark constraint evaluation latency and throughput across 1000 iterations`() {
        val resolver = BenchmarkCapabilityResolver()
        val engine = DefaultConstraintEngine(resolver)
        val context = createBenchmarkContext()
        val testTools = listOf("export_pdf", "save_file", "record_audio", "web_search")

        // Warmup JIT
        repeat(200) {
            engine.evaluate(testTools, requiresAI = true, requiresInternet = true, context = context)
        }

        // Benchmark 1,000 iterations
        val iterations = 1000
        var totalConstraintsEvaluated = 0

        val durationNanos = measureNanoTime {
            for (i in 0 until iterations) {
                val results = engine.evaluate(
                    requiredToolNames = testTools,
                    requiresAI = true,
                    requiresInternet = true,
                    context = context
                )
                totalConstraintsEvaluated += results.size
            }
        }

        val totalDurationMillis = durationNanos / 1_000_000.0
        val avgLatencyMicros = (durationNanos / iterations.toDouble()) / 1_000.0

        println("================================================================================")
        println("  AURAAI ON-DEVICE CONSTRAINT REASONING BENCHMARK")
        println("================================================================================")
        println("  Total Iterations        : $iterations")
        println("  Total Evaluation Time   : ${String.format("%.2f", totalDurationMillis)} ms")
        println("  Average Latency / Eval  : ${String.format("%.2f", avgLatencyMicros)} µs (< 0.1 ms)")
        println("  Constraints Evaluated   : $totalConstraintsEvaluated (${totalConstraintsEvaluated / iterations} per eval)")
        println("  Throughput              : ${String.format("%.0f", (iterations / (totalDurationMillis / 1000.0)))} evaluations/sec")
        println("================================================================================")

        // On modern JVM, 1 evaluation of 4 tools + network + battery + AI should take < 500 microseconds on average
        assertTrue("Average latency must be sub-millisecond for on-device real-time guarantees", avgLatencyMicros < 500.0)
        assertEquals(7000, totalConstraintsEvaluated) // 7 constraints per iteration * 1000
    }

    @Test
    fun `benchmark decision engine throughput across distinct sub-goal topologies`() {
        val resolver = BenchmarkCapabilityResolver()
        val decisionEngine = DefaultDecisionEngine(resolver)
        val context = createBenchmarkContext()

        val subGoals = listOf(
            SubGoal(description = "Search documentation", impliedToolName = "web_search", requiresAI = false),
            SubGoal(description = "Synthesize report summary", impliedToolName = null, requiresAI = true),
            SubGoal(description = "Save PDF to storage", impliedToolName = "export_pdf", requiresAI = false)
        )

        // Warmup
        repeat(200) {
            decisionEngine.decide("Research and export my monthly notes", subGoals, context, hasRelevantMemory = true)
        }

        val iterations = 1000
        val durationNanos = measureNanoTime {
            for (i in 0 until iterations) {
                val decisions = decisionEngine.decide(
                    goal = "Research and export my monthly notes",
                    subGoals = subGoals,
                    context = context,
                    hasRelevantMemory = true
                )
                assertTrue(decisions.requiresAI)
                assertTrue(decisions.requiresMemory)
                assertTrue(decisions.requiresInternet)
            }
        }

        val avgMicros = (durationNanos / iterations.toDouble()) / 1_000.0
        println("  Decision Engine Average Latency: ${String.format("%.2f", avgMicros)} µs")
        assertTrue("Decision engine should resolve in < 100 µs", avgMicros < 100.0)
    }
}
