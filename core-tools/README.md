# core-tools

The catalog of invokable capabilities — "open_app", "set_reminder", "web_search", and so on.
Pure Kotlin/JVM; depends only on core-ai.

## What lives here

- **`Tool`** — one capability: a name, a description, a parameter schema, and a suspend
  `execute(arguments)` returning an `AuraResult<ToolResult>`.
- **`ToolRegistry`** — the live, mutable catalog of every `Tool` currently available.
  `DefaultToolRegistry` is a thread-safe, in-memory `ConcurrentHashMap`-backed implementation.
- **`ToolResult`** — what a tool hands back: a human/model-readable `summary` plus structured
  `data`.

## Where the actual tools are

This module defines the *contract*, not any concrete tool. The real, Android-backed
implementations (open an app, set a reminder, create a calendar event, ...) live in
**core-actions**, which depends on this module. The app module registers its own tools here too
(see `AppAutomationTool` in the app's `com.aura.ai.ai` package) — the whole point of a plain
registry rather than a hard-coded list is that both a lower module (core-actions) and the app
itself can contribute to the same catalog without knowing about each other.

## Who reads this

- core-planner reads `ToolRegistry.descriptors()` (well — `Tool.toDescriptor()`, wherever a
  caller actually has tools in hand) to know what a plan can reference.
- The app's `PlanExecutor` reads `ToolRegistry` (indirectly, via `ActionEngine`) to resolve a
  `PlanStep.toolName` into something it can actually run.
- `AuraApplication.onCreate()` is where every `Tool` this app ships with gets registered, via a
  Hilt `Set<Tool>` multibinding populated by core-actions' `AiToolsModule` (app module) equivalent.

## Status

Fully implemented and real — the registry itself has no "not connected yet" caveat. What's
registered *into* it (see core-actions) is what's split between real and provider-dependent.
