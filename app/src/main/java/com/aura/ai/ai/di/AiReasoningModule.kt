package com.aura.ai.ai.di

import com.aura.ai.ai.reasoning.AndroidBatteryStatusProvider
import com.aura.ai.ai.reasoning.AndroidNetworkStatusProvider
import com.aura.ai.ai.reasoning.AndroidPermissionChecker
import com.aura.ai.core.reasoning.DefaultReasoningEngine
import com.aura.ai.core.reasoning.ReasoningEngine
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.capability.DefaultCapabilityResolver
import com.aura.ai.core.reasoning.confidence.ConfidenceEvaluator
import com.aura.ai.core.reasoning.confidence.DefaultConfidenceEvaluator
import com.aura.ai.core.reasoning.constraint.ConstraintEngine
import com.aura.ai.core.reasoning.constraint.DefaultConstraintEngine
import com.aura.ai.core.reasoning.context.BatteryStatusProvider
import com.aura.ai.core.reasoning.context.DefaultExecutionContextProvider
import com.aura.ai.core.reasoning.context.ExecutionContextProvider
import com.aura.ai.core.reasoning.context.NetworkStatusProvider
import com.aura.ai.core.reasoning.context.PermissionChecker
import com.aura.ai.core.reasoning.decision.DecisionEngine
import com.aura.ai.core.reasoning.decision.DefaultDecisionEngine
import com.aura.ai.core.reasoning.decomposition.RuleBasedTaskDecomposer
import com.aura.ai.core.reasoning.decomposition.TaskDecomposer
import com.aura.ai.core.reasoning.goal.DefaultGoalManager
import com.aura.ai.core.reasoning.goal.GoalManager
import com.aura.ai.core.reasoning.validation.ActionValidator
import com.aura.ai.core.reasoning.validation.DefaultActionValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Phase 5 bindings — the Reasoning Engine and everything it's built from. [PermissionChecker],
 * [NetworkStatusProvider], and [BatteryStatusProvider] are the one part of core-reasoning that
 * can't have a real default *inside* core-reasoning (it's deliberately pure Kotlin/JVM, no
 * `Context`) — this is the one place in the whole app that binds them to the real,
 * Android-backed implementations rather than core-reasoning's own honest "unknown" fallbacks
 * (`UnknownPermissionChecker`, `UnknownNetworkStatusProvider`, `UnknownBatteryStatusProvider`).
 * Everything else here is the same one-line-swap pattern as [AiCoreModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiReasoningModule {
    @Binds
    @Singleton
    abstract fun bindPermissionChecker(impl: AndroidPermissionChecker): PermissionChecker

    @Binds
    @Singleton
    abstract fun bindNetworkStatusProvider(impl: AndroidNetworkStatusProvider): NetworkStatusProvider

    @Binds
    @Singleton
    abstract fun bindBatteryStatusProvider(impl: AndroidBatteryStatusProvider): BatteryStatusProvider

    @Binds
    @Singleton
    abstract fun bindExecutionContextProvider(impl: DefaultExecutionContextProvider): ExecutionContextProvider

    @Binds
    @Singleton
    abstract fun bindCapabilityResolver(impl: DefaultCapabilityResolver): CapabilityResolver

    @Binds
    @Singleton
    abstract fun bindGoalManager(impl: DefaultGoalManager): GoalManager

    @Binds
    @Singleton
    abstract fun bindTaskDecomposer(impl: RuleBasedTaskDecomposer): TaskDecomposer

    @Binds
    @Singleton
    abstract fun bindConstraintEngine(impl: DefaultConstraintEngine): ConstraintEngine

    @Binds
    @Singleton
    abstract fun bindConfidenceEvaluator(impl: DefaultConfidenceEvaluator): ConfidenceEvaluator

    @Binds
    @Singleton
    abstract fun bindDecisionEngine(impl: DefaultDecisionEngine): DecisionEngine

    @Binds
    @Singleton
    abstract fun bindActionValidator(impl: DefaultActionValidator): ActionValidator

    @Binds
    @Singleton
    abstract fun bindReasoningEngine(impl: DefaultReasoningEngine): ReasoningEngine
}
