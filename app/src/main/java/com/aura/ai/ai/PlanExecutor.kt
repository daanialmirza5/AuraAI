package com.aura.ai.ai

import com.aura.ai.core.actions.ActionEngine
import com.aura.ai.core.ai.AiMessage
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.GenerationRequest
import com.aura.ai.core.ai.MessageRole
import com.aura.ai.core.planner.ExecutionPlan
import com.aura.ai.core.planner.PlanParameterResolver
import com.aura.ai.core.planner.PlanStep
import com.aura.ai.core.providers.AIProviderManager
import com.aura.ai.core.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one piece that necessarily lives in the app module rather than in a core module: it's the
 * only thing that needs *both* core-planner's [ExecutionPlan] (portable, Android-unaware) and
 * core-actions' [ActionEngine] (Android-aware, deliberately unaware of what a "plan" is) — gluing
 * decoupled core modules together is exactly what the composition root is for.
 *
 * Walks a plan's [PlanStep]s in dependency order (not just list order — a step only runs once
 * every step in its [PlanStep.dependsOn] has completed), resolving `${step:<id>.output}`
 * references via [PlanParameterResolver] as it goes. A step with no [PlanStep.toolName] is
 * handed to [AIProviderManager] instead of [ActionEngine] — which, with no provider connected
 * yet, correctly and honestly fails rather than fabricating a result.
 */
@Singleton
class PlanExecutor
    @Inject
    constructor(
        private val actionEngine: ActionEngine,
        private val providerManager: AIProviderManager,
    ) {
        suspend fun run(plan: ExecutionPlan): AuraResult<Map<String, ToolResult>> {
            val outputs = mutableMapOf<String, String>()
            val results = mutableMapOf<String, ToolResult>()
            val remaining = plan.steps.toMutableList()

            while (remaining.isNotEmpty()) {
                val ready = remaining.filter { step -> step.dependsOn.all { outputs.containsKey(it) } }
                if (ready.isEmpty()) {
                    val stuckIds = remaining.joinToString { it.id }
                    return AuraResult.Failure(AuraError.Unknown("Plan has unresolved dependencies for step(s): $stuckIds"))
                }

                for (step in ready) {
                    val result = runStep(step, outputs)
                    when (result) {
                        is AuraResult.Success -> {
                            outputs[step.id] = result.value.data["path"] ?: result.value.summary
                            results[step.id] = result.value
                        }
                        is AuraResult.Failure -> return AuraResult.Failure(result.error)
                    }
                    remaining.remove(step)
                }
            }
            return AuraResult.Success(results)
        }

        private suspend fun runStep(
            step: PlanStep,
            priorOutputs: Map<String, String>,
        ): AuraResult<ToolResult> {
            val resolvedParameters = PlanParameterResolver.resolve(step.parameters, priorOutputs)
            val toolName = step.toolName
            if (toolName != null) {
                return actionEngine.execute(toolName, resolvedParameters)
            }

            // No local tool covers this step — it genuinely needs the active AI provider.
            val request = GenerationRequest(messages = listOf(AiMessage(MessageRole.User, step.description)))
            return when (val generation = providerManager.generate(request)) {
                is AuraResult.Success -> AuraResult.Success(ToolResult(summary = generation.value.text))
                is AuraResult.Failure -> AuraResult.Failure(generation.error)
            }
        }
    }
