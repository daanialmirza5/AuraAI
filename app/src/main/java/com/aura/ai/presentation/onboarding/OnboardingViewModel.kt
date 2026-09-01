package com.aura.ai.presentation.onboarding

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Immutable
data class OnboardingSlide(
    val art: OnboardingArt,
    val kicker: String,
    val title: String,
    val body: String,
)

private val Slides =
    listOf(
        OnboardingSlide(
            OnboardingArt.Presence,
            "Presence",
            "Meet AURA",
            "A reasoning core that sees your day, your devices, and your intent — and acts on all three.",
        ),
        OnboardingSlide(
            OnboardingArt.Memory,
            "Memory",
            "It remembers what matters",
            "Long-term memory keeps context across weeks and projects — no re-explaining yourself.",
        ),
        OnboardingSlide(
            OnboardingArt.Control,
            "Control",
            "One voice, every system",
            "From smart home to calendar to code — a single assistant orchestrates it all.",
        ),
        OnboardingSlide(
            OnboardingArt.Trust,
            "Trust",
            "You stay in command",
            "Every permission, memory, and automation is visible and reversible — always your call.",
        ),
    )

@Immutable
data class OnboardingUiState(
    val step: Int = 0,
    val slides: List<OnboardingSlide> = Slides,
) {
    val current: OnboardingSlide get() = slides[step]
    val isLastStep: Boolean get() = step >= slides.lastIndex
    val ctaLabel: String get() = if (isLastStep) "Get Started" else "Continue"
}

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        private val _finished = MutableStateFlow(false)
        val finished: StateFlow<Boolean> = _finished.asStateFlow()

        fun onNext() {
            if (_uiState.value.isLastStep) {
                _finished.value = true
            } else {
                _uiState.update { it.copy(step = it.step + 1) }
            }
        }

        fun onSkip() {
            _finished.value = true
        }
    }
