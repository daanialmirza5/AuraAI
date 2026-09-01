# core-reasoning

AURA's reasoning layer — sits between `core-intent`'s `IntentRecognizer` and `core-planner`'s
`Planner`, deciding whether and how a goal should be acted on before a plan is generated, and
validating what comes back once one is. Pure Kotlin/JVM; depends on `core-ai`, `core-intent`,
`core-tools`, `core-memory`, `core-providers`, and `core-planner`. See `docs/REASONING_ENGINE.md`
at the project root for the full architecture writeup (component map, sequence diagrams, the
brief's worked example traced end to end); this file is the implementation-level,
package-by-package reference.

## What lives here

```
com.aura.ai.core.reasoning/
├── ReasoningEngine.kt / DefaultReasoningEngine.kt   the orchestrator — composes everything below
├── model/            ReasoningTrace, ReasoningDecisions, ConfidenceScore, SubGoal,
│                     ConstraintCheckResult, AlternativePlan, FailureRecoveryPlan,
│                     ReasoningVerdict, ReasoningOutcome
├── context/          ExecutionContext + ExecutionContextProvider, plus the
│                     PermissionChecker / NetworkStatusProvider / BatteryStatusProvider
│                     abstractions and their honest "unknown" defaults
├── capability/        CapabilityResolver — which tools exist, what each one needs
├── goal/              GoalManager — "understand goal," grounded in core-memory
├── decomposition/      TaskDecomposer — goal -> ordered SubGoals
├── constraint/         ConstraintEngine — pre-plan pass/fail checks
├── confidence/         ConfidenceEvaluator — 4-factor confidence scoring
├── decision/           DecisionEngine — the brief's 8 questions, answered in one call
└── validation/         ActionValidator — post-plan pass/fail checks against the real plan
```

### The orchestrator

`DefaultReasoningEngine.reason(goal, intent)` runs every stage above in the order the brief's own
worked example lays out — understand the goal, decompose it, capture the device's current state,
answer the 8 questions, check constraints, score confidence, generate (and validate) a real
`com.aura.ai.core.planner.ExecutionPlan` — then assembles one `ReasoningTrace` narrating all of it
and a `ReasoningVerdict` that can never claim more confidence than that trace supports. Full
sequence diagram and a complete worked-example trace: `docs/EXECUTION_FLOW.md`.

### Explainable trace, by design

Every component that makes a decision produces one or more `TraceEntry`s — a `stage`, a complete
human-readable `reason` sentence, and an `outcome` (`Satisfied`/`Warning`/`Blocked`/`Info`). This
isn't a logging afterthought bolted onto the end; `ConstraintCheckResult.detail` and
`ReasoningDecisions`' narrative in `DefaultReasoningEngine` are written *as* trace sentences from
the start — `"Reason: AI required because request needs content generation."`,
`"Reason: Storage available."`, `"Reason: PDF exporter registered."` are the brief's own example,
produced verbatim by this code, not paraphrased after the fact.

### Two orthogonal "what/how" layers

`SubGoal` (this module) and `com.aura.ai.core.planner.PlanStep` (Phase 3) look similar but answer
different questions at different times: `SubGoal` is *what* needs to happen, decided before any
tool is chosen; `PlanStep` is *how*, decided by `Planner` during planning. `TaskDecomposer`
doesn't read `Planner`'s output or vice versa — both independently recognize the same real request
shapes (the document-creation pipeline) because reasoning has to make its 8 decisions *before* it
knows whether to invoke planning at all. Full detail: `docs/GOAL_DECOMPOSITION.md`.

### Permission/network/battery: real, not scaffolded

Unlike embeddings or AI providers, checking a permission, checking connectivity, and checking
battery level need no cloud connection — they're plain, already-buildable Android capability. This
module's own defaults (`UnknownPermissionChecker`, `UnknownNetworkStatusProvider`,
`UnknownBatteryStatusProvider`) are honest "I can't check, I have no `Context`" placeholders — pure
Kotlin/JVM, no Android dependency, consistent with every other core module except `core-actions`
— but `:app`'s `AiReasoningModule` binds real, working implementations instead
(`AndroidPermissionChecker`, `AndroidNetworkStatusProvider`, `AndroidBatteryStatusProvider`, all
under `app/src/main/java/com/aura/ai/ai/reasoning/`). Full detail, including the exact
permission/network requirement map for all 11 of AURA's registered tools: `docs/CONSTRAINT_ENGINE.md`.

## Relationship to `core-planner`

`core-reasoning` depends on `core-planner`, never the other way around — `Planner.plan` is the
literal last step `DefaultReasoningEngine` takes ("generate execution plan," the brief's own
worked example). `core-planner` has no idea `core-reasoning` exists, and never needs to: reasoning
is a consumer of planning, sitting in front of it, not a replacement for it.

## Status

The full pipeline — goal understanding, decomposition, the 8 decisions, constraint checking,
confidence scoring, plan generation and validation, alternative planning, failure recovery
planning — runs today, entirely locally, with no network calls. Permission, network, and battery
awareness are genuinely real once `:app`'s bindings are in the graph (they always are — see
`AiReasoningModule`), not placeholders. The one honest, deliberate gap: a `SubGoal`/`PlanStep` that
needs AI generation still fails until a real `AIProvider` is connected — reasoning's job is to
predict, explain, and offer an alternative for that gap in advance, not to hide it or fake it.
