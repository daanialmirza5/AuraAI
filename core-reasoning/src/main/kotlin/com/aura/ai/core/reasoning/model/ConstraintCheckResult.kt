package com.aura.ai.core.reasoning.model

/** What kind of real-world constraint a [ConstraintCheckResult] is reporting on. */
enum class ConstraintType { Permission, Network, Battery, ToolAvailability, AiProvider }

/**
 * One pass/fail check against reality — "storage available," "PDF exporter registered," "no
 * network required" are all exactly this shape. Both `ConstraintEngine` (checked before planning)
 * and `ActionValidator` (checked against the actual generated plan) produce these, so a caller
 * sees the same vocabulary regardless of which stage flagged something.
 */
data class ConstraintCheckResult(
    val type: ConstraintType,
    /** A short label, e.g. "Storage permission" — not the full sentence; see [detail] for that. */
    val description: String,
    val satisfied: Boolean,
    /** The full explanation, written to be used directly as a [TraceEntry.reason]. */
    val detail: String,
)
