# core-planner

Breaks a goal into an ordered, dependency-linked `ExecutionPlan`. Pure Kotlin/JVM; depends on
core-ai and core-intent.

## What lives here

- **`PlanStep`** — one node: an `id`, a `description`, an optional `toolName` (`null` means "no
  local tool covers this, ask the AI provider directly"), `parameters`, `dependsOn`, `status`.
- **`ExecutionPlan`** — a `goal` plus its `steps`.
- **`PlanParameterResolver`** — resolves `${step:<id>.output}` references in a step's parameters
  against prior steps' actual outputs. Pure data transformation; no execution happens here.
- **`Planner`** — the interface. **`TemplatePlanner`** is the only implementation this phase
  ships: fixed, hand-authored step sequences per `IntentType`.

## The worked example, exactly as specified

`TemplatePlanner` recognizes "creation" goals (the goal text mentions assignment / report /
essay / document / paper, paired with a Research/Coding/Unknown intent) and produces:

```
1. Research the topic         -> tool: web_search
2. Generate the content       -> tool: null (requires a connected AI provider)
3. Export as PDF              -> tool: export_pdf   (content = ${step:2.output})
4. Save the file              -> tool: save_file    (source = ${step:3.output})
5. Notify the user            -> tool: notify       (message = ${step:4.output})
```

Every intent besides that resolves to a single step calling the one tool that maps onto it
(`open_app`, `set_reminder`, `create_calendar_event`, `navigate`, `compose_email`,
`web_search`, `shopping_search`, `app_automation`). `Coding` and `Conversation` steps have no
tool at all — see below.

## Running this today

The app's `PlanExecutor` (in the app module, since it's the one place that needs both this
module's `ExecutionPlan` and core-actions' `ActionEngine`) will correctly execute steps 1, 3, 4,
5 above — and correctly, honestly fail at step 2, since no AI provider is connected in this
phase. That's not a bug in the executor; it's the accurate state of the system today.

## Status

Fully implemented and real. A future `LlmPlanner`, backed by `AIProviderManager`, could
decompose goals dynamically instead of from a fixed template — swapping it in is a one-line DI
change (see the app's `AiCoreModule`), and nothing that calls `Planner.plan()` would need to change.
