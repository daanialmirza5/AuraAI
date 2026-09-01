package com.aura.ai.ai

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import com.aura.ai.domain.repository.AutomationRepository
import com.aura.ai.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The "Automation" intent's concrete execution — and the one [Tool] that couldn't live in
 * core-actions, because there's nothing generic to automate without the app's own
 * [DeviceRepository]/[AutomationRepository]. This is exactly the point of [com.aura.ai.core.tools.ToolRegistry]
 * being a plain registry rather than something core-actions owns outright: the app contributes
 * its own tools alongside the generic Android ones, and core-planner never has to know the
 * difference — it only ever sees a tool name and a [com.aura.ai.core.ai.ToolDescriptor].
 *
 * Not a new feature: it's a new *entry point* onto the Smart Home / Automations screens that
 * already ship — toggling a real, already-persisted device or automation, nothing invented.
 */
class AppAutomationTool
    @Inject
    constructor(
        private val deviceRepository: DeviceRepository,
        private val automationRepository: AutomationRepository,
    ) : Tool {
        override val name = "app_automation"
        override val description = "Toggles a smart home device or automation already configured in AURA."
        override val parameters =
            mapOf(
                "request" to
                    ParameterSchema(
                        ParameterType.String,
                        "The device or automation name to toggle, e.g. \"Living Room Lights\"",
                        required = true,
                    ),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val request = arguments["request"]?.trim().orEmpty()
            if (request.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("request is required."))
            }

            val devices = deviceRepository.observeDevices().first()
            val matchedDevice = devices.firstOrNull { it.name.contains(request, ignoreCase = true) }
            if (matchedDevice != null) {
                deviceRepository.toggleDevice(matchedDevice.id)
                return AuraResult.Success(ToolResult(summary = "Toggled ${matchedDevice.name}."))
            }

            val automations = automationRepository.observeAutomations().first()
            val matchedAutomation = automations.firstOrNull { it.name.contains(request, ignoreCase = true) }
            if (matchedAutomation != null) {
                automationRepository.toggleAutomation(matchedAutomation.id)
                return AuraResult.Success(ToolResult(summary = "Toggled ${matchedAutomation.name}."))
            }

            return AuraResult.Failure(AuraError.InvalidRequest("No device or automation matches \"$request\"."))
        }
    }
