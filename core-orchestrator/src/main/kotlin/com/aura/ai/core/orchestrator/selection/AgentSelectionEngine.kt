package com.aura.ai.core.orchestrator.selection

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.intent.RecognizedIntent

/**
 * "Which agents are needed" — the brief's first orchestrator decision. Resolves not just the
 * agents whose `supportedIntents` directly match, but also every agent those agents transitively
 * need via `Agent.dependsOnCapabilities` (e.g. selecting `PlannerAgent` for any intent also pulls
 * in `MemoryAgent`, since `PlannerAgent` depends on `Capability.MemoryRecall`) — without that
 * closure, an agent whose own `supportedIntents` is honestly empty (several of the 9 are, by
 * design — see `docs/AGENT_SYSTEM.md`) could never run.
 */
interface AgentSelectionEngine {
    fun selectAgents(intent: RecognizedIntent): List<Agent>
}
