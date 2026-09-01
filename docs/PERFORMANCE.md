# AURA AI — Performance

Milestone 7 of the "AURA FINAL VERSION 1.0 COMPLETION" brief: "profile and optimize: Startup,
Memory, Compose recomposition, Database, Provider latency, Reasoning latency, Agent execution,
Plugin loading."

## 0. What this is, and isn't

**No Android device or emulator exists in this environment.** Everything below is a *static,
code-level* review — reading the actual implementation for known anti-patterns and tracing actual
data flow and dispatcher usage — not measured runtime profiling (no Macrobenchmark, no Layout
Inspector, no Perfetto trace). Every finding is anchored to a real file and line, not a
hypothetical. Numbers like "wave-parallel agent execution" are structural facts about the code,
not measured latencies. Where a finding was real but not worth fixing blind (no device to verify
the fix actually helps, or the fix would itself be riskier than the problem), that's said
explicitly rather than either skipped silently or "fixed" without a way to confirm it.

---

## 1. Findings and what happened to each

| # | Area | Finding | Outcome |
|---|---|---|---|
| 1 | Startup | `AuraApplication`'s `@Inject lateinit var tools: Set<Tool>` / `agents: Set<Agent>` force Hilt to eagerly construct all 22 `Tool` and 9 `Agent` bindings synchronously during field injection, before `onCreate()`'s body runs. | **Deferred, documented** — see §2. |
| 2 | Compose | `TraceOverlay`'s `LazyColumn` (`AuraTabScreen.kt`) had no `key` on its `items(...)` call, and `formatTrace(trace)` re-ran its string formatting on every recomposition instead of being `remember`ed. | **Fixed** — see §3. |
| 3 | Database | `chat_messages` (the one table with unbounded, ever-growing row count) had no index on `timestampMillis`, the column every read sorts by. | **Fixed** — see §4. |
| 4 | Coroutines | `ConversationPipeline.run()` — the single entry point for intent recognition, memory retrieval, and reasoning, all CPU-bound rule/heuristic work — ran on whatever dispatcher the caller used (`viewModelScope`'s `Dispatchers.Main.immediate`), never switching to `Dispatchers.Default`. | **Fixed** — see §5. |
| 5 | Compose | `HomeUiState.timeline`/`.notifications`, `ProfileUiState.providerRows` are plain `List<T>`, not `ImmutableList<T>`, unlike every other list-bearing UI state in the app. | **Not a live bug, left alone** — see §6. |
| 6 | Room | Suspend DAO calls, `SELECT *` usage, `Flow`-vs-one-shot reads. | **No issue found.** |
| 7 | Providers | Shared `OkHttpClient` construction/timeouts/pooling. | **No issue found.** |
| 8 | Agent execution | Whether independent agents run sequentially or concurrently. | **No issue found — already concurrent.** |
| 9 | Plugin loading | Whether plugin discovery/loading blocks a hot path. | **No issue found — already backgrounded, and nothing expensive exists to load yet.** |
| 10 | Coroutines | `GlobalScope` usage anywhere in the codebase. | **None found.** |

---

## 2. Startup: eager `Tool`/`Agent` construction — a real, deliberately deferred finding

`AuraApplication.kt` declares:

```kotlin
@Inject lateinit var tools: Set<@JvmSuppressWildcards Tool>
@Inject lateinit var agents: Set<@JvmSuppressWildcards Agent>
```

Hilt's field-injection pass (which runs inside `Hilt_AuraApplication.onCreate()`, *before*
`AuraApplication.onCreate()`'s own body executes) must therefore construct every `Tool` and
`Agent` binding synchronously, on the main thread, before the app can proceed — 31 objects, plus
`toolRegistry`/`agentRegistry`/`pluginLoader`/`databaseSeeder`/`workerFactory`/`applicationScope`
themselves.

This is real, but every individual constructor is cheap — each `Tool`/`Agent` only stores its
injected `Context`/repositories; none does I/O or heavy computation at construction time (e.g.
`OcrTool`/`BarcodeScanTool` don't touch ML Kit until `execute()` actually runs). Fixing the pattern
properly means switching to lazy injection (`dagger.Lazy<Set<Tool>>` or `Provider<Set<Tool>>`) and
registering tools/agents on first use instead of at startup — a real change to how `ToolRegistry`/
`AgentRegistry` populate themselves, not a one-line fix. Given the brief's own "do not redesign the
architecture" instruction, and with no device available to measure whether this actually produces
a visible first-frame delay versus being lost in the noise of process start, this is left as a
documented, deliberate finding rather than a blind architectural change made without a way to
verify it helped. A future pass with an actual device/Macrobenchmark trace is the right way to
decide if this is worth doing.

---

## 3. Compose: `TraceOverlay` recomposition fix

`AuraTabScreen.kt`'s debug trace overlay had two real, textbook issues, both fixed:

```kotlin
// Before
items(formatTrace(trace)) { line -> ... }          // no key; formatTrace() re-runs every recomposition

// After
val lines = remember(trace) { formatTrace(trace) }  // computed once per distinct trace
itemsIndexed(lines, key = { index, _ -> index }) { _, line -> ... }
```

Low real-world impact — this is a debug-only overlay, not a hot path — but it's the one genuine
instance in the presentation layer of the exact anti-pattern this milestone was asked to look for.
Everywhere else, the codebase was already disciplined: every `ViewModel`-sourced list uses
`kotlinx.collections.immutable.ImmutableList` (`AuraTabViewModel.kt`, `AutomateScreen.kt`), and
every other `LazyColumn`/`LazyVerticalGrid` already supplies a stable `key`.

---

## 4. Database: `chat_messages` index

```kotlin
@Entity(tableName = "chat_messages", indices = [Index("timestampMillis")])
```

`chat_messages` is the one table with genuinely unbounded growth — every chat turn inserts a row,
and nothing prunes history. `ChatMessageDao` orders every read by `timestampMillis`; because the
read is exposed as `Flow<List<ChatMessageEntity>>`, Room re-runs that sort on every insert-
triggered invalidation. Without an index this is an unindexed resort that gets more expensive as
history grows, not just a constant cost — the one table in the schema where that distinction
actually matters (every other table is small and curated). `AuraDatabase` bumped from `version = 2`
to `version = 3`; `fallbackToDestructiveMigration(true)` (already in place, no migrations exist
yet, no pre-1.0 install base to preserve) covers the bump — no migration to write.

---

## 5. Coroutines: `ConversationPipeline.run()` moved to `Dispatchers.Default`

```kotlin
suspend fun run(session: ConversationSession, userMessage: String): ExecutionResult =
    withContext(Dispatchers.Default) { /* unchanged body */ }
```

`run()` is the single entry point every chat message flows through (`AuraTabViewModel.sendChat()`
→ `viewModelScope.launch` → this function). Its three heaviest stages —
`intentRecognizer.recognize` (`KeywordIntentRecognizer`'s keyword scoring across ~10 rule sets),
`memoryRetriever.retrieve`, and `reasoningEngine.reason` (which internally ranks up to 200
candidate memories via `WeightedMemoryRanker`'s Jaccard-similarity text scoring) — are all
CPU-bound, and none of them ever explicitly moved off whatever dispatcher the caller used. Since
`viewModelScope` runs on `Dispatchers.Main.immediate`, all of that scoring work was running on the
main thread the entire time.

Wrapping the single top-level entry point, rather than adding `withContext` calls inside every
individual class, keeps the fix to one file and one clear boundary — "this whole pipeline is
CPU-bound reasoning work, it belongs off Main" — instead of scattering dispatcher decisions across
`core-intent`/`core-memory`/`core-reasoning`. It's also safe by construction: the stages that
genuinely need I/O (the four HTTP providers, Room) already call their own `withContext(ioDispatcher)`
internally (verified — `ClaudeProvider.kt`, `OpenAIProvider.kt`, `GeminiProvider.kt`,
`OllamaProvider.kt` all correctly use an injected `IoDispatcher`), and nested `withContext` calls
compose correctly — they temporarily switch out to `Dispatchers.IO` and back, unaffected by the
outer `Dispatchers.Default` wrapper.

`KeywordIntentRecognizer` itself was deliberately *not* individually wrapped — ten short rule sets
of `String.contains` checks is negligible CPU work on its own; the value here is in moving the
*pipeline's* aggregate work off Main, which the outer `withContext` in `ConversationPipeline`
already accomplishes.

---

## 6. What was found but not changed

**`HomeUiState.timeline`/`.notifications`, `ProfileUiState.providerRows` as plain `List<T>`**: every
other list-bearing UI state in the app uses `ImmutableList<T>` so the Compose compiler can treat it
as a stable parameter. These two classes use plain `List<T>` instead — but both are `@Immutable`-
annotated `data class`es, which makes the Compose compiler trust the *whole type* as stable
regardless of individual field types, and both are always populated with fresh `listOf`/`.map()`
results, never mutated in place after construction. There is no live recomposition bug here; making
these consistent with the rest of the codebase would be a cosmetic normalization, not a fix, so it
wasn't done as part of this pass to avoid unrelated churn in files this milestone didn't otherwise
need to touch.

**Everything under "No issue found" in §1's table** (Room query shape, provider `OkHttpClient`
configuration, agent-execution concurrency, plugin loading, `GlobalScope` usage) was reviewed and
is already correct — see the full findings detail folded into §1–§5 above; nothing further to
report for those areas.

---

## 7. Verification

`assembleDebug`, `assembleRelease`, `lintDebug` (0 errors, 85 pre-existing warnings — unchanged
count from before this milestone), and `test` (all 144 tests, including the two touched modules'
existing suites) all green after every fix in this document.
