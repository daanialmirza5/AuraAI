# core-capabilities

The bidirectional index between agent names and what they can do. Pure Kotlin/JVM,
dependency-free. See `docs/CAPABILITY_REGISTRY.md` at the project root for the full design
writeup, including a worked example of how it drives agent selection; this file is the quick
reference.

## What lives here

- **`Capability`** — a fixed, closed enum of 11 values (`GoalPlanning`, `MemoryRecall`,
  `MemoryStorage`, `WebSearch`, `CodeGeneration`, `CodeReview`, `CalendarManagement`,
  `DeviceAutomation`, `ShoppingSearch`, `VisionAnalysis`, `Notification`) — every one genuinely
  provided or depended on by this phase's 9 agents, nothing speculative.
- **`CapabilityRegistry`** — `register(agentName, capabilities)` / `agentsFor(capability): Set<String>` /
  `capabilitiesOf(agentName): Set<Capability>`. `DefaultCapabilityRegistry` is two
  `ConcurrentHashMap`s, each the inverse of the other, for O(1) lookups both directions.

## Relationship to `core-agents.AgentRegistry`

Deliberately kept separate: `CapabilityRegistry` only ever deals in `String` names and
`Capability` values — it has never seen a full `Agent` object. `AgentRegistry` (in `core-agents`)
registers into both itself and this registry in one call
(`DefaultAgentRegistry.register`), so `core-orchestrator`'s `AgentSelectionEngine` can resolve
"which agent names provide capability X" without needing the full `Agent` interface for that
question alone.

## Status

Fully real. The one thing this registry makes possible that intent-matching alone couldn't:
`MemoryAgent` has no matching `IntentType` at all, and is only ever selected because
`PlannerAgent` (which matches every intent) declares `dependsOnCapabilities = {MemoryRecall}`,
resolved back to `MemoryAgent` through this registry.
