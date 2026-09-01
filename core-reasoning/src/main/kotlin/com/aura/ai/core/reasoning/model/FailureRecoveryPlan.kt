package com.aura.ai.core.reasoning.model

/** One concrete step that would resolve an unsatisfied [ConstraintCheckResult] — "grant the
 *  Notifications permission," "connect an AI provider in Settings," "retry once back online." */
data class RecoveryAction(
    val description: String,
    /** True if this is something the user/app can actually do right now (grant a permission,
     *  reconnect to Wi-Fi); false if it's out of AURA's hands this phase (e.g. "wait for a future
     *  phase to connect a provider") — kept distinct so a UI knows what to offer as a button
     *  versus what to just explain. */
    val actionable: Boolean,
)

/** The full recovery plan for whatever [ConstraintCheckResult]s came back unsatisfied — empty
 *  when nothing did. One [RecoveryAction] per unsatisfied constraint, in the same order. */
data class FailureRecoveryPlan(
    val actions: List<RecoveryAction>,
) {
    val isEmpty: Boolean get() = actions.isEmpty()
}
