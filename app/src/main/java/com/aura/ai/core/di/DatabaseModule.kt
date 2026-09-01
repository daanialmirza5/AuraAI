package com.aura.ai.core.di

import android.content.Context
import androidx.room.Room
import com.aura.ai.data.local.AuraDatabase
import com.aura.ai.data.local.dao.AutomationDao
import com.aura.ai.data.local.dao.ChatMessageDao
import com.aura.ai.data.local.dao.DeviceDao
import com.aura.ai.data.local.dao.MemoryDao
import com.aura.ai.data.local.dao.TodoDao
import com.aura.ai.data.local.dao.WorkflowDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAuraDatabase(
        @ApplicationContext context: Context,
    ): AuraDatabase =
        Room
            .databaseBuilder(context, AuraDatabase::class.java, AuraDatabase.DATABASE_NAME)
            // No real migrations exist yet (pre-1.0, no install base to preserve) — destructive
            // fallback is the honest choice over crashing on every future schema bump, exactly
            // the same "no migrations to justify more machinery yet" call already made for
            // exportSchema in AuraDatabase.kt.
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideDeviceDao(database: AuraDatabase): DeviceDao = database.deviceDao()

    @Provides
    fun provideAutomationDao(database: AuraDatabase): AutomationDao = database.automationDao()

    @Provides
    fun provideChatMessageDao(database: AuraDatabase): ChatMessageDao = database.chatMessageDao()

    @Provides
    fun provideMemoryDao(database: AuraDatabase): MemoryDao = database.memoryDao()

    @Provides
    fun provideTodoDao(database: AuraDatabase): TodoDao = database.todoDao()

    @Provides
    fun provideWorkflowDao(database: AuraDatabase): WorkflowDao = database.workflowDao()
}
