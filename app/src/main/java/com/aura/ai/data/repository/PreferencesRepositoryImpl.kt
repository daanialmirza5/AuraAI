package com.aura.ai.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aura.ai.domain.model.AppPermission
import com.aura.ai.domain.model.AssistantVoice
import com.aura.ai.domain.model.ThemePreview
import com.aura.ai.domain.model.UserPreferences
import com.aura.ai.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private object Keys {
    val OnboardingComplete = booleanPreferencesKey("onboarding_complete")
    val IsAuthenticated = booleanPreferencesKey("is_authenticated")
    val ThemePreview = stringPreferencesKey("theme_preview")
    val AssistantVoice = stringPreferencesKey("assistant_voice")
    val VoiceContinuousConversation = booleanPreferencesKey("voice_continuous_conversation")

    fun permission(permission: AppPermission) = booleanPreferencesKey("perm_${permission.storageKey}")
}

@Singleton
class PreferencesRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : PreferencesRepository {
        override fun observePreferences(): Flow<UserPreferences> =
            dataStore.data.map { prefs ->
                UserPreferences(
                    onboardingComplete = prefs[Keys.OnboardingComplete] ?: false,
                    isAuthenticated = prefs[Keys.IsAuthenticated] ?: false,
                    grantedPermissions =
                        AppPermission.entries.filterTo(mutableSetOf()) { permission ->
                            prefs[Keys.permission(permission)] ?: false
                        },
                    themePreview =
                        prefs[Keys.ThemePreview]
                            ?.let { runCatching { ThemePreview.valueOf(it) }.getOrNull() }
                            ?: ThemePreview.Dark,
                    assistantVoice =
                        prefs[Keys.AssistantVoice]
                            ?.let { runCatching { AssistantVoice.valueOf(it) }.getOrNull() }
                            ?: AssistantVoice.Jarvis,
                    voiceContinuousConversationEnabled = prefs[Keys.VoiceContinuousConversation] ?: true,
                )
            }

        override suspend fun setOnboardingComplete() {
            dataStore.edit { it[Keys.OnboardingComplete] = true }
        }

        override suspend fun setAuthenticated(authenticated: Boolean) {
            dataStore.edit { it[Keys.IsAuthenticated] = authenticated }
        }

        override suspend fun setPermissionGranted(
            permission: AppPermission,
            granted: Boolean,
        ) {
            dataStore.edit { it[Keys.permission(permission)] = granted }
        }

        override suspend fun setThemePreview(theme: ThemePreview) {
            dataStore.edit { it[Keys.ThemePreview] = theme.name }
        }

        override suspend fun setAssistantVoice(voice: AssistantVoice) {
            dataStore.edit { it[Keys.AssistantVoice] = voice.name }
        }

        override suspend fun setVoiceContinuousConversationEnabled(enabled: Boolean) {
            dataStore.edit { it[Keys.VoiceContinuousConversation] = enabled }
        }
    }
