package com.aura.ai.ai.runtime

import com.aura.ai.ai.PlanExecutor
import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.AgentTask
import com.aura.ai.core.agents.DefaultSharedContext
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.intent.IntentRecognizer
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.intent.RecognizedIntent
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.retrieval.MemoryRetriever
import com.aura.ai.core.orchestrator.aggregation.AggregatedResult
import com.aura.ai.core.orchestrator.aggregation.ResultAggregator
import com.aura.ai.core.orchestrator.coordination.ExecutionCoordinator
import com.aura.ai.core.orchestrator.selection.AgentSelectionEngine
import com.aura.ai.core.reasoning.ReasoningEngine
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.context.ConnectivityState
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.ExecutionContextProvider
import com.aura.ai.core.reasoning.model.ConfidenceScore
import com.aura.ai.core.reasoning.model.FailureRecoveryPlan
import com.aura.ai.core.reasoning.model.ReasoningDecisions
import com.aura.ai.core.reasoning.model.ReasoningOutcome
import com.aura.ai.core.reasoning.model.ReasoningTrace
import com.aura.ai.core.reasoning.model.ReasoningVerdict
import com.aura.ai.core.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real execution flow the platform review called for, in one place: User Message →
 * Conversation Session → Execution Context → Intent Recognition → Memory Retrieval → Reasoning
 * Engine (which itself composes Decision Engine, Goal Manager, Constraint Engine, Confidence
 * Evaluator, and Planner — see `docs/RUNTIME_PIPELINE.md` §2 for why those four don't get their
 * own separate calls here) → Agent Orchestrator → Execution Coordinator → Tool Registry →
 * Provider Manager (only when needed) → Response Builder. Every dependency below is an existing,
 * already-built interface from Phases 3–7; this class contains no reasoning, planning, or
 * execution logic of its own — only sequencing and response formatting.
 */
@Singleton
class ConversationPipeline
    @Inject
    constructor(
        private val executionContextProvider: ExecutionContextProvider,
        private val intentRecognizer: IntentRecognizer,
        private val memoryRetriever: MemoryRetriever,
        private val reasoningEngine: ReasoningEngine,
        private val capabilityResolver: CapabilityResolver,
        private val agentSelectionEngine: AgentSelectionEngine,
        private val executionCoordinator: ExecutionCoordinator,
        private val resultAggregator: ResultAggregator,
        private val planExecutor: PlanExecutor,
    ) {
        private companion object {
            /** Agents that only plan or only gather context — never a sign the goal was actually
             *  *carried out*. Used to decide whether the plan needs executing directly; see
             *  [executeGoal]. */
            val NON_EXECUTING_AGENTS = setOf("PlannerAgent", "MemoryAgent")
        }

        /**
         * The whole pipeline runs on [Dispatchers.Default], not whatever dispatcher the caller used
         * (in practice, `viewModelScope`'s `Dispatchers.Main.immediate`): intent recognition, memory
         * ranking, and reasoning are all CPU-bound rule/heuristic work with no I/O of their own — see
         * `docs/PERFORMANCE.md` §4. Stages that *do* need I/O (the four HTTP providers, Room) already
         * switch to their own dispatcher internally, so nesting under `Default` here doesn't change
         * their behavior, only what this function's own CPU work runs on.
         */
        suspend fun run(
            session: ConversationSession,
            userMessage: String,
        ): ExecutionResult =
            withContext(Dispatchers.Default) {
                val overallStart = System.currentTimeMillis()
                val stageMillis = mutableMapOf<String, Long>()

                val intent = timed(stageMillis, "intentRecognition") { recognizeIntent(userMessage) }
                val retrievedMemories = timed(stageMillis, "memoryRetrieval") { retrieveMemories(userMessage) }

                val reasoningOutcome =
                    timed(stageMillis, "reasoning") {
                        when (val result = reasoningEngine.reason(userMessage, intent)) {
                            is AuraResult.Success -> result.value
                            is AuraResult.Failure -> null
                        }
                    }

                if (reasoningOutcome == null) {
                    return@withContext buildUnreasonedResult(session, userMessage, intent, retrievedMemories, stageMillis, overallStart)
                }

                val relevantPermissions =
                    capabilityResolver
                        .requiredPermissions(reasoningOutcome.subGoals.mapNotNull { it.impliedToolName })
                        .toSet()
                val executionContext =
                    timed(stageMillis, "executionContext") {
                        executionContextProvider.capture(relevantPermissions)
                    }

                val selectedAgents =
                    timed(stageMillis, "agentSelection") {
                        agentSelectionEngine.selectAgents(intent)
                    }

                val execution =
                    timed(stageMillis, "execution") {
                        executeGoal(userMessage, intent, reasoningOutcome, selectedAgents)
                    }

                val responseText = buildResponse(reasoningOutcome, execution)
                val totalMillis = System.currentTimeMillis() - overallStart

                val trace =
                    ExecutionTrace(
                        sessionId = session.id,
                        userMessage = userMessage,
                        recognizedIntent = intent,
                        executionContext = executionContext,
                        reasoningTrace = reasoningOutcome.trace,
                        decisions = reasoningOutcome.decisions,
                        verdict = reasoningOutcome.verdict,
                        selectedAgents = selectedAgents.map { it.name },
                        selectedTools = execution.toolsInvoked,
                        retrievedMemories = retrievedMemories,
                        executionPlan = reasoningOutcome.executionPlan,
                        confidence = reasoningOutcome.confidence,
                        timing = TimingMetrics(totalMillis, stageMillis),
                        providerRequired = reasoningOutcome.decisions.requiresAI,
                        providerConnected = executionContext.aiProviderAvailable,
                        alternatives = reasoningOutcome.alternatives,
                        recoveryPlan = reasoningOutcome.recoveryPlan,
                    )

                ExecutionResult(responseText, trace)
            }

        private suspend fun recognizeIntent(userMessage: String): RecognizedIntent =
            when (val result = intentRecognizer.recognize(userMessage)) {
                is AuraResult.Success -> result.value
                is AuraResult.Failure -> RecognizedIntent(IntentType.Unknown, confidence = 0f, rawText = userMessage)
            }

        private suspend fun retrieveMemories(userMessage: String): List<MemoryEntry> =
            when (val result = memoryRetriever.retrieve(userMessage)) {
                is AuraResult.Success -> result.value
                is AuraResult.Failure -> emptyList()
            }

        /** One goal's worth of "Execution Coordinator → Tool Registry → Provider Manager." */
        private data class GoalExecution(
            val aggregated: AggregatedResult?,
            val planResult: AuraResult<Map<String, ToolResult>>?,
            val toolsInvoked: List<String>,
        )

        /**
         * Which path actually carries the goal out depends on whether a *specialized* agent (not just
         * `PlannerAgent`/`MemoryAgent`, which only plan or only gather context) is available for this
         * intent:
         *
         * - If one is (`ResearchAgent` for `Research`, `CalendarAgent` for `Calendar`, and so on), the
         *   agents run for real via [ExecutionCoordinator] — the exact tool that agent wraps is the
         *   same one `Planner` would have put in the plan anyway, so running the plan too would
         *   double-invoke it (e.g. two browser searches for one "research X" goal).
         * - If none is (`"Open Spotify"` selects only `PlannerAgent`, which *generates* a plan but
         *   never runs it) — or agent execution produced no real success — the goal's
         *   [ReasoningOutcome.executionPlan] is executed directly via [PlanExecutor], the same class
         *   Phase 3 built for exactly this: walking a plan's steps through `ActionEngine` for tool
         *   steps and `AIProviderManager` for steps with no tool, honestly failing with
         *   `AuraError.ProviderNotConnected` when one is needed and isn't there.
         */
        private suspend fun executeGoal(
            goal: String,
            intent: RecognizedIntent,
            reasoningOutcome: ReasoningOutcome,
            selectedAgents: List<Agent>,
        ): GoalExecution {
            if (reasoningOutcome.decisions.shouldSplitTask) {
                return runPlanDirectly(reasoningOutcome, aggregated = null)
            }

            val sharedContext = DefaultSharedContext(goal)
            val task = AgentTask(goal = goal, intent = intent)
            val outcomes = executionCoordinator.coordinate(selectedAgents, task, sharedContext)
            val aggregated = resultAggregator.aggregate(outcomes)

            val succeededSpecialists =
                outcomes
                    .filter { it.result is AuraResult.Success && it.agentName !in NON_EXECUTING_AGENTS }
                    .map { it.agentName }
                    .toSet()

            if (succeededSpecialists.isNotEmpty()) {
                val toolsInvoked =
                    selectedAgents
                        .filter { it.name in succeededSpecialists }
                        .flatMap { it.requiredTools }
                return GoalExecution(aggregated = aggregated, planResult = null, toolsInvoked = toolsInvoked)
            }

            // No specialized agent actually carried this out — fall back to running the plan
            // directly, so goals like "open Spotify" (no dedicated executing agent, only
            // PlannerAgent's plan) still actually happen.
            return runPlanDirectly(reasoningOutcome, aggregated = aggregated)
        }

        private suspend fun runPlanDirectly(
            reasoningOutcome: ReasoningOutcome,
            aggregated: AggregatedResult?,
        ): GoalExecution {
            val plan =
                reasoningOutcome.executionPlan
                    ?: return GoalExecution(
                        aggregated = aggregated,
                        planResult = AuraResult.Failure(AuraError.NotSupported("No execution plan was generated for this goal.")),
                        toolsInvoked = emptyList(),
                    )
            val planResult = planExecutor.run(plan)
            val toolsInvoked = plan.steps.mapNotNull { it.toolName }
            return GoalExecution(aggregated = aggregated, planResult = planResult, toolsInvoked = toolsInvoked)
        }

        /** "Response Builder." Prefers the direct plan-execution outcome when one exists (it's the
         *  more concrete, tool-level result); falls back to the aggregated agent summary; and builds
         *  the "intelligent fallback" — explaining *why*, using [ReasoningOutcome.alternatives] and
         *  [ReasoningOutcome.recoveryPlan], both already computed by [ReasoningEngine] — when the
         *  failure is specifically a missing AI provider. */
        private fun buildResponse(
            reasoningOutcome: ReasoningOutcome,
            execution: GoalExecution,
        ): String {
            execution.planResult?.let { planResult ->
                return when (planResult) {
                    is AuraResult.Success -> {
                        val summaries = planResult.value.values.map { it.summary }
                        summaries.joinToString(" ").ifBlank { "Done." }
                    }
                    is AuraResult.Failure -> buildFallbackResponse(reasoningOutcome, planResult.error)
                }
            }

            val aggregated = execution.aggregated
            if (aggregated != null) {
                return if (aggregated.succeeded.isEmpty() && aggregated.failed.isNotEmpty()) {
                    "I couldn't complete that — ${aggregated.summary}"
                } else {
                    aggregated.summary
                }
            }

            return "I wasn't able to work out how to help with that."
        }

        private fun buildFallbackResponse(
            reasoningOutcome: ReasoningOutcome,
            error: AuraError,
        ): String {
            val needsProvider = error is AuraError.ProviderNotConnected || error is AuraError.RequiresProvider
            val reason =
                if (needsProvider) {
                    "That needs a connected AI provider, which isn't set up yet."
                } else {
                    error.message
                }

            return buildString {
                append(reason)
                reasoningOutcome.alternatives.firstOrNull()?.let { alternative ->
                    append(' ')
                    append(alternative.description)
                }
                reasoningOutcome.recoveryPlan.actions.firstOrNull()?.let { recovery ->
                    append(' ')
                    append(recovery.description)
                }
            }
        }

        /** Reasoning itself failing outright (rather than reaching a verdict) is rare — it means a
         *  lower stage like `GoalManager`'s own memory lookup failed — but still produces a full,
         *  honest [ExecutionResult] rather than throwing, matching every other failure path here. */
        private fun buildUnreasonedResult(
            session: ConversationSession,
            userMessage: String,
            intent: RecognizedIntent,
            retrievedMemories: List<MemoryEntry>,
            stageMillis: MutableMap<String, Long>,
            overallStart: Long,
        ): ExecutionResult {
            val totalMillis = System.currentTimeMillis() - overallStart
            val emptyDecisions =
                ReasoningDecisions(
                    canPerformLocally = false,
                    requiresAI = false,
                    requiresMemory = false,
                    requiresInternet = false,
                    availableTools = emptyList(),
                    missingPermissions = emptyList(),
                    shouldCombineTools = false,
                    shouldSplitTask = false,
                )
            return ExecutionResult(
                responseText = "I ran into a problem understanding that request. Could you try rephrasing it?",
                trace =
                    ExecutionTrace(
                        sessionId = session.id,
                        userMessage = userMessage,
                        recognizedIntent = intent,
                        executionContext =
                            ExecutionContext(
                                availableTools = emptyList(),
                                connectivity = ConnectivityState.Unknown,
                                battery = null,
                                permissionState = emptyMap(),
                                aiProviderAvailable = false,
                                capturedAtMillis = System.currentTimeMillis(),
                            ),
                        reasoningTrace = ReasoningTrace(emptyList()),
                        decisions = emptyDecisions,
                        verdict = ReasoningVerdict.Blocked,
                        selectedAgents = emptyList(),
                        selectedTools = emptyList(),
                        retrievedMemories = retrievedMemories,
                        executionPlan = null,
                        confidence = ConfidenceScore(0f, 0f, 0f, 0f, 0f),
                        timing = TimingMetrics(totalMillis, stageMillis),
                        providerRequired = false,
                        providerConnected = false,
                        alternatives = emptyList(),
                        recoveryPlan = FailureRecoveryPlan(emptyList()),
                    ),
            )
        }

        private suspend inline fun <T> timed(
            stageMillis: MutableMap<String, Long>,
            stage: String,
            block: suspend () -> T,
        ): T {
            val start = System.currentTimeMillis()
            val result = block()
            stageMillis[stage] = System.currentTimeMillis() - start
            return result
        }
    }
