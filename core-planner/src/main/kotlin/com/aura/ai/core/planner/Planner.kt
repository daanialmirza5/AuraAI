package com.aura.ai.core.planner

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.intent.RecognizedIntent

/**
 * Breaks a goal into an [ExecutionPlan]. [TemplatePlanner] is the only implementation this phase
 * ships — fixed, hand-authored step sequences per [com.aura.ai.core.intent.IntentType], the same
 * role a router/rules-engine plays in front of an LLM planner in a production agent system. A
 * future `LlmPlanner`, backed by `AIProviderManager`, can decompose goals dynamically instead of
 * from a template — swapping it in requires no change to anything that calls [Planner.plan].
 */
interface Planner {
    suspend fun plan(
        goal: String,
        intent: RecognizedIntent,
        availableTools: List<ToolDescriptor> = emptyList(),
    ): AuraResult<ExecutionPlan>
}
