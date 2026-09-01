package com.aura.ai.core.reasoning.constraint

import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.reasoning.model.ConstraintCheckResult
import com.aura.ai.core.reasoning.model.ConstraintType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultConstraintEngine
    @Inject
    constructor(
        private val capabilityResolver: CapabilityResolver,
    ) : ConstraintEngine {
        private companion object {
            /** Friendlier trace wording for tools that come up in the brief's own worked example;
             *  any tool name not listed here falls back to its raw registry name, which is still an
             *  honest, readable label. */
            val FRIENDLY_NAMES: Map<String, String> =
                mapOf(
                    "export_pdf" to "PDF exporter",
                    "save_file" to "file saver",
                    "web_search" to "web search",
                    "notify" to "notifier",
                    "set_reminder" to "reminder scheduler",
                    "create_calendar_event" to "calendar",
                    "navigate" to "navigation",
                    "compose_email" to "email composer",
                    "shopping_search" to "shopping search",
                    "open_app" to "app launcher",
                    "app_automation" to "automation",
                )

            fun friendlyName(toolName: String): String = FRIENDLY_NAMES[toolName] ?: toolName
        }

        override fun evaluate(
            requiredToolNames: List<String>,
            requiresAI: Boolean,
            requiresInternet: Boolean,
            context: ExecutionContext,
        ): List<ConstraintCheckResult> {
            val results = mutableListOf<ConstraintCheckResult>()

            for (toolName in requiredToolNames.distinct()) {
                results += toolAvailability(toolName, context)
                results += permissionChecks(toolName, context)
            }

            results += networkCheck(requiresInternet, context)
            results += batteryCheck(context)

            if (requiresAI) {
                results += aiProviderCheck(context)
            }

            return results
        }

        private fun toolAvailability(
            toolName: String,
            context: ExecutionContext,
        ): ConstraintCheckResult {
            val label = friendlyName(toolName)
            val available = context.hasTool(toolName)
            return ConstraintCheckResult(
                type = ConstraintType.ToolAvailability,
                description = "$label registered",
                satisfied = available,
                detail =
                    if (available) {
                        "$label registered."
                    } else {
                        "$label is not registered — this step cannot run locally."
                    },
            )
        }

        private fun permissionChecks(
            toolName: String,
            context: ExecutionContext,
        ): List<ConstraintCheckResult> {
            val label = friendlyName(toolName)
            val permissions = capabilityResolver.requiredPermissions(toolName)
            if (permissions.isEmpty()) {
                return listOf(
                    ConstraintCheckResult(
                        type = ConstraintType.Permission,
                        description = "$label permissions",
                        satisfied = true,
                        detail =
                            "${label.replaceFirstChar(Char::uppercase)} needs no permission — it writes to the " +
                                "app's own storage or hands off to another app.",
                    ),
                )
            }

            return permissions.map { permission ->
                val state = context.permissionState[permission] ?: PermissionState.Unknown
                val satisfied = state == PermissionState.Granted
                val shortName = permission.substringAfterLast('.')
                ConstraintCheckResult(
                    type = ConstraintType.Permission,
                    description = shortName,
                    satisfied = satisfied,
                    detail =
                        when (state) {
                            PermissionState.Granted -> "$shortName is granted."
                            PermissionState.Denied -> "$shortName is denied — $label cannot run until it's granted."
                            PermissionState.Unknown -> "$shortName's state could not be confirmed — treating $label as unavailable until it is."
                        },
                )
            }
        }

        private fun networkCheck(
            requiresInternet: Boolean,
            context: ExecutionContext,
        ): ConstraintCheckResult =
            if (!requiresInternet) {
                ConstraintCheckResult(
                    type = ConstraintType.Network,
                    description = "Connectivity",
                    satisfied = true,
                    detail = "No network required.",
                )
            } else {
                ConstraintCheckResult(
                    type = ConstraintType.Network,
                    description = "Connectivity",
                    satisfied = context.isOnline,
                    detail =
                        if (context.isOnline) {
                            "Device is online."
                        } else {
                            "This goal needs network access, but the device is offline (or connectivity is unknown)."
                        },
                )
            }

        private fun batteryCheck(context: ExecutionContext): ConstraintCheckResult {
            val battery = context.battery
            return ConstraintCheckResult(
                type = ConstraintType.Battery,
                description = "Battery",
                satisfied = !context.isBatteryConstrained,
                detail =
                    when {
                        battery == null -> "Battery status unknown — proceeding without a battery constraint."
                        context.isBatteryConstrained -> "Battery is low (${battery.percent}%) and not charging — heavy steps may be deferred."
                        else -> "Battery is sufficient (${battery.percent}%${if (battery.isCharging) ", charging" else ""})."
                    },
            )
        }

        private fun aiProviderCheck(context: ExecutionContext): ConstraintCheckResult =
            ConstraintCheckResult(
                type = ConstraintType.AiProvider,
                description = "AI provider",
                satisfied = context.aiProviderAvailable,
                detail =
                    if (context.aiProviderAvailable) {
                        "A connected AI provider is available."
                    } else {
                        "No AI provider is currently connected — steps that need generation will fail until one is."
                    },
            )
    }
