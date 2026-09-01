package com.aura.ai.core.planner

/** The full decomposition of one user goal into an ordered, dependency-linked set of [PlanStep]s. */
data class ExecutionPlan(
    val id: String,
    val goal: String,
    val steps: List<PlanStep>,
    val createdAtMillis: Long,
) {
    fun step(id: String): PlanStep? = steps.firstOrNull { it.id == id }

    fun withStep(updated: PlanStep): ExecutionPlan = copy(steps = steps.map { if (it.id == updated.id) updated else it })
}
