package com.aura.ai.presentation.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.domain.repository.AutomationRepository
import com.aura.ai.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val sub: HomeSub = HomeSub.Dashboard,
    val onlineDeviceCount: Int = 0,
    val totalDeviceCount: Int = 0,
    val activeAutomationCount: Int = 0,
    val timeline: List<TimelineItem> = HomeSampleContent.timeline,
    val notifications: List<NotificationSummaryItem> = HomeSampleContent.notifications,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        deviceRepository: DeviceRepository,
        automationRepository: AutomationRepository,
    ) : ViewModel() {
        private val sub = MutableStateFlow(HomeSub.Dashboard)

        private val counts =
            combine(
                deviceRepository.observeDevices(),
                automationRepository.observeAutomations(),
            ) { devices, automations ->
                Triple(devices.count { it.isOn }, devices.size, automations.count { it.isOn })
            }

        val uiState: StateFlow<HomeUiState> =
            combine(sub, counts) { sub, (online, total, activeAutomations) ->
                HomeUiState(
                    sub = sub,
                    onlineDeviceCount = online,
                    totalDeviceCount = total,
                    activeAutomationCount = activeAutomations,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

        fun setSub(value: HomeSub) {
            sub.value = value
        }
    }
