package com.aura.ai.core.events

import java.util.UUID

/**
 * The one thing every module in the multi-agent system communicates through instead of calling
 * each other directly — "no agent should directly depend on another agent, only communicate
 * through Event Bus / Shared Context." Deliberately lightweight: every event below carries only
 * primitive data (strings, numbers, booleans), never a rich domain type from another module —
 * that's what keeps `core-events` dependency-free and safe for literally every other module to
 * depend on without risking a cycle.
 *
 * [eventId] and [timestampMillis] are common to every event but deliberately not part of any
 * subtype's own `data class` equality — two events with identical content published at different
 * times are still "the same fact" for comparison purposes, which is why these two are plain
 * constructor defaults on the base class rather than primary-constructor properties duplicated
 * into each subtype.
 */
sealed class AuraEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val timestampMillis: Long = System.currentTimeMillis(),
)

/** An utterance was classified — `core-intent.IntentRecognizer`'s output, in trace form. */
data class IntentRecognizedEvent(
    val utterance: String,
    val intentType: String,
    val confidence: Float,
) : AuraEvent()

/** A memory lookup ran — `core-memory.MemoryRetriever`'s output, in trace form. */
data class MemoryRetrievedEvent(
    val query: String,
    val memoryCount: Int,
) : AuraEvent()

/** `core-planner.Planner` produced an `ExecutionPlan`. */
data class PlanCreatedEvent(
    val goal: String,
    val planId: String,
    val stepCount: Int,
) : AuraEvent()

/** A `core-tools.Tool` ran to completion (successfully or not). */
data class ToolExecutedEvent(
    val toolName: String,
    val succeeded: Boolean,
    val summary: String,
) : AuraEvent()

/** A `core-providers.AIProvider` became usable. Defined for completeness — nothing in this
 *  codebase publishes it yet, since no provider's `isAvailable()` ever returns `true` this phase;
 *  the day one does, the check that fires this event is already in place (see
 *  `com.aura.ai.core.orchestrator.DefaultAgentOrchestrator`). */
data class ProviderConnectedEvent(
    val providerId: String,
) : AuraEvent()

/** One `core-agents.Agent` finished a task — success or failure both produce this alongside
 *  [TaskFailedEvent] on the failure path, so a listener can subscribe to "every agent that ran"
 *  without also having to separately watch for failures. */
data class AgentCompletedEvent(
    val agentName: String,
    val taskId: String,
    val succeeded: Boolean,
    val durationMillis: Long,
) : AuraEvent()

/** A task an agent (or the orchestrator) attempted did not succeed, after retries. */
data class TaskFailedEvent(
    val taskId: String,
    val reason: String,
    val agentName: String? = null,
) : AuraEvent()

/** An agent needed a permission that isn't confirmed granted — published instead of, not in
 *  addition to, a generic [TaskFailedEvent] for that specific cause, so a UI can react
 *  specifically ("ask for this permission") rather than just "something failed." */
data class PermissionDeniedEvent(
    val permission: String,
    val agentName: String? = null,
) : AuraEvent()

// --- Phase 7: Plugin lifecycle ------------------------------------------------------------
//
// Published by plugin-runtime, which bridges com.aura.ai.plugin.api.PluginEvent (the
// plugin-facing vocabulary plugin-api defines independently, since plugin-api has zero
// dependency on this module) into these host-facing equivalents. A plugin never sees or
// publishes an AuraEvent directly — see docs/PLUGIN_RUNTIME.md.

/** A plugin's `onLoad` succeeded. */
data class PluginLoadedEvent(
    val pluginId: String,
) : AuraEvent()

/** A plugin's `onEnable` succeeded — it may now have live tools/capabilities/intents registered. */
data class PluginEnabledEvent(
    val pluginId: String,
) : AuraEvent()

/** A plugin's `onDisable` succeeded — still loaded, no longer contributing anything. */
data class PluginDisabledEvent(
    val pluginId: String,
) : AuraEvent()

/** A plugin's `onUnload` succeeded — terminal for that plugin instance. */
data class PluginUnloadedEvent(
    val pluginId: String,
) : AuraEvent()

/** A plugin lifecycle callback failed, or its manifest was rejected before ever loading. */
data class PluginFailedEvent(
    val pluginId: String,
    val reason: String,
) : AuraEvent()

/** A plugin's self-reported health changed since the last check. [status] mirrors
 *  `com.aura.ai.plugin.api.PluginHealthStatus.name` as a plain string — this module never
 *  imports plugin-api, the same way it never imports any other module. */
data class PluginHealthChangedEvent(
    val pluginId: String,
    val status: String,
) : AuraEvent()
