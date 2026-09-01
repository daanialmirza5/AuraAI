package com.aura.ai.core.capabilities

/**
 * A named ability an `com.aura.ai.core.agents.Agent` can provide — the vocabulary
 * `CapabilityRegistry` indexes agents by, and the same vocabulary an agent uses to declare what
 * it needs from *other* agents via `Agent.dependsOnCapabilities` (see
 * `com.aura.ai.core.orchestrator.coordination.ExecutionCoordinator` for how that drives
 * dependency ordering). Deliberately a fixed, closed enum rather than a free-form string: every
 * capability here is one this phase's 9 agents genuinely provide or depend on — nothing is listed
 * speculatively.
 */
enum class Capability {
    GoalPlanning,
    MemoryRecall,
    MemoryStorage,
    WebSearch,
    CodeGeneration,
    CodeReview,
    CalendarManagement,
    DeviceAutomation,
    ShoppingSearch,
    VisionAnalysis,
    Notification,
}
