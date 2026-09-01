package com.aura.ai.core.reasoning.model

/**
 * The one-line summary of a whole reasoning pass, derived from the worst
 * [ConstraintCheckResult]/[TraceEntry] outcome seen — never set directly, always computed, so it
 * can never say something rosier than the trace underneath it actually supports.
 */
enum class ReasoningVerdict {
    /** Every constraint satisfied — the generated plan can run as-is. */
    Proceed,

    /** The plan can still run, but at least one non-fatal gap exists (e.g. AI required but no
     *  provider connected yet — the local steps still work, the AI-dependent step won't). */
    ProceedWithWarnings,

    /** The goal itself is too vague to decompose or act on — more input is needed before
     *  anything downstream (planning, execution) is worth attempting. */
    NeedsMoreInformation,

    /** A hard constraint failed with no local workaround (e.g. a required permission is denied
     *  and every tool that could satisfy the goal needs it). */
    Blocked,
}
