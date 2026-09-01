package com.aura.ai.core.di

import com.aura.ai.data.repository.AutomationRepositoryImpl
import com.aura.ai.data.repository.ChatRepositoryImpl
import com.aura.ai.data.repository.DeviceRepositoryImpl
import com.aura.ai.data.repository.MemoryRepositoryImpl
import com.aura.ai.data.repository.PreferencesRepositoryImpl
import com.aura.ai.data.repository.TodoRepositoryImpl
import com.aura.ai.data.repository.WorkflowRepositoryImpl
import com.aura.ai.domain.repository.AutomationRepository
import com.aura.ai.domain.repository.ChatRepository
import com.aura.ai.domain.repository.DeviceRepository
import com.aura.ai.domain.repository.MemoryRepository
import com.aura.ai.domain.repository.PreferencesRepository
import com.aura.ai.domain.repository.TodoRepository
import com.aura.ai.domain.repository.WorkflowRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindAutomationRepository(impl: AutomationRepositoryImpl): AutomationRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMemoryRepository(impl: MemoryRepositoryImpl): MemoryRepository

    @Binds
    @Singleton
    abstract fun bindTodoRepository(impl: TodoRepositoryImpl): TodoRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindWorkflowRepository(impl: WorkflowRepositoryImpl): WorkflowRepository
}
