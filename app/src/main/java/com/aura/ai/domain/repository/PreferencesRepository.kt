package com.aura.ai.domain.repository

import com.aura.ai.domain.model.AppPermission
import com.aura.ai.domain.model.AssistantVoice
import com.aura.ai.domain.model.ThemePreview
import com.aura.ai.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>

    suspend fun setOnboardingComplete()

    suspend fun setAuthenticated(authenticated: Boolean)

    suspend fun setPermissionGranted(
        permission: AppPermission,
        granted: Boolean,
    )

    suspend fun setThemePreview(theme: ThemePreview)

    suspend fun setAssistantVoice(voice: AssistantVoice)

    suspend fun setVoiceContinuousConversationEnabled(enabled: Boolean)
}
