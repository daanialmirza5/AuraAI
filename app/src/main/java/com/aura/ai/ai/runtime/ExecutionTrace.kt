package com.aura.ai.ai.runtime

import com.aura.ai.core.intent.RecognizedIntent
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.planner.ExecutionPlan
import com.aura.ai.core.reasoning.context.ExecutionContext
import com.aura.ai.core.reasoning.model.AlternativePlan
import com.aura.ai.core.reasoning.model.ConfidenceScore
import com.aura.ai.core.reasoning.model.FailureRecoveryPlan
import com.aura.ai.core.reasoning.model.ReasoningDecisions
import com.aura.ai.core.reasoning.model.ReasoningTrace
import com.aura.ai.core.reasoning.model.ReasoningVerdict

/** How long each pipeline stage took, plus the total — "Timing Metrics." Stage names match
 *  [ConversationPipeline]'s own internal stage labels, so a trace reader can line the two up. */
data class TimingMetrics(
    val totalMillis: Long,
    val stageMillis: Map<String, Long>,
)

/**
 * Everything one [ConversationPipeline] run produced, beyond the reply text itself — the brief's
 * own list, verbatim: [reasoningTrace] ("Reasoning Trace"), [selectedAgents] ("Selected Agents"),
 * [selectedTools] ("Selected Tools"), [retrievedMemories] ("Retrieved Memories"), [executionPlan]
 * ("Execution Plan"), [timing] ("Timing Metrics"), [confidence] ("Confidence Score") — this whole
 * type together is the "Execution Trace." Never rendered directly in the normal chat UI (see
 * `docs/EXECUTION_TRACE.md`); it exists for developer mode.
 */
data class ExecutionTrace(
    val sessionId: String,
    val userMessage: String,
    val recognizedIntent: RecognizedIntent,
    val executionContext: ExecutionContext,
    val reasoningTrace: ReasoningTrace,
    val decisions: ReasoningDecisions,
    val verdict: ReasoningVerdict,
    val selectedAgents: List<String>,
    val selectedTools: List<String>,
    val retrievedMemories: List<MemoryEntry>,
    val executionPlan: ExecutionPlan?,
    val confidence: ConfidenceScore,
    val timing: TimingMetrics,
    val providerRequired: Boolean,
    val providerConnected: Boolean,
    val alternatives: List<AlternativePlan>,
    val recoveryPlan: FailureRecoveryPlan,
)
