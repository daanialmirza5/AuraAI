# core-agents

AURA's 9 agents and the `Agent` contract they all implement. Pure Kotlin/JVM; depends on
`core-ai`, `core-intent`, `core-tools`, `core-memory`, `core-providers`, `core-planner`,
`core-reasoning`, `core-events`, `core-capabilities`. See `docs/AGENT_SYSTEM.md` at the project
root for the full architecture writeup; this file is the implementation-level reference.

## What lives here

```
com.aura.ai.core.agents/
├── Agent.kt              the interface every agent implements, + AgentTask/AgentResult/AgentHealth
├── SharedContext.kt        the cross-agent blackboard ("Context Sharing")
├── ToolBackedAgent.kt       shared base for the 5 agents that wrap exactly one Tool
├── registry/                AgentRegistry — the agent-name -> Agent catalog
└── impl/                    the 9 concrete agents
```

### The 9 agents

`PlannerAgent`, `MemoryAgent`, `ResearchAgent`, `CodingAgent`, `CalendarAgent`,
`AutomationAgent`, `ShoppingAgent`, `VisionAgent`, `NotificationAgent`. Full table of each one's
capabilities/tools/permissions/intents: `docs/AGENT_SYSTEM.md §3`. Every one of the 9 wraps
already-real Phase 3–5 machinery (`Planner`, `core-memory`'s retrieval/extraction, registered
`Tool`s, `AIProviderManager`) — none of them are new fake logic, they're a new, uniform way of
invoking what already existed.

### No agent depends on another agent

Every constructor dependency an agent takes is on a lower module. The only two channels between
agents are `SharedContext` (read/write, one instance per orchestration pass) and
`core-events.EventBus` (fire-and-forget). There is no import from one `impl/*Agent.kt` file to
another — this is enforced by what each file actually imports, not just documented as a rule.

### `ToolBackedAgent`

5 of the 9 agents (`ResearchAgent`, `CalendarAgent`, `AutomationAgent`, `ShoppingAgent`,
`NotificationAgent`) do their real work by invoking exactly one registered `core-tools.Tool`.
`ToolBackedAgent` writes `health()` (checks the tool is registered + required permissions are
granted, via `core-reasoning`'s real, Android-backed `PermissionChecker`) and `runTool()` (invoke
+ publish `ToolExecutedEvent`) once, instead of five times.

### What's genuinely scaffolded

`CodingAgent` and `VisionAgent` cannot succeed yet — both honestly, for different reasons.
`CodingAgent` constructs and sends a real `GenerationRequest`; it fails only because every
`AIProvider` is still scaffolded. `VisionAgent` fails before attempting anything: `GenerationRequest`
has no image field at all, so there's no request to even build. Full detail: `docs/AGENT_SYSTEM.md §3`.

## Status

The full agent layer — all 9 agents, `AgentRegistry`, `SharedContext`, health checks, capability
declarations — runs today, entirely locally. Two agents (`CodingAgent`, `VisionAgent`) have an
honest, unavoidable gap: genuine AI generation. Nothing here fakes success or hides that gap.
