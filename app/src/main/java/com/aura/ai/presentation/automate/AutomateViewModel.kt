package com.aura.ai.presentation.automate

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.ai.workflow.WorkflowManager
import com.aura.ai.core.actions.ActionEngine
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.domain.model.Automation
import com.aura.ai.domain.model.Device
import com.aura.ai.domain.model.Workflow
import com.aura.ai.domain.repository.AutomationRepository
import com.aura.ai.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AutomateUiState(
    val sub: AutomateSub = AutomateSub.Automation,
    val automations: ImmutableList<Automation> = persistentListOf(),
    val devices: ImmutableList<Device> = persistentListOf(),
    val workflows: ImmutableList<Workflow> = persistentListOf(),
    /** The result text of the last Quick Action tap (or workflow run), or `null` — transient,
     *  dismissed by [AutomateViewModel.dismissQuickActionResult] or the next tap/run. */
    val quickActionResult: String? = null,
)

@HiltViewModel
class AutomateViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val automationRepository: AutomationRepository,
        private val deviceRepository: DeviceRepository,
        private val actionEngine: ActionEngine,
        private val workflowManager: WorkflowManager,
    ) : ViewModel() {
        private val sub = MutableStateFlow(AutomateSub.fromKey(savedStateHandle["sub"]))
        private val quickActionResult = MutableStateFlow<String?>(null)

        private data class CoreFlags(
            val sub: AutomateSub,
            val automations: ImmutableList<Automation>,
            val devices: ImmutableList<Device>,
            val workflows: ImmutableList<Workflow>,
        )

        // combine() only has typed overloads up to 5 flows — quickActionResult is folded in with a
        // second, 2-flow combine rather than reaching for the untyped vararg overload, the same
        // pattern AuraTabViewModel already established.
        private val coreFlags =
            combine(
                sub,
                automationRepository.observeAutomations().map { it.toPersistentList() },
                deviceRepository.observeDevices().map { it.toPersistentList() },
                workflowManager.observeWorkflows().map { it.toPersistentList() },
            ) { sub, automations, devices, workflows -> CoreFlags(sub, automations, devices, workflows) }

        val uiState: StateFlow<AutomateUiState> =
            combine(coreFlags, quickActionResult) { core, result ->
                AutomateUiState(
                    sub = core.sub,
                    automations = core.automations,
                    devices = core.devices,
                    workflows = core.workflows,
                    quickActionResult = result,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutomateUiState())

        fun setSub(value: AutomateSub) {
            sub.value = value
        }

        fun toggleAutomation(id: String) {
            viewModelScope.launch { automationRepository.toggleAutomation(id) }
        }

        fun toggleDevice(id: String) {
            viewModelScope.launch { deviceRepository.toggleDevice(id) }
        }

        /** Runs a tool directly, bypassing [com.aura.ai.core.tools.Tool.requiresConfirmation] — valid
         *  here specifically because this is called from a direct button tap, never autonomously. See
         *  `docs/ANDROID_AUTOMATION.md` §3. */
        fun runQuickAction(
            toolName: String,
            arguments: Map<String, String> = emptyMap(),
        ) {
            viewModelScope.launch {
                val result = actionEngine.executeConfirmed(toolName, arguments)
                quickActionResult.value =
                    when (result) {
                        is AuraResult.Success -> result.value.summary
                        is AuraResult.Failure -> result.error.message
                    }
            }
        }

        fun addWorkflow(workflow: Workflow) {
            viewModelScope.launch {
                workflowManager.addWorkflow(workflow)
                quickActionResult.value = "Added \"${workflow.name}\"."
            }
        }

        fun setWorkflowEnabled(
            workflow: Workflow,
            enabled: Boolean,
        ) {
            viewModelScope.launch { workflowManager.setEnabled(workflow, enabled) }
        }

        fun deleteWorkflow(workflow: Workflow) {
            viewModelScope.launch { workflowManager.deleteWorkflow(workflow) }
        }

        fun runWorkflowNow(workflow: Workflow) {
            viewModelScope.launch {
                val result = workflowManager.runNow(workflow)
                quickActionResult.value =
                    when (result) {
                        is AuraResult.Success -> "\"${workflow.name}\" ran successfully."
                        is AuraResult.Failure -> "\"${workflow.name}\" failed: ${result.error.message}"
                    }
            }
        }

        fun dismissQuickActionResult() {
            quickActionResult.value = null
        }
    }
