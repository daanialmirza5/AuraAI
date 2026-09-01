package com.aura.ai.core.agents

import java.util.concurrent.ConcurrentHashMap

/**
 * "Context Sharing" — the brief's other sanctioned channel between agents, alongside the event
 * bus. A plain, thread-safe blackboard: one agent's [put] is visible to every agent that runs
 * after it (or alongside it, once written) in the same orchestration pass, without either agent
 * knowing the other exists. `com.aura.ai.core.orchestrator.coordination.ExecutionCoordinator`
 * creates exactly one [SharedContext] per `AgentOrchestrator.orchestrate` call and passes the same
 * instance to every agent it runs.
 *
 * Deliberately `String -> String`, the same shape as `com.aura.ai.core.tools.ToolResult.data` and
 * `com.aura.ai.core.planner.PlanStep.parameters` — every other cross-module handoff in this
 * codebase already uses plain string maps rather than passing rich types across a boundary, and
 * shared context between independently-developed agents is exactly that kind of boundary.
 */
interface SharedContext {
    val goal: String

    fun put(
        key: String,
        value: String,
    )

    fun get(key: String): String?

    fun snapshot(): Map<String, String>
}

class DefaultSharedContext(
    override val goal: String,
) : SharedContext {
    private val values = ConcurrentHashMap<String, String>()

    override fun put(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun get(key: String): String? = values[key]

    override fun snapshot(): Map<String, String> = values.toMap()
}
