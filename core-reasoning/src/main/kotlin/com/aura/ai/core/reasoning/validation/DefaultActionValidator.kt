package com.aura.ai.core.reasoning.validation

import com.aura.ai.core.planner.ExecutionPlan
import com.aura.ai.core.planner.PlanStep
import com.aura.ai.core.reasoning.capability.CapabilityResolver
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.context.PermissionState
import com.aura.ai.core.reasoning.model.ConstraintCheckResult
import com.aura.ai.core.reasoning.model.ConstraintType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultActionValidator
    @Inject
    constructor(
        private val capabilityResolver: CapabilityResolver,
    ) : ActionValidator {
        override fun validate(
            plan: ExecutionPlan,
            context: ExecutionContext,
        ): List<ConstraintCheckResult> = plan.steps.flatMap { step -> validateStep(step, context) }

        private fun validateStep(
            step: PlanStep,
            context: ExecutionContext,
        ): List<ConstraintCheckResult> {
            val toolName = step.toolName
            if (toolName == null) {
                return listOf(
                    ConstraintCheckResult(
                        type = ConstraintType.AiProvider,
                        description = "Step ${step.id}: ${step.description}",
                        satisfied = context.aiProviderAvailable,
                        detail =
                            if (context.aiProviderAvailable) {
                                "Step ${step.id} (\"${step.description}\") has a connected AI provider to run against."
                            } else {
                                "Step ${step.id} (\"${step.description}\") has no local tool and no AI provider is connected — it will fail until one is."
                            },
                    ),
                )
            }

            val results = mutableListOf<ConstraintCheckResult>()
            val registered = context.hasTool(toolName)
            results +=
                ConstraintCheckResult(
                    type = ConstraintType.ToolAvailability,
                    description = "Step ${step.id}: $toolName",
                    satisfied = registered,
                    detail =
                        if (registered) {
                            "Step ${step.id} uses \"$toolName\", which is registered."
                        } else {
                            "Step ${step.id} uses \"$toolName\", which is not registered — this step cannot run."
                        },
                )

            for (permission in capabilityResolver.requiredPermissions(toolName)) {
                val state = context.permissionState[permission] ?: PermissionState.Unknown
                val satisfied = state == PermissionState.Granted
                val shortName = permission.substringAfterLast('.')
                results +=
                    ConstraintCheckResult(
                        type = ConstraintType.Permission,
                        description = "Step ${step.id}: $shortName",
                        satisfied = satisfied,
                        detail =
                            if (satisfied) {
                                "Step ${step.id} has $shortName granted."
                            } else {
                                "Step ${step.id} needs $shortName, which is not confirmed granted."
                            },
                    )
            }

            return results
        }
    }
