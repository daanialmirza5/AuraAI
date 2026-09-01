package com.aura.ai.ai.di

import com.aura.ai.core.events.DefaultEventBus
import com.aura.ai.core.events.EventBus
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiEventsModule {
    @Binds
    @Singleton
    abstract fun bindEventBus(impl: DefaultEventBus): EventBus
}
