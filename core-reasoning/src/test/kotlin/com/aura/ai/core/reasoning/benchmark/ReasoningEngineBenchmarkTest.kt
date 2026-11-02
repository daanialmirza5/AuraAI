package com.aura.ai.core.reasoning.benchmark

import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.constraint.DefaultConstraintEngine
import com.aura.ai.core.reasoning.context.BatteryStatus
import com.aura.ai.core.reasoning.context.ConnectivityState
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.reasoning.decision.DefaultDecisionEngine
import com.aura.ai.core.reasoning.model.SubGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.system.measureNanoTime

class ReasoningEngineBenchmarkTest {
    private class BenchmarkCapabilityResolver : CapabilityResolver {
        private val permissions =
            mapOf(
                "record_audio" to listOf("android.permission.RECORD_AUDIO"),
                "camera" to listOf("android.permission.CAMERA"),
                "calendar" to listOf("android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"),
                "export_pdf" to emptyList(),
                "save_file" to emptyList(),
                "web_search" to emptyList(),
            )

        private val networkTools = setOf("web_search", "shopping_search")

        override fun availableTools(): List<ToolDescriptor> =
            permissions.keys.map {
                ToolDescriptor(name = it, description = "Benchmark tool $it")
            }

        override fun requiredPermissions(toolName: String): List<String> = permissions[toolName].orEmpty()

        override fun requiresNetwork(toolName: String): Boolean = toolName in networkTools
    }

    private fun createBenchmarkContext(tools: List<String> = listOf("export_pdf", "save_file", "record_audio", "web_search")) =
        ExecutionContext(
            availableTools = tools.map { ToolDescriptor(name = it, description = "") },
            connectivity = ConnectivityState.Online,
            battery = BatteryStatus(percent = 85, isCharging = false, isLow = false),
            permissionState =
                listOf("android.permission.RECORD_AUDIO", "android.permission.READ_CALENDAR")
                    .associateWith { PermissionState.Granted },
            aiProviderAvailable = true,
            capturedAtMillis = System.currentTimeMillis(),
        )

    private fun Double.fmt(decimals: Int): String = String.format(Locale.ROOT, "%.${decimals}f", this)

    @Test
    fun `benchmark constraint evaluation latency and throughput across 1000 iterations`() {
        val resolver = BenchmarkCapabilityResolver()
        val engine = DefaultConstraintEngine(resolver)
        val context = createBenchmarkContext()
        val testTools = listOf("export_pdf", "save_file", "record_audio", "web_search")

        // Warm up the JIT before measuring.
        repeat(200) {
            engine.evaluate(testTools, requiresAI = true, requiresInternet = true, context = context)
        }

        val iterations = 1000
        var totalConstraintsEvaluated = 0

        val durationNanos =
            measureNanoTime {
                repeat(iterations) {
                    val results =
                        engine.evaluate(
                            requiredToolNames = testTools,
                            requiresAI = true,
                            requiresInternet = true,
                            context = context,
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
        println("  Total Evaluation Time   : ${totalDurationMillis.fmt(2)} ms")
        println("  Average Latency / Eval  : ${avgLatencyMicros.fmt(2)} µs")
        println("  Constraints Evaluated   : $totalConstraintsEvaluated (${totalConstraintsEvaluated / iterations} per eval)")
        println("  Throughput              : ${(iterations / (totalDurationMillis / 1000.0)).fmt(0)} evaluations/sec")
        println("================================================================================")

        // One evaluation covers 4 tools plus network, battery and AI checks.
        assertTrue("Average latency must be sub-millisecond for on-device real-time guarantees", avgLatencyMicros < 500.0)
        // Per evaluation: each tool yields one availability and one permission result (every
        // benchmark tool needs at most one permission), plus one network, battery and AI check.
        val expectedPerEvaluation = testTools.size * 2 + 3
        assertEquals(expectedPerEvaluation * iterations, totalConstraintsEvaluated)
    }

    @Test
    fun `benchmark decision engine throughput across distinct sub-goal topologies`() {
        val resolver = BenchmarkCapabilityResolver()
        val decisionEngine = DefaultDecisionEngine(resolver)
        val context = createBenchmarkContext()

        val subGoals =
            listOf(
                SubGoal(id = "search", description = "Search documentation", priority = 1, impliedToolName = "web_search", requiresAI = false),
                SubGoal(id = "synthesize", description = "Synthesize report summary", priority = 2, impliedToolName = null, requiresAI = true),
                SubGoal(id = "export", description = "Save PDF to storage", priority = 3, impliedToolName = "export_pdf", requiresAI = false),
            )

        // Warm up the JIT before measuring.
        repeat(200) {
            decisionEngine.decide("Research and export my monthly notes", subGoals, context, hasRelevantMemory = true)
        }

        val iterations = 1000
        val durationNanos =
            measureNanoTime {
                repeat(iterations) {
                    val decisions =
                        decisionEngine.decide(
                            goal = "Research and export my monthly notes",
                            subGoals = subGoals,
                            context = context,
                            hasRelevantMemory = true,
                        )
                    assertTrue(decisions.requiresAI)
                    assertTrue(decisions.requiresMemory)
                    assertTrue(decisions.requiresInternet)
                }
            }

        val avgMicros = (durationNanos / iterations.toDouble()) / 1_000.0
        println("  Decision Engine Average Latency: ${avgMicros.fmt(2)} µs")
        assertTrue("Decision engine should resolve in < 100 µs", avgMicros < 100.0)
    }
}
