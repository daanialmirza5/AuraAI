package com.aura.ai.core.agents

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.capabilities.Capability
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.intent.RecognizedIntent
import java.util.UUID

/** One unit of work handed to an [Agent] — the goal in full, the intent already recognized for
 *  it (agents never re-recognize intent themselves), and any parameters a caller wants to pass
 *  beyond the raw goal text. */
data class AgentTask(
    val id: String = UUID.randomUUID().toString(),
    val goal: String,
    val intent: RecognizedIntent,
    val parameters: Map<String, String> = emptyMap(),
)

/** What an [Agent] hands back on success. [confidence] feeds
 *  `com.aura.ai.core.orchestrator.aggregation.ResultAggregator`'s "confidence aggregation" — an
 *  agent that's unsure how good its own answer is should say so here rather than always
 *  reporting 1.0. */
data class AgentResult(
    val summary: String,
    val data: Map<String, String> = emptyMap(),
    val confidence: Float = 1f,
)

enum class AgentHealthStatus { Healthy, Degraded, Unavailable }

/** The result of [Agent.health] — structured, not just a display string, so
 *  `com.aura.ai.core.orchestrator.dispatch.TaskDispatcher` can act on exactly what's missing
 *  (skip execution entirely on [AgentHealthStatus.Unavailable]; publish a
 *  `com.aura.ai.core.events.PermissionDeniedEvent` per entry in [missingPermissions]) rather than
 *  parsing prose. */
data class AgentHealth(
    val status: AgentHealthStatus,
    val detail: String,
    val missingPermissions: List<String> = emptyList(),
    val missingTools: List<String> = emptyList(),
)

/**
 * The one contract every one of AURA's 9 agents implements. Every field the brief asks for is
 * here — [name], [description], [capabilities], [requiredPermissions], [requiredTools],
 * [supportedIntents], [execute], [health] — plus two with safe defaults that exist purely to make
 * orchestration real rather than hand-waved:
 *
 * - [priority]: lower runs first when `AgentSelectionEngine` has to order more than one matching
 *   agent.
 * - [dependsOnCapabilities]: what this agent needs *already available* (typically because another
 *   selected agent just produced it into `SharedContext`) before it should run — the actual data
 *   `ExecutionCoordinator` topologically sorts on for "dependency ordering."
 *
 * Agents never call each other directly — the only channels between them are the
 * `com.aura.ai.core.events.EventBus` and [SharedContext]. Every dependency an `Agent`
 * implementation takes is on a *lower* module (`core-tools`, `core-memory`, `core-planner`,
 * `core-providers`, `core-reasoning`'s `PermissionChecker`), never on another `Agent`.
 */
interface Agent {
    val name: String
    val description: String
    val capabilities: Set<Capability>
    val requiredPermissions: List<String>
    val requiredTools: List<String>
    val supportedIntents: Set<IntentType>

    val priority: Int get() = DEFAULT_PRIORITY
    val dependsOnCapabilities: Set<Capability> get() = emptySet()

    suspend fun execute(
        task: AgentTask,
        context: SharedContext,
    ): AuraResult<AgentResult>

    suspend fun health(): AgentHealth

    companion object {
        const val DEFAULT_PRIORITY = 100
    }
}
