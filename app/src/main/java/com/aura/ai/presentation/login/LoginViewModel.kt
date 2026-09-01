package com.aura.ai.presentation.login

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class LoginUiState(
    val isAuthenticating: Boolean = false,
    val errorMessage: String? = null,
    val authenticated: Boolean = false,
)

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun onAuthenticationStarted() {
            _uiState.update { it.copy(isAuthenticating = true, errorMessage = null) }
        }

        fun onAuthenticationSucceeded() {
            viewModelScope.launch {
                preferencesRepository.setAuthenticated(true)
                _uiState.update { it.copy(isAuthenticating = false, authenticated = true) }
            }
        }

        /** A hard error (cancelled, too many attempts, hardware unavailable) — ends the attempt. */
        fun onAuthenticationError(message: String) {
            _uiState.update { it.copy(isAuthenticating = false, errorMessage = message) }
        }

        /** A single non-matching biometric read — the system prompt itself stays open for retry. */
        fun onAuthenticationFailed() {
            _uiState.update { it.copy(errorMessage = "Not recognized — try again") }
        }
    }
