package com.aura.ai.ai.di

import com.aura.ai.core.agents.Agent
import com.aura.ai.core.agents.impl.AutomationAgent
import com.aura.ai.core.agents.impl.CalendarAgent
import com.aura.ai.core.agents.impl.CodingAgent
import com.aura.ai.core.agents.impl.MemoryAgent
import com.aura.ai.core.agents.impl.NotificationAgent
import com.aura.ai.core.agents.impl.PlannerAgent
import com.aura.ai.core.agents.impl.ResearchAgent
import com.aura.ai.core.agents.impl.ShoppingAgent
import com.aura.ai.core.agents.impl.VisionAgent
import com.aura.ai.core.agents.registry.AgentRegistry
import com.aura.ai.core.agents.registry.DefaultAgentRegistry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Every [Agent] AURA ships with — all 9 land in the same `Set<Agent>`, which
 * [com.aura.ai.AuraApplication] registers into the shared [AgentRegistry] at startup, the exact
 * pattern [AiToolsModule] already established for `Set<Tool>`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiAgentsModule {
    @Binds
    @Singleton
    abstract fun bindAgentRegistry(impl: DefaultAgentRegistry): AgentRegistry

    @Binds
    @IntoSet
    abstract fun bindPlannerAgent(impl: PlannerAgent): Agent

    @Binds
    @IntoSet
    abstract fun bindMemoryAgent(impl: MemoryAgent): Agent

    @Binds
    @IntoSet
    abstract fun bindResearchAgent(impl: ResearchAgent): Agent

    @Binds
    @IntoSet
    abstract fun bindCodingAgent(impl: CodingAgent): Agent

    @Binds
    @IntoSet
    abstract fun bindCalendarAgent(impl: CalendarAgent): Agent

    @Binds
    @IntoSet
    abstract fun bindAutomationAgent(impl: AutomationAgent): Agent

    @Binds
    @IntoSet
    abstract fun bindShoppingAgent(impl: ShoppingAgent): Agent

    @Binds
    @IntoSet
    abstract fun bindVisionAgent(impl: VisionAgent): Agent

    @Binds
    @IntoSet
    abstract fun bindNotificationAgent(impl: NotificationAgent): Agent
}
