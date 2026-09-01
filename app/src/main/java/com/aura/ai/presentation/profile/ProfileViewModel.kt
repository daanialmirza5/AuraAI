package com.aura.ai.presentation.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.core.ai.AIProvider
import com.aura.ai.core.providers.AIProviderManager
import com.aura.ai.core.providers.ProviderCredentialStore
import com.aura.ai.core.providers.ProviderId
import com.aura.ai.domain.model.AssistantVoice
import com.aura.ai.domain.model.ThemePreview
import com.aura.ai.domain.repository.MemoryRepository
import com.aura.ai.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ProfileUiState(
    val sub: ProfileSub = ProfileSub.Profile,
    val themePreview: ThemePreview = ThemePreview.Dark,
    val assistantVoice: AssistantVoice = AssistantVoice.Jarvis,
    val voiceContinuousConversationEnabled: Boolean = true,
    val signedOut: Boolean = false,
    val providerRows: List<ProviderSettingsRow> = emptyList(),
)

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val preferencesRepository: PreferencesRepository,
        private val providerManager: AIProviderManager,
        private val credentialStore: ProviderCredentialStore,
        private val memoryRepository: MemoryRepository,
    ) : ViewModel() {
        private val sub = MutableStateFlow(ProfileSub.fromKey(savedStateHandle["sub"]))
        private val signedOut = MutableStateFlow(false)

        /** Bumped after every credential/base-url change to force [providerRows] to re-derive —
         *  [ProviderCredentialStore] is a plain suspend get/set, not a reactive store, so nothing else
         *  would otherwise tell this flow a provider's connection state may have changed. */
        private val refreshTick = MutableStateFlow(0)

        private val providerRows: Flow<List<ProviderSettingsRow>> =
            combine(
                providerManager.activeProvider,
                refreshTick,
            ) { active, _ -> buildProviderRows(active) }

        val uiState: StateFlow<ProfileUiState> =
            combine(
                sub,
                preferencesRepository.observePreferences(),
                signedOut,
                providerRows,
            ) { sub, prefs, signedOut, providerRows ->
                ProfileUiState(
                    sub = sub,
                    themePreview = prefs.themePreview,
                    assistantVoice = prefs.assistantVoice,
                    voiceContinuousConversationEnabled = prefs.voiceContinuousConversationEnabled,
                    signedOut = signedOut,
                    providerRows = providerRows,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

        private suspend fun buildProviderRows(active: AIProvider): List<ProviderSettingsRow> =
            providerManager.availableProviders().map { provider ->
                val id = ProviderId.entries.first { it.key == provider.id }
                ProviderSettingsRow(
                    id = id,
                    displayName = provider.displayName,
                    isLocal = provider.capabilities.isLocal,
                    connected = provider.isAvailable(),
                    isActive = provider.id == active.id,
                    baseUrl = if (id == ProviderId.Ollama) credentialStore.baseUrlOverride(id) else null,
                )
            }

        fun setSub(value: ProfileSub) {
            sub.value = value
        }

        fun setThemePreview(theme: ThemePreview) {
            viewModelScope.launch { preferencesRepository.setThemePreview(theme) }
        }

        fun setAssistantVoice(voice: AssistantVoice) {
            viewModelScope.launch { preferencesRepository.setAssistantVoice(voice) }
        }

        fun setVoiceContinuousConversationEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.setVoiceContinuousConversationEnabled(enabled) }
        }

        fun setProviderApiKey(
            id: ProviderId,
            apiKey: String,
        ) {
            viewModelScope.launch {
                credentialStore.setApiKey(id, apiKey.trim().ifBlank { null })
                refreshTick.update { it + 1 }
            }
        }

        fun setProviderBaseUrl(
            id: ProviderId,
            baseUrl: String,
        ) {
            viewModelScope.launch {
                credentialStore.setBaseUrlOverride(id, baseUrl.trim().ifBlank { null })
                refreshTick.update { it + 1 }
            }
        }

        fun setActiveProvider(id: ProviderId) {
            providerManager.selectProvider(id)
        }

        fun signOut() {
            viewModelScope.launch {
                preferencesRepository.setAuthenticated(false)
                signedOut.value = true
            }
        }

        /** Irreversible — the Profile screen only calls this after the user confirms an explicit
         *  dialog, matching the "explicit confirmation before any destructive action" rule the rest
         *  of the app's destructive tools already follow (see `docs/ANDROID_AUTOMATION.md` §3). */
        fun clearAllMemory() {
            viewModelScope.launch { memoryRepository.forgetAll() }
        }
    }
