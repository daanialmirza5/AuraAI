package com.aura.ai.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.core.navigation.AuraRoute
import com.aura.ai.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Matches the source's 2.6s boot sequence (auraBoot 2.4s fill + auraFlicker settle). */
private const val BOOT_DURATION_MILLIS = 2600L

@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        private val _nextRoute = MutableStateFlow<String?>(null)
        val nextRoute: StateFlow<String?> = _nextRoute

        init {
            viewModelScope.launch {
                delay(BOOT_DURATION_MILLIS)
                val prefs = preferencesRepository.observePreferences().first()
                _nextRoute.value =
                    when {
                        !prefs.onboardingComplete -> AuraRoute.ONBOARDING
                        !prefs.isAuthenticated -> AuraRoute.LOGIN
                        else -> AuraRoute.APP_SHELL
                    }
            }
        }
    }
