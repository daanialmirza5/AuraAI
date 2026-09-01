package com.aura.ai.presentation.permissions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.domain.model.AppPermission
import com.aura.ai.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PermissionsUiState(
    val granted: Map<AppPermission, Boolean> = AppPermission.entries.associateWith { false },
    val finished: Boolean = false,
)

@HiltViewModel
class PermissionsViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PermissionsUiState())
        val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

        fun setPermissionGranted(
            permission: AppPermission,
            granted: Boolean,
        ) {
            _uiState.update { it.copy(granted = it.granted + (permission to granted)) }
        }

        fun onActivate() {
            viewModelScope.launch {
                val state = _uiState.value
                state.granted.forEach { (permission, granted) ->
                    preferencesRepository.setPermissionGranted(permission, granted)
                }
                preferencesRepository.setOnboardingComplete()
                _uiState.update { it.copy(finished = true) }
            }
        }
    }
