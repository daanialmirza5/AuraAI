package com.aura.ai.ai.di

import com.aura.ai.core.orchestrator.AgentOrchestrator
import com.aura.ai.core.orchestrator.DefaultAgentOrchestrator
import com.aura.ai.core.orchestrator.aggregation.DefaultResultAggregator
import com.aura.ai.core.orchestrator.aggregation.ResultAggregator
import com.aura.ai.core.orchestrator.coordination.DefaultExecutionCoordinator
import com.aura.ai.core.orchestrator.coordination.ExecutionCoordinator
import com.aura.ai.core.orchestrator.dispatch.DefaultTaskDispatcher
import com.aura.ai.core.orchestrator.dispatch.TaskDispatcher
import com.aura.ai.core.orchestrator.selection.AgentSelectionEngine
import com.aura.ai.core.orchestrator.selection.DefaultAgentSelectionEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiOrchestratorModule {
    @Binds
    @Singleton
    abstract fun bindAgentSelectionEngine(impl: DefaultAgentSelectionEngine): AgentSelectionEngine

    @Binds
    @Singleton
    abstract fun bindTaskDispatcher(impl: DefaultTaskDispatcher): TaskDispatcher

    @Binds
    @Singleton
    abstract fun bindExecutionCoordinator(impl: DefaultExecutionCoordinator): ExecutionCoordinator

    @Binds
    @Singleton
    abstract fun bindResultAggregator(impl: DefaultResultAggregator): ResultAggregator

    @Binds
    @Singleton
    abstract fun bindAgentOrchestrator(impl: DefaultAgentOrchestrator): AgentOrchestrator
}
