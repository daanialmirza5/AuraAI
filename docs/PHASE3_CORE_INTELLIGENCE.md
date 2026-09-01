# AURA AI — Phase 3: Core Intelligence Architecture

**What this phase is:** the internal AI architecture that will power AURA — intent recognition,
planning, tool execution, memory, and a provider-abstraction layer — built as 7 new Gradle
modules, with zero connection to any cloud AI provider. **What this phase is not:** a working
chatbot. Every seam where a real model would plug in is real, typed, and DI-wired; none of them
make a network call yet.

Each module also has its own `README.md` with implementation-level detail; this document is the
cross-cutting picture — how the modules fit together, what actually runs end-to-end today, and
what "connecting a provider" (a later phase) will and won't touch.

---

## 1. Module map

```
                      ┌─────────────┐
                      │   core-ai   │   foundation: AuraResult, AuraError, AiMessage,
                      │             │   ToolDescriptor, ProviderCapabilities, Generation*,
                      │             │   the AIProvider interface. No Android. No deps.
                      └──────┬──────┘
             ┌───────────────┼───────────────┬───────────────┐
             │               │               │               │
      ┌──────▼─────┐  ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼───────┐
      │ core-tools │  │ core-memory │ │ core-intent │ │core-providers│
      │            │  │             │ │             │ │              │
      │Tool,       │  │MemoryStore, │ │IntentRecog- │ │AIProviderMgr,│
      │ToolRegistry│  │Conversation-│ │nizer,       │ │5 scaffolded  │
      │            │  │Buffer       │ │IntentType   │ │providers     │
      └──────┬─────┘  └─────────────┘ └──────┬──────┘ └──────────────┘
             │                                │
             │        ┌───────────────────────┘
             │        │
      ┌──────▼────────▼──┐
      │   core-planner    │  Planner, ExecutionPlan, PlanStep, PlanParameterResolver
      │  (needs core-ai + │
      │   core-intent)    │
      └───────────────────┘

      ┌────────────────────┐
      │    core-actions     │  the ONE Android-aware core module (needs a real Context)
      │ (needs core-ai +    │  ActionEngine + 10 real Tool implementations
      │  core-tools)        │
      └────────────────────┘

      ┌──────────────────────────────────────────────────────────┐
      │                          :app                              │
      │  depends on all 7 — the composition root.                  │
      │  • Hilt modules bind every interface to its default impl   │
      │  • AppAutomationTool (app-specific, wired to real repos)   │
      │  • PlanExecutor (the only thing that needs BOTH a Planner's│
      │    ExecutionPlan and core-actions' ActionEngine)            │
      └──────────────────────────────────────────────────────────┘
```

No cycles. `core-actions` and `core-planner` never depend on each other — see §4 for why that's
deliberate, not an oversight.

| Module | Kotlin/JVM or Android? | Depends on |
|---|---|---|
| `core-ai` | Kotlin/JVM | — |
| `core-tools` | Kotlin/JVM | core-ai |
| `core-memory` | Kotlin/JVM | core-ai |
| `core-intent` | Kotlin/JVM | core-ai |
| `core-planner` | Kotlin/JVM | core-ai, core-intent |
| `core-providers` | Kotlin/JVM | core-ai |
| `core-actions` | Android library | core-ai, core-tools |
| `:app` | Android application | all 7 |

Six of seven modules are plain Kotlin/JVM — unit-testable with no emulator, no Robolectric, no
Android framework at all. Only `core-actions` needs Android, because it's the only module that
actually touches the device (launching intents, scheduling alarms, writing files).

---

## 2. The seven interfaces, in the brief's own numbering

1. **`AIProvider`** (core-ai) — `suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse>`, plus `generateStream`, `capabilities`, `isAvailable()`. 5 implementations in core-providers, all scaffolded.
2. **`IntentRecognizer`** (core-intent) — `suspend fun recognize(utterance: String): AuraResult<RecognizedIntent>`. One real, local, offline implementation: `KeywordIntentRecognizer`, covering all 10 requested categories (Open App, Reminder, Calendar, Research, Coding, Shopping, Navigation, Email, Automation, Conversation) plus `Unknown`.
3. **`Planner`** (core-planner) — `suspend fun plan(goal, intent, availableTools): AuraResult<ExecutionPlan>`. One real implementation: `TemplatePlanner`, which produces exactly the brief's own worked example (§3 below).
4. **Action Engine** (core-actions) — `ActionEngine.execute(toolName, arguments): AuraResult<ToolResult>`, backed by 10 real Android-side `Tool` implementations.
5. **Tool Registry** (core-tools) — `ToolRegistry`: `register`/`unregister`/`get`/`all`/`descriptors`. Populated at app startup from a Hilt `Set<Tool>` multibinding (10 core-actions tools + 1 app-specific tool).
6. **AI Provider Manager** (core-providers) — `AIProviderManager`: `activeProvider: StateFlow<AIProvider>`, `selectProvider(id)`, `generate(...)` delegates to whichever provider is active. This is the concrete answer to "users should be able to switch providers without changing business logic" — see §5.

---

## 3. The worked example, running today

The brief's own example — *"Create my assignment" → Research → Generate → Export PDF → Save →
Notify* — is not just documentation. `TemplatePlanner` recognizes any goal mentioning
"assignment"/"report"/"essay"/"document"/"paper" and produces exactly this 5-step plan, with real
data flowing between steps via `${step:<id>.output}` references that `PlanParameterResolver`
resolves at execution time:

| Step | Tool | Status if run today |
|---|---|---|
| 1. Research the topic | `web_search` | ✅ Real — opens a browser search |
| 2. Generate the content | *(none)* | ❌ Correctly fails: `AuraError.ProviderNotConnected` — this step has no local implementation because writing the actual document body requires a connected AI provider |
| 3. Export as PDF | `export_pdf` | ✅ Real — renders `${step:2.output}` via `android.graphics.pdf.PdfDocument` |
| 4. Save the file | `save_file` | ✅ Real — copies from cache to permanent app storage |
| 5. Notify the user | `notify` | ✅ Real — posts a local notification |

The app's `PlanExecutor` (in `com.aura.ai.ai`) walks a plan in *dependency* order (via
`PlanStep.dependsOn`), not just list order, and halts on the first failure — so running this
plan today gets exactly as far as step 1, then stops at step 2 with an honest error, precisely
because no provider is connected. That is the correct, intended behavior for this phase, not a
bug to fix later.

Every other intent resolves to a single real step:

| Intent | Tool | Real today? |
|---|---|---|
| OpenApp | `open_app` | ✅ |
| Reminder | `set_reminder` | ✅ |
| Calendar | `create_calendar_event` | ✅ |
| Navigation | `navigate` | ✅ |
| Email | `compose_email` | ✅ |
| Shopping | `shopping_search` | ✅ |
| Research | `web_search` | ✅ |
| Automation | `app_automation` | ✅ (wired to the app's real `DeviceRepository`/`AutomationRepository`) |
| Coding | *(none)* | ❌ Needs a provider, honestly |
| Conversation | *(none)* | ❌ Needs a provider, honestly — this is just "talk back," which is exactly what a provider is for |

---

## 4. Two deliberate non-dependencies

**`core-actions` does not depend on `core-planner`.** `ActionEngine.execute()` takes a plain
`(toolName: String, arguments: Map<String, String>)`, not a `PlanStep`. If it depended on
core-planner just to accept a richer parameter type, the only genuinely Android-aware module
would drag in a portable, pure-Kotlin module's data model for no real benefit — and worse, it
would make core-planner's own tests implicitly need to think about Android-shaped concerns. The
app's `PlanExecutor` is the (correct, expected) place that needs both.

**`core-planner` does not depend on `core-tools`.** Its `Planner.plan()` signature accepts
`availableTools: List<ToolDescriptor>` — and `ToolDescriptor` lives in `core-ai`, not
`core-tools` (see core-ai's own README for why). `TemplatePlanner` references tools purely by
name string (`"web_search"`, `"export_pdf"`, ...) and never touches the `Tool` interface or
`ToolRegistry` type at all. An early draft of this module *did* declare a `core-tools`
dependency defensively; it was removed once nothing in the module actually used it — an unused
inter-module dependency is exactly the kind of unjustified coupling this architecture is trying
to avoid introducing.

---

## 5. How "switch providers without changing business logic" actually works

```kotlin
// Anything that wants a completion — core-planner today, a future ViewModel later —
// depends on this interface and NOTHING else:
interface AIProviderManager {
    val activeProvider: StateFlow<AIProvider>
    fun selectProvider(id: ProviderId): AuraResult<Unit>
    suspend fun generate(request: GenerationRequest): AuraResult<GenerationResponse>
    // ...
}
```

The 5 providers are bound into a single Hilt `Set<AIProvider>` multibinding
(`app/.../ai/di/AiProvidersModule.kt`). `DefaultAIProviderManager` receives that whole set via
constructor injection and does nothing more than route `generate()` to whichever provider is
currently selected. **Switching providers is `selectProvider(ProviderId.Ollama)` — one function
call, zero code changes anywhere else.** Adding a 6th provider (say, a new cloud API) is one new
class implementing `AIProvider` plus one new `@Binds @IntoSet` line in `AiProvidersModule` —
`core-planner`, `core-intent`, and every future caller of `AIProviderManager` are entirely
unaffected.

This is the same pattern applied twice more in this phase, for the same reason:
- **Tools**: 11 `Tool` implementations (10 core-actions + 1 app-specific) → one `Set<Tool>`
  multibinding → one `ToolRegistry`. Adding a 12th tool never touches `ActionEngine`,
  `core-planner`, or anything that calls `ToolRegistry.get(name)`.
- **The default implementations themselves** (`KeywordIntentRecognizer`, `TemplatePlanner`,
  `InMemoryMemoryStore`, `SlidingWindowConversationBuffer`) are each a single `@Binds` line in
  `AiCoreModule`. Replacing `TemplatePlanner` with a future `LlmPlanner` is exactly as
  consequence-free as switching providers is.

---

## 6. DI wiring reference

All in the app module, `com.aura.ai.ai` / `com.aura.ai.ai.di`:

| File | What it binds |
|---|---|
| `AiCoreModule.kt` | `IntentRecognizer→KeywordIntentRecognizer`, `Planner→TemplatePlanner`, `MemoryStore→InMemoryMemoryStore`, `ConversationBuffer→SlidingWindowConversationBuffer`, `ToolRegistry→DefaultToolRegistry`, `ActionEngine→DefaultActionEngine`, `AIProviderManager→DefaultAIProviderManager` |
| `AiProvidersModule.kt` | The 5 `AIProvider`s → `Set<AIProvider>` |
| `AiToolsModule.kt` | The 10 core-actions `Tool`s + `AppAutomationTool` → `Set<Tool>` |
| `AppAutomationTool.kt` | The one app-specific `Tool`, constructor-injected with the app's real `DeviceRepository`/`AutomationRepository` |
| `PlanExecutor.kt` | Walks an `ExecutionPlan` using `ActionEngine` + `AIProviderManager` |

`AuraApplication.onCreate()` injects `ToolRegistry` and the `Set<Tool>` multibinding directly and
registers every tool at startup — the one place the whole graph gets assembled into something
runnable.

---

## 7. What Phase 4 (actually connecting a provider) will touch

**Will touch:** exactly one provider class in `core-providers` (e.g. `GeminiProvider`) — add an
HTTP client dependency, implement `generate`/`generateStream`/`isAvailable` for real, source a
real API key from encrypted storage via DI instead of the placeholder `ProviderConfig` each
class currently hardcodes.

**Will not touch:** `core-ai`, `core-tools`, `core-memory`, `core-intent`, `core-planner`,
`core-actions`, or any of the other 4 providers. The "Generate" step of the document pipeline and
the Coding/Conversation intents will simply start succeeding instead of returning
`ProviderNotConnected` — nothing about how they're planned or routed changes.

---

## 8. Honesty ledger

| Claim | True? |
|---|---|
| Every module builds a real interface, not a sketch | ✅ |
| `IntentRecognizer`, `Planner`, `ToolRegistry`, `MemoryStore`, `ConversationBuffer` have real, working default implementations | ✅ |
| 10 of the Action Engine's tools genuinely execute real Android actions | ✅ |
| Any `AIProvider.generate()` call succeeds today | ❌ — and is documented as such everywhere it matters (code comments, this doc, each module's README) |
| The document-creation example plan runs end-to-end today | ❌ — runs correctly through step 1, then fails honestly at step 2 |
| Switching providers requires touching business logic | ❌ — that's the point (§5) |
