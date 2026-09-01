package com.aura.ai.core.reasoning.model

/** How a single [TraceEntry] resolved. [Blocked] is the only outcome that can force a
 *  [ReasoningVerdict.Blocked] verdict; [Warning] degrades a verdict to
 *  [ReasoningVerdict.ProceedWithWarnings] without stopping reasoning outright. */
enum class TraceOutcome { Satisfied, Warning, Blocked, Info }

/**
 * One explainable step in a reasoning pass — the brief's own requirement, "every reasoning
 * decision must produce an explainable trace," made concrete. [reason] is deliberately a
 * complete, human-readable sentence (e.g. "AI required because request needs content
 * generation.") rather than a code or enum a UI would need to translate — every component in
 * this module that makes a decision produces one or more of these directly.
 */
data class TraceEntry(
    val stage: String,
    val reason: String,
    val outcome: TraceOutcome,
)

/** The full, ordered explanation for one [com.aura.ai.core.reasoning.ReasoningEngine.reason] call. */
data class ReasoningTrace(
    val entries: List<TraceEntry>,
) {
    val hasBlockers: Boolean get() = entries.any { it.outcome == TraceOutcome.Blocked }
    val hasWarnings: Boolean get() = entries.any { it.outcome == TraceOutcome.Warning }

    /** Renders each entry as `"Reason: <sentence>"` — exactly the brief's own example format. */
    fun explain(): List<String> = entries.map { "Reason: ${it.reason}" }
}
