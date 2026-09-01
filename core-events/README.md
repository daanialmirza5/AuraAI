# core-events

AURA's internal event bus — "every module communicates using events." Pure Kotlin/JVM, genuinely
dependency-free (not even `core-ai`). See `docs/EVENT_BUS.md` at the project root for the full
design writeup; this file is the quick reference.

## What lives here

- **`AuraEvent`** — a sealed hierarchy of 14 events, each carrying only primitive data
  (`String`/`Int`/`Float`/`Boolean`), never a rich type from another module:
  `IntentRecognizedEvent`, `MemoryRetrievedEvent`, `PlanCreatedEvent`, `ToolExecutedEvent`,
  `ProviderConnectedEvent`, `AgentCompletedEvent`, `TaskFailedEvent`, `PermissionDeniedEvent`
  (Phase 6), plus `PluginLoadedEvent`, `PluginEnabledEvent`, `PluginDisabledEvent`,
  `PluginUnloadedEvent`, `PluginFailedEvent`, `PluginHealthChangedEvent` (Phase 7, published by
  `plugin-runtime` — see `docs/PLUGIN_RUNTIME.md §6`).
- **`EventBus`** — `publish(event)` / `events(): Flow<AuraEvent>`, plus a reified `on<T>()`
  extension for subscribing to one event type. `DefaultEventBus` wraps one `MutableSharedFlow`
  with no replay and a bounded, drop-oldest buffer — publishing never blocks on a slow subscriber.

## Why no dependencies

Every event is primitive-only specifically so `core-events` never needs to depend on `core-intent`,
`core-memory`, `core-planner`, or anything else to describe what happened in those modules. That's
what lets `core-agents` and `core-orchestrator` (and, in principle, any future module) depend on
`core-events` with zero risk of a dependency cycle.

## Status

Fully real — this is a working, general-purpose pub/sub bus, not scaffolding. `core-agents`
(agent-specific events like `MemoryRetrievedEvent`/`PlanCreatedEvent`/`ToolExecutedEvent`),
`core-orchestrator` (lifecycle events like `AgentCompletedEvent`/`TaskFailedEvent`/
`PermissionDeniedEvent`/`IntentRecognizedEvent`), and `plugin-runtime` (the 6 `Plugin*Event`
cases) are its publishers. `ProviderConnectedEvent` is defined and checked-for but never actually
fires yet — no `AIProvider` in this codebase reports `isAvailable() == true`.
