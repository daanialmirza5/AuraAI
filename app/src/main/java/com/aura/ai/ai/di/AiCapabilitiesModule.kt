package com.aura.ai.ai.di

import com.aura.ai.core.capabilities.CapabilityRegistry
import com.aura.ai.core.capabilities.DefaultCapabilityRegistry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiCapabilitiesModule {
    @Binds
    @Singleton
    abstract fun bindCapabilityRegistry(impl: DefaultCapabilityRegistry): CapabilityRegistry
}
