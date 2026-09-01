# core-orchestrator

The Agent Orchestrator — selects, coordinates, retries, and aggregates AURA's 9 agents. Pure
Kotlin/JVM; depends on `core-ai`, `core-intent`, `core-providers`, `core-events`,
`core-capabilities`, `core-agents` (never `core-tools`/`core-memory`/`core-planner`/
`core-reasoning` directly — this module only ever sees those concerns through the `Agent`
abstraction). See `docs/ORCHESTRATOR.md` and `docs/EXECUTION_PIPELINE.md` at the project root for
the full design writeup and a complete worked example; this file is the quick reference.

## What lives here

```
com.aura.ai.core.orchestrator/
├── AgentOrchestrator.kt / DefaultAgentOrchestrator.kt   the single entry point
├── OrchestrationResult.kt
├── selection/       AgentSelectionEngine — "which agents are needed"
├── dispatch/         TaskDispatcher — health-gated, retried, timed-out single-agent execution
├── coordination/      ExecutionCoordinator — parallel/sequential/dependency-ordered waves
└── aggregation/        ResultAggregator — confidence aggregation
```

### `AgentOrchestrator.orchestrate(goal, additionalAgentNames = emptySet())`

The one method that composes everything: recognizes intent, selects agents (intent match plus
capability-dependency closure), coordinates their execution, aggregates the results — all in one
call, starting from raw text.

### One algorithm, three execution modes

`ExecutionCoordinator` runs agents in dependency-respecting waves — every agent within a wave in
parallel, waves themselves in sequence. No selected agent has a dependency → one wave, fully
parallel. A chain of agents each depending on the previous one's capability → one agent per wave,
fully sequential. Independent agents share a wave; dependent ones wait — the common, mixed case.
Real topological ordering off `Agent.dependsOnCapabilities`, not a hardcoded special case.

### Retries and timeouts

`TaskDispatcher` checks `Agent.health()` before attempting anything (skipping entirely on
`AgentHealthStatus.Unavailable`), then retries up to `DispatchPolicy.maxAttempts` times, each
attempt wrapped in `withTimeoutOrNull(timeoutMillis)`. Every outcome — health gate, each attempt,
final result — publishes the relevant `core-events` event.

## Status

Fully real. Every component runs entirely locally: agent selection, dependency-ordered parallel
coordination, retry/timeout handling, and confidence-weighted result aggregation. The only honest
gap is inherited from `core-agents`, not introduced here — `CodingAgent`/`VisionAgent` still fail
without a connected AI provider, and the orchestrator reports that faithfully (`TaskFailedEvent`,
a non-empty `AggregatedResult.failed`) rather than hiding it.
