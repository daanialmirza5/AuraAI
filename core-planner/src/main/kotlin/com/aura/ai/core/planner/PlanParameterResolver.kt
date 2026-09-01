package com.aura.ai.core.planner

/**
 * Substitutes `${step:<id>.output}` references in a step's parameters with a prior step's actual
 * output, so "Export PDF" can consume what "Generate" produced. This is pure data transformation
 * — the executor that walks an [ExecutionPlan] step by step (in the app module, once it has both
 * a plan and an `ActionEngine` to run it with) calls this before invoking each step's tool.
 */
object PlanParameterResolver {
    private val REFERENCE = Regex("""\$\{step:([\w-]+)\.output}""")

    fun resolve(
        parameters: Map<String, String>,
        priorOutputs: Map<String, String>,
    ): Map<String, String> =
        parameters.mapValues { (_, value) ->
            REFERENCE.replace(value) { match ->
                val stepId = match.groupValues[1]
                priorOutputs[stepId] ?: match.value
            }
        }
}
