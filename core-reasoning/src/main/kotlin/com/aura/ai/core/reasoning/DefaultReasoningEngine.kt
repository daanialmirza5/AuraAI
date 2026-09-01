package com.aura.ai.core.reasoning

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.intent.RecognizedIntent
import com.aura.ai.core.planner.ExecutionPlan
import com.aura.ai.core.planner.Planner
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.confidence.ConfidenceEvaluator
import com.aura.ai.core.reasoning.constraint.ConstraintEngine
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.ExecutionContextProvider
import com.aura.ai.core.reasoning.decision.DecisionEngine
import com.aura.ai.core.reasoning.decomposition.TaskDecomposer
import com.aura.ai.core.reasoning.goal.ActiveGoal
import com.aura.ai.core.reasoning.goal.GoalManager
import com.aura.ai.core.reasoning.model.AlternativePlan
import com.aura.ai.core.reasoning.model.ConfidenceInput
import com.aura.ai.core.reasoning.model.ConstraintCheckResult
import com.aura.ai.core.reasoning.model.ConstraintType
import com.aura.ai.core.reasoning.model.FailureRecoveryPlan
import com.aura.ai.core.reasoning.model.ReasoningDecisions
import com.aura.ai.core.reasoning.model.ReasoningOutcome
import com.aura.ai.core.reasoning.model.ReasoningTrace
import com.aura.ai.core.reasoning.model.ReasoningVerdict
import com.aura.ai.core.reasoning.model.RecoveryAction
import com.aura.ai.core.reasoning.model.SubGoal
import com.aura.ai.core.reasoning.model.TraceEntry
import com.aura.ai.core.reasoning.model.TraceOutcome
import com.aura.ai.core.reasoning.validation.ActionValidator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The default [ReasoningEngine]. Runs every stage the brief names, in the order its own worked
 * example lays out — [GoalManager] ("understand goal") -> [TaskDecomposer] ("determine missing
 * information" starts here) -> [CapabilityResolver]/[ExecutionContextProvider] (what's actually
 * available right now) -> [DecisionEngine] (the 8 questions) -> [ConstraintEngine] (checked
 * against the goal) -> [ConfidenceEvaluator] -> `Planner.plan` ("generate execution plan") ->
 * [ActionValidator] (checked against the actual plan) — then assembles one [ReasoningTrace]
 * narrating all of it, and a [ReasoningVerdict] that can never claim more confidence than that
 * trace supports.
 */
@Singleton
class DefaultReasoningEngine
    @Inject
    constructor(
        private val goalManager: GoalManager,
        private val taskDecomposer: TaskDecomposer,
        private val capabilityResolver: CapabilityResolver,
        private val executionContextProvider: ExecutionContextProvider,
        private val decisionEngine: DecisionEngine,
        private val constraintEngine: ConstraintEngine,
        private val confidenceEvaluator: ConfidenceEvaluator,
        private val actionValidator: ActionValidator,
        private val planner: Planner,
    ) : ReasoningEngine {
        private companion object {
            const val VAGUE_GOAL_WORD_COUNT = 1
        }

        override suspend fun reason(
            goal: String,
            intent: RecognizedIntent,
        ): AuraResult<ReasoningOutcome> {
            val activeGoalResult = goalManager.establishGoal(goal)
            val activeGoal =
                when (activeGoalResult) {
                    is AuraResult.Success -> activeGoalResult.value
                    is AuraResult.Failure -> return activeGoalResult
                }

            val subGoals = taskDecomposer.decompose(goal, intent)
            val impliedTools = subGoals.mapNotNull { it.impliedToolName }.distinct()
            val relevantPermissions = capabilityResolver.requiredPermissions(impliedTools).toSet()

            val context = executionContextProvider.capture(relevantPermissions)
            val hasRelevantMemory = activeGoal.relatedMemories.isNotEmpty()

            val decisions = decisionEngine.decide(goal, subGoals, context, hasRelevantMemory)
            val preflightConstraints =
                constraintEngine.evaluate(
                    requiredToolNames = impliedTools,
                    requiresAI = decisions.requiresAI,
                    requiresInternet = decisions.requiresInternet,
                    context = context,
                )

            val confidence =
                confidenceEvaluator.evaluate(
                    ConfidenceInput(
                        intentConfidence = intent.confidence,
                        decisions = decisions,
                        constraints = preflightConstraints,
                        subGoals = subGoals,
                        hasRelevantMemory = hasRelevantMemory,
                    ),
                )

            val planResult = planner.plan(goal, intent, context.availableTools)
            val executionPlan = (planResult as? AuraResult.Success)?.value

            val validationConstraints = executionPlan?.let { actionValidator.validate(it, context) }.orEmpty()
            val allConstraints = preflightConstraints + validationConstraints

            val isVague = goal.trim().split(Regex("\\s+")).count { it.isNotBlank() } <= VAGUE_GOAL_WORD_COUNT
            val trace =
                buildTrace(goal, activeGoal, subGoals, decisions, preflightConstraints, executionPlan, planResult, validationConstraints)
            val verdict = determineVerdict(trace, isVague)

            return AuraResult.Success(
                ReasoningOutcome(
                    goal = goal,
                    intent = intent,
                    verdict = verdict,
                    decisions = decisions,
                    subGoals = subGoals,
                    confidence = confidence,
                    constraints = allConstraints,
                    alternatives = buildAlternatives(decisions, context),
                    recoveryPlan = buildRecoveryPlan(allConstraints),
                    executionPlan = executionPlan,
                    trace = trace,
                ),
            )
        }

        private fun determineVerdict(
            trace: ReasoningTrace,
            isVague: Boolean,
        ): ReasoningVerdict =
            when {
                trace.hasBlockers -> ReasoningVerdict.Blocked
                isVague -> ReasoningVerdict.NeedsMoreInformation
                trace.hasWarnings -> ReasoningVerdict.ProceedWithWarnings
                else -> ReasoningVerdict.Proceed
            }

        private fun buildTrace(
            goal: String,
            activeGoal: ActiveGoal,
            subGoals: List<SubGoal>,
            decisions: ReasoningDecisions,
            preflightConstraints: List<ConstraintCheckResult>,
            executionPlan: ExecutionPlan?,
            planResult: AuraResult<ExecutionPlan>,
            validationConstraints: List<ConstraintCheckResult>,
        ): ReasoningTrace {
            val entries = mutableListOf<TraceEntry>()

            entries +=
                TraceEntry(
                    stage = "Understand goal",
                    reason =
                        "Goal understood: \"$goal\". " +
                            if (activeGoal.relatedMemories.isEmpty()) {
                                "No related memories found."
                            } else {
                                "Found ${activeGoal.relatedMemories.size} related memor${if (activeGoal.relatedMemories.size == 1) "y" else "ies"}."
                            },
                    outcome = TraceOutcome.Info,
                )

            entries +=
                if (decisions.requiresMemory && activeGoal.relatedMemories.isEmpty()) {
                    TraceEntry(
                        stage = "Determine missing information",
                        reason = "This goal references personal context, but no related memory was found — some information may be missing.",
                        outcome = TraceOutcome.Warning,
                    )
                } else {
                    TraceEntry(
                        stage = "Determine missing information",
                        reason = "No missing information detected.",
                        outcome = TraceOutcome.Info,
                    )
                }

            val researchGoal = subGoals.firstOrNull { it.impliedToolName == "web_search" }
            entries +=
                TraceEntry(
                    stage = "Research required?",
                    reason =
                        if (researchGoal != null) {
                            "Research required — the plan includes a web search step."
                        } else {
                            "No research step required."
                        },
                    outcome = TraceOutcome.Info,
                )

            entries +=
                TraceEntry(
                    stage = "AI generation required?",
                    reason =
                        if (decisions.requiresAI) {
                            "AI required because request needs content generation."
                        } else {
                            "No AI generation required — every step maps to a local tool."
                        },
                    outcome = TraceOutcome.Info,
                )
            if (decisions.requiresAI) {
                preflightConstraints.firstOrNull { it.type == ConstraintType.AiProvider }?.let { aiConstraint ->
                    entries +=
                        TraceEntry(
                            stage = "AI provider available?",
                            reason = aiConstraint.detail,
                            outcome = if (aiConstraint.satisfied) TraceOutcome.Satisfied else TraceOutcome.Warning,
                        )
                }
            }

            val exportGoal = subGoals.firstOrNull { it.impliedToolName == "export_pdf" }
            val saveGoal = subGoals.firstOrNull { it.impliedToolName == "save_file" }
            entries +=
                TraceEntry(
                    stage = "Export required?",
                    reason =
                        if (exportGoal != null) {
                            "Export required — the plan includes an export step."
                        } else {
                            "No export step required."
                        },
                    outcome = TraceOutcome.Info,
                )

            if (exportGoal != null || saveGoal != null) {
                val storagePermissions =
                    capabilityResolver.requiredPermissions(
                        listOfNotNull(exportGoal?.impliedToolName, saveGoal?.impliedToolName),
                    )
                entries +=
                    TraceEntry(
                        stage = "Storage permission?",
                        reason =
                            if (storagePermissions.isEmpty()) {
                                "Storage available — no permission is required; files are written to the app's own private storage."
                            } else {
                                "Storage requires: ${storagePermissions.joinToString()}."
                            },
                        outcome = TraceOutcome.Satisfied,
                    )
            }

            preflightConstraints
                .filter { it.type == ConstraintType.ToolAvailability }
                .forEach { entries += TraceEntry("Tool availability", it.detail, it.outcomeFor()) }

            preflightConstraints
                .filter { it.type == ConstraintType.Network }
                .forEach { entries += TraceEntry("Connectivity", it.detail, it.outcomeFor()) }

            preflightConstraints
                .filter { it.type == ConstraintType.Battery }
                .forEach { entries += TraceEntry("Battery", it.detail, it.outcomeFor()) }

            entries +=
                when (planResult) {
                    is AuraResult.Success ->
                        TraceEntry(
                            stage = "Generate execution plan",
                            reason = "Execution plan generated with ${executionPlan?.steps?.size ?: 0} step(s).",
                            outcome = TraceOutcome.Satisfied,
                        )
                    is AuraResult.Failure ->
                        TraceEntry(
                            stage = "Generate execution plan",
                            reason = "Failed to generate an execution plan: ${planResult.error.message}",
                            outcome = TraceOutcome.Blocked,
                        )
                }

            validationConstraints
                .filterNot { it.satisfied }
                .forEach { entries += TraceEntry("Plan validation", it.detail, it.outcomeFor()) }

            return ReasoningTrace(entries)
        }

        /** [ConstraintType.Permission] and [ConstraintType.ToolAvailability] failures mean a step
         *  literally cannot run — a hard [TraceOutcome.Blocked]. Everything else (network, battery,
         *  a missing AI provider) degrades a plan without necessarily stopping every step in it, so
         *  it's a [TraceOutcome.Warning] instead. */
        private fun ConstraintCheckResult.outcomeFor(): TraceOutcome =
            when {
                satisfied -> TraceOutcome.Satisfied
                type == ConstraintType.Permission || type == ConstraintType.ToolAvailability -> TraceOutcome.Blocked
                else -> TraceOutcome.Warning
            }

        private fun buildAlternatives(
            decisions: ReasoningDecisions,
            context: ExecutionContext,
        ): List<AlternativePlan> =
            if (decisions.requiresAI && !context.aiProviderAvailable) {
                listOf(
                    AlternativePlan(
                        description =
                            "Produce the local steps only (research, export, save, notify) and leave the " +
                                "AI-generated content as a placeholder for the user to fill in.",
                        tradeoff = "No AI-written content — the user has to write the body themselves until a provider is connected.",
                    ),
                )
            } else {
                emptyList()
            }

        private fun buildRecoveryPlan(constraints: List<ConstraintCheckResult>): FailureRecoveryPlan {
            val actions = constraints.filterNot { it.satisfied }.map { it.toRecoveryAction() }
            return FailureRecoveryPlan(actions)
        }

        private fun ConstraintCheckResult.toRecoveryAction(): RecoveryAction =
            when (type) {
                ConstraintType.Permission -> RecoveryAction("Grant the missing permission: $description.", actionable = true)
                ConstraintType.Network -> RecoveryAction("Reconnect to the internet and try again.", actionable = true)
                ConstraintType.Battery -> RecoveryAction("Charge the device before running battery-intensive steps.", actionable = true)
                ConstraintType.AiProvider ->
                    RecoveryAction(
                        "Connect an AI provider once one is available — none is functional yet in this build.",
                        actionable = false,
                    )
                ConstraintType.ToolAvailability ->
                    RecoveryAction(
                        "Register a tool that can handle: $description.",
                        actionable = false,
                    )
            }
    }
