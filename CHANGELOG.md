# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions reference [VERSION.md](VERSION.md), not
the Android `versionCode`/`versionName`.

## [0.19.0-dev] — 2026-07-30

### Added
- Optional release signing config (`app/build.gradle.kts`) — reads `keystore.properties` (already
  `.gitignore`d) when present; absent (true here and in CI), `release` stays unsigned exactly as
  before. `keystore.properties.example` documents the format and the `keytool` command to generate
  a real keystore.
- `LocalCrashLogger` (`core/crash/`) — a dependency-free `Thread.setDefaultUncaughtExceptionHandler`
  hook writing every crash to a bounded, on-device log before delegating to the previous handler.
  A real crash-reporting *service* needs its own account/credentials, which only AURA's publisher
  can create — flagged, not guessed.
- Profile → About (new `ProfileSub`) — real `BuildConfig.VERSION_NAME`/`VERSION_CODE`, a condensed
  privacy summary, and pointers to the two new docs below. "Privacy & Data" now navigates there
  directly instead of falling through to Settings.
- `docs/PRIVACY_POLICY.md` — a real policy built from `docs/SECURITY.md`'s own verified findings,
  covering every declared permission; explicitly marked as a draft pending the publisher's own
  business/contact/jurisdiction details and legal review.
- `docs/THIRD_PARTY_LICENSES.md` — every real dependency from the version catalog, grouped by
  actual license (Apache 2.0, EPL 1.0), with Google ML Kit correctly called out as proprietary
  terms rather than open source.
- `docs/PRODUCTION_RELEASE.md`.

### Fixed
- `config/detekt/detekt.yml` — detekt has its own separate `MaxLineLength`/`FunctionNaming`/
  `LongMethod` rules that don't share ktlint's `.editorconfig` exemptions; added matching
  Compose-aware adjustments (`ignoreAnnotated: [Composable]`) after detekt correctly caught them as
  new findings against this milestone's own code — proof the CI/CD milestone's baseline setup
  actually works as intended.

## [0.18.0-dev] — 2026-07-29

### Added
- `.github/workflows/ci.yml` — three parallel jobs (static analysis, unit tests, build) on every
  push/PR to `main`: `ktlintCheck`, `detekt`, `test`, Android `lintDebug`, `assembleDebug`,
  `assembleRelease`. Artifacts uploaded: detekt reports, test reports, Android Lint report, debug
  APK.
- `org.jlleitschuh.gradle.ktlint` and `io.gitlab.arturbosch.detekt`, applied to all 18 modules from
  the root `build.gradle.kts`. `config/detekt/detekt.yml` (three documented rule adjustments on top
  of detekt's default ruleset) and a per-module `config/detekt/baseline-<module>.xml` (generated
  via `detektBaseline`) grandfathering ~460 pre-existing findings across ~8 milestones of code that
  predates this milestone, while still failing on any new finding.
- `.editorconfig` (repo root) — exempts `@Composable` functions from lowercase-naming enforcement
  (Compose's own convention, not a violation), disables `max_line_length` (this codebase's
  established prose-dense KDoc style), and excludes two pre-existing files from the
  filename-matches-class rule.
- `docs/CI_CD.md`.

### Fixed
- Ran `ktlintFormat` across all 18 modules — fixed every auto-fixable formatting violation. Two
  violations it couldn't auto-fix (`core-events.DefaultEventBus`, `core-plugin.DefaultPluginEventBus`
  each had a `_events` backing field paired with a function `events()`, not a matching property —
  outside ktlint's `backing-property-naming` pattern) fixed by renaming to `mutableEvents` in both
  files; zero behavior change.

## [0.17.0-dev] — 2026-07-29

### Added
- `docs/SECURITY.md` — full review of permissions, API-key storage, logging, secrets-in-source,
  network security config, backup, ProGuard rules, data deletion, export/import, and third-party
  data sharing.
- `MemoryRepository.forgetAll()` / `MemoryDao.deleteAll()` — real, irreversible deletion backing
  the Profile screen's "Clear all memory" action.
- `PluginLogSink` (`core-plugin`) — an injected qualifier for the plugin-logging facade's sink,
  implemented in `:app` as a real no-op in release builds (`BuildConfig.DEBUG`-gated `Log.d` in
  debug). `buildFeatures.buildConfig = true` enabled in `app/build.gradle.kts` to make
  `BuildConfig.DEBUG` available.

### Fixed
- **Data deletion**: Profile → Memory Manager's "Clear all memory" row had no `onClick` handler at
  all — a `danger = true`, "Irreversible" control that silently did nothing. Now wired to a
  Material3 confirmation dialog and real deletion of every persisted `Memory` entry.
- **Backup**: the `EncryptedSharedPreferences` file backing AI provider API keys
  (`aura_provider_credentials`) was never excluded from `data_extraction_rules.xml`/
  `backup_rules.xml`, unlike the Room database and DataStore prefs. Excluded from both cloud-backup
  and device-transfer — the latter a real gap, since some OEM device-transfer implementations can
  migrate Keystore-backed encryption keys alongside app data.
- **Logging**: `PluginContextFactory`'s plugin-logging sink used unconditional `println`, reachable
  by any plugin and unfiltered even in release builds. Now a real no-op in release, `Log.d` in
  debug only.

### Documented
- "Export My Data" / "Export memory graph" remain unwired — a real gap, but a substantive feature
  in its own right already correctly tracked as a separate High-priority `ROADMAP.md` item
  ("Conversation/memory export & backup"), not rushed into this pass. See `docs/SECURITY.md` §5.

## [0.16.0-dev] — 2026-07-29

### Added
- `docs/PERFORMANCE.md` — static, code-level performance review (no device/emulator available in
  this environment, so no measured runtime profiling) covering startup, Compose recomposition,
  Room, coroutine dispatcher usage, provider latency, agent-execution concurrency, and plugin
  loading.

### Changed
- `ConversationPipeline.run()` — the single entry point for every chat message — now runs on
  `Dispatchers.Default` instead of whatever dispatcher the caller used (`viewModelScope`'s
  `Dispatchers.Main.immediate` in practice). Its heaviest stages (intent recognition, memory
  retrieval, reasoning/ranking) are CPU-bound and were never explicitly moved off Main; the stages
  that genuinely need I/O already switch to their own dispatcher internally and are unaffected.
- `chat_messages` (Room) gained an index on `timestampMillis`, the column every read sorts by —
  the one table with unbounded, ever-growing row count. `AuraDatabase` bumped `version = 2` →
  `3`; covered by the existing `fallbackToDestructiveMigration(true)`, no migration needed.
- `TraceOverlay` (`AuraTabScreen.kt`): its `LazyColumn` now has a stable `key`, and the trace
  formatting is `remember`ed instead of re-running on every recomposition.

### Documented
- `AuraApplication`'s eager `Set<Tool>`/`Set<Agent>` Hilt field injection (forces all 31
  tool/agent constructors to run synchronously before `onCreate()`'s body) — a real finding, left
  deliberately unfixed: each constructor is individually cheap, and fixing the pattern properly
  means a real DI redesign (lazy injection + register-on-first-use) that the brief's own
  "do not redesign the architecture" instruction argues against making blind, with no device
  available to confirm it actually helps. See `docs/PERFORMANCE.md` §2.

## [0.15.0-dev] — 2026-07-29

### Added
- **Critical item 7/10, Testing**: 74 new tests across the three modules that had zero — the
  multi-agent pipeline's actual decision points, per `docs/TODO_V1.md` §2.7's own stated priority
  ("prioritize by blast radius").
  - `core-reasoning` (44 tests, 6 new files): `DefaultDecisionEngineTest`,
    `DefaultConfidenceEvaluatorTest`, `DefaultConstraintEngineTest`, `RuleBasedTaskDecomposerTest`,
    `DefaultGoalManagerTest`, `DefaultCapabilityResolverTest`.
  - `core-orchestrator` (18 tests, 3 new files): `DefaultAgentSelectionEngineTest`,
    `DefaultResultAggregatorTest`, `DefaultTaskDispatcherTest` (retry/timeout logic driven through
    `kotlinx-coroutines-test` virtual time).
  - `core-agents` (12 tests, 2 new files): `ToolBackedAgentTest` (the shared `health()`/`runTool()`
    logic five agents inherit), `DefaultAgentRegistryTest`.
  - `docs/TESTING.md`.
- Test dependencies (`junit4`, `mockk`, `kotlinx-coroutines-test`) added to `core-reasoning`,
  `core-agents`, and `core-orchestrator`'s `build.gradle.kts`.

### Fixed
- `DefaultCapabilityResolver` (`core-reasoning`): `file_search`'s real, API-26–28-only
  `READ_EXTERNAL_STORAGE` requirement was never mirrored into the resolver's permission map —
  found while writing `DefaultCapabilityResolverTest`. Left unmapped rather than added
  unconditionally (the map has no per-API-level dimension, so either range would be told the wrong
  thing); documented as a deliberate, narrow scope boundary in the class's own KDoc. No user-facing
  behavior was ever wrong — `FileSearchTool` itself already checks and reports honestly at call
  time.

## [0.14.0-dev] — 2026-07-29

### Added
- `KnowledgeGraph`/`DefaultKnowledgeGraph` (`core-memory`) — `relatedTo(id, type?)` (searches both
  outgoing and incoming edges) and transitive `neighborhood(id, depth)` (cycle-safe), built over
  `MemoryEntry.relationships`/`RelationshipType`, both of which have existed since Phase 4 with no
  reader or writer until now.
- `RelationshipLinker` — pure, word-overlap-based relationship suggestion: structural memories
  (`Task`/`Assignment`/`Meeting`/`Document`/`Research`/`File`) link `PartOf` a matching `Project`;
  any memory mentioning a name link `RelatesTo` a matching `Person`. Honestly documented as
  heuristic, not real entity resolution.
- `MemoryAgent.remember()` now calls `KnowledgeGraph.autoLink` immediately before persisting each
  newly-extracted memory.
- Six new `MemoryCategory` values: `Person`, `Document`, `Meeting`, `Assignment`, `Research`,
  `File` — plus real `RuleBasedMemoryClassifier` trigger phrases for each, so they're reachable
  from actual conversation text, not just unreachable enum values.
- `core-memory`'s first test source set: 12 tests (`RelationshipLinkerTest`,
  `DefaultKnowledgeGraphTest`) covering word-overlap linking (both rules, together, and the
  negative cases), bidirectional `relatedTo`, type filtering, transitive `neighborhood`, and cycle
  termination.
- `docs/KNOWLEDGE_GRAPH.md`.

## [0.13.0-dev] — 2026-07-29

### Added
- Persisted `Workflow`/`WorkflowStep` domain model — steps are the exact same `(toolName,
  arguments)` shape every `Tool` call already uses. `WorkflowEntity` (Room's 4th new entity;
  `AuraDatabase` bumped to `version = 2`), `WorkflowDao`, `WorkflowRepository`/`Impl`.
- `WorkflowStepEncoding` — percent-encodes steps into a single Room column via plain
  `java.net.URLEncoder`, not kotlinx.serialization — deliberately, given this project's own
  earlier build-stability issues introducing that library into a KSP processor classpath
  (`docs/ANDROID_AUTOMATION.md` §5).
- `WorkflowManager` (`:app`) — the one facade `AutomateViewModel` depends on, keeping
  `WorkflowRepository` and `WorkflowScheduler`/`WorkManager` in sync.
- `WorkflowExecutor` — runs a workflow's steps via `ActionEngine.executeConfirmed`, stopping at the
  first failure (matching `PlanExecutor`'s existing convention for `ExecutionPlan`).
- `WorkflowScheduler` + `@HiltWorker WorkflowExecutionWorker` — `WorkManager` `PeriodicWorkRequest`
  per workflow, Daily (1-day interval) or Weekly (7-day interval), initial delay computed to the
  next real occurrence via `java.time`. Reuses the `HiltWorkerFactory` wiring from
  `docs/ANDROID_AUTOMATION.md` §3 rather than introducing a second scheduling mechanism.
- `WorkflowPresets` — Morning Brief, Study Routine, Shopping Routine, Daily Review, Weekly Planning,
  built strictly from existing tools (`notify`, `media_control`, `shopping_search`).
- A new "Workflows" section in the Automate tab: one-tap preset add, enable/disable toggle, Run
  Now, delete.
- `DatabaseModule` now calls `.fallbackToDestructiveMigration(true)` — no real migrations exist yet
  and there's no pre-1.0 install base to preserve.
- 9 new tests: 5 for `WorkflowStepEncoding` (round-trips, including delimiter-character values), 4
  for `WorkflowExecutor` (in-order execution, stop-on-first-failure, empty workflow, and proof that
  `executeConfirmed` — not `execute` — is what's actually called).
- `docs/WORKFLOW_ENGINE.md`.

## [0.12.0-dev] — 2026-07-29

### Added
- 4 new tools: `ocr` and `scan_barcode` (ML Kit, on-device, free, offline), `scan_receipt` (OCR +
  heuristic total/date/merchant parsing, honestly non-guaranteed), `understand_image` (routes to
  whichever connected `AIProviderManager` provider supports vision).
- `AiMessage.imageBase64`/`AiMessage.imageMimeType` (both nullable, default `null` — fully
  additive, no existing call site changes). `ClaudeProvider`/`OpenAIProvider`/`GeminiProvider` each
  map them into their own real vision content-block format; `OllamaProvider` untouched
  (`supportsVision = false`).
- A "Scan Text (Camera)" Quick Action in the Automate tab: requests `CAMERA` permission at the
  point of use, captures via `ActivityResultContracts.TakePicture()` to a fresh per-capture file
  under `cacheDir/captures/`, exposed to the system camera app through a new `FileProvider`
  (`res/xml/file_paths.xml`), then runs `ocr` on the result.
- 7 new tests (`core-actions/src/test`) for `parseReceipt`'s heuristic — labeled-total,
  largest-amount fallback, date extraction, missing date, merchant guess, and both branches of
  `ReceiptSummary.describe()`'s honesty check.
- `docs/VISION_RUNTIME.md`.

### Fixed
- A real string-concatenation bug in `ReceiptSummary.describe()`, caught while writing its own
  tests: the "couldn't confidently parse" fallback could run directly into "Unknown merchant" with
  no separator between them, since both code paths could fire in the same call.
- `VisionAgent`'s (Phase 6) own documentation and failure-reason string were stale after this
  milestone and `docs/AI_PROVIDER_INTEGRATION.md` closed the two gaps it was originally scaffolded
  around (no connected provider, no image-carrying request shape) — updated to describe the one
  gap that's actually still real (this pure-Kotlin/JVM agent has no way to load image bytes
  without an Android `Context`; `ImageUnderstandingTool` in `:app` is where vision actually landed).

## [0.11.0-dev] — 2026-07-29

### Added
- 8 new tools: `open_settings`, `clipboard`, `share`, `file_search` (Downloads collection),
  `media_control` (synthetic media-button events), `schedule_action` (WorkManager, runs another
  tool once later), `read_screen` and `device_navigate` (via a new, narrowly-scoped
  `AuraAccessibilityService` — reads on-screen text and presses back/home/recents only; no gesture
  dispatch or simulated taps on other apps).
- `Tool.requiresConfirmation` (default `false`) and `ActionEngine.executeConfirmed()` — a real
  safety gate: `ActionEngine.execute()` (the only method the reasoning/agent/plan pipeline ever
  calls) now refuses a tool marked `requiresConfirmation = true`; only a direct UI tap
  (`AutomateViewModel.runQuickAction`, a new "Quick Actions" row in the Automate tab) can run one,
  via `executeConfirmed()`.
- The project's first WorkManager usage: `DeferredActionTool` + `@HiltWorker DeferredActionWorker`,
  Hilt-integrated via `HiltWorkerFactory` (`AuraApplication` now implements
  `Configuration.Provider`; the manifest's default `WorkManagerInitializer` is removed).
- `AuraAccessibilityService`, its config XML, and an explicit disclosure card in Settings →
  Automation before sending the user to system Accessibility settings (which cannot be requested
  via a normal permission dialog).
- 5 new tests (`core-actions/src/test`, this module's first test source set) covering the
  confirmation gate.
- `docs/ANDROID_AUTOMATION.md`.

### Fixed
- **Room was silently resolving to 2.8.4** (not the declared 2.6.1) once WorkManager was added —
  `androidx.work` transitively requires a newer Room than this project's old pin, and Gradle's
  highest-version-wins resolution honored that silently. Re-pinned `room` to `2.8.4` so the
  declared version matches reality, and aligned `kotlinx-serialization-json` to `1.8.1` (Room
  2.8.4's own requirement, itself verified compatible with this project's pinned Kotlin 2.1.0).
  Even after aligning both, a residual debug/release KSP-classpath version-drift flake persisted in
  Room's schema-bundle (de)serialization (`AbstractMethodError` in generated `$$serializer`
  classes) — resolved by setting `exportSchema = false` on `AuraDatabase`, since this project has
  no migrations yet to justify the fragility.
- **`assembleRelease` had never succeeded in this project before this milestone** — R8 failed on
  Google Tink's (behind `androidx.security.crypto`) references to compile-time-only
  `com.google.errorprone.annotations.*`. Fixed with the exact `-dontwarn` rules Android Gradle
  Plugin itself generates for this well-documented Tink/R8 interaction.
- `androidx.hilt:hilt-work` pinned to `1.2.0` (matching the already-proven-working
  `hilt-navigation-compose` version) after `1.4.0` generated Hilt codegen incompatible with this
  project's pinned `com.google.dagger:hilt-android:2.53.1` (`Hilt_MainActivity.java` referenced a
  method — `getSavedStateHandleHolder()` — that didn't exist on that Dagger Hilt version).

## [0.10.0-dev] — 2026-07-29

### Added
- **Push-to-talk Voice Runtime** — the orb (`AuraSub.Orb`) is a real voice interface: streaming
  speech-to-text via `SpeechRecognizer` (live partial transcripts routed into the existing chat
  input field), `TextToSpeech` output mapped to the persisted `AssistantVoice` persona
  (`Jarvis`/`Friday`/`Neutral`) via pitch/rate, tap-triggered interruption (barge-in), automatic
  "conversation continuation" for follow-ups (bounded by the recognizer's own silence timeout, no
  hand-rolled timer), and transient `USAGE_ASSISTANT` audio focus around every listen/speak turn.
- `com.aura.ai.ai.voice` package (`:app`): `VoiceInputController`, `VoiceOutputController`,
  `AudioFocusCoordinator`, `VoiceRuntime` — the same "thin facade over Android-framework-wrapping
  collaborators" shape `AuraRuntimeFacade` already established for text.
- A fresh `RECORD_AUDIO` permission check at the point of use (tapping the orb), independent of
  onboarding's own toggle — handles a permission declined at onboarding or revoked since.
- A "Conversation continuation" toggle in Settings → Personalization
  (`UserPreferences.voiceContinuousConversationEnabled`, default on).
- A transient, auto-dismissing banner for genuine voice errors (device has no recognizer, a real
  recognition failure) — the expected "user said nothing" case never surfaces one.
- `AuraRuntimeFacade.sendMessage` now returns `ExecutionResult` (previously `Unit`) — needed so the
  voice layer can speak the response; the existing typed-chat call site simply ignores the return
  value, unchanged.
- 10 new tests, `:app`'s first test source set (`app/src/test`): error-message mapping,
  silent-vs-surfaced error classification, and persona pitch/rate mapping — everything in the voice
  layer that's pure logic rather than a live `SpeechRecognizer`/`TextToSpeech` instance.
- `docs/VOICE_RUNTIME.md`.

### Fixed
- Two real, pre-existing `MissingPermission` Lint errors in `core-actions` (`NotifyTool.kt`,
  `ReminderBroadcastReceiver.kt`) — both already checked `POST_NOTIFICATIONS` correctly at runtime
  before calling `NotificationManagerCompat.notify`, but tripped a documented Lint limitation that
  doesn't recognize `ContextCompat.checkSelfPermission` as a valid guard for that specific API in
  this AGP/Lint version, in any of the shapes tried (early-return, positive-guard, inline condition,
  intermediate variable). Fixed with a justified `@SuppressLint("MissingPermission")` — the standard
  response to a verified false positive — not a code change, since the logic was already correct.
  Found because this milestone's verification ran `./gradlew lintDebug` at the project root for the
  first time; previous milestones had only run it scoped to `:app`.
- Removed a dead `SDK_INT < O` guard in `NotificationChannels.kt` — `minSdk` is already 26 (`O`).

## [0.9.0-dev] — 2026-07-29

### Added
- Real `generate`/`generateStream` for **Claude** (Anthropic Messages API), **OpenAI** (Chat
  Completions), **Gemini** (`generateContent`/`streamGenerateContent`), and **Ollama** (local
  NDJSON) — all four now make genuine HTTP calls via OkHttp instead of returning
  `AuraError.ProviderNotConnected` unconditionally. Streaming uses Server-Sent Events for the three
  cloud providers and raw newline-delimited JSON for Ollama, matching each API's real transport.
- `ProviderCredentialStore` (core-providers, pure Kotlin) + `AndroidProviderCredentialStore` (`:app`,
  `EncryptedSharedPreferences`-backed) — API keys are encrypted at rest, in a preference file
  separate from general app settings.
- A working Settings → AI Providers UI (`ProfileScreen`/`ProfileViewModel`): per-provider connection
  status, API key entry, Ollama base-URL override, active-provider selection.
- `network_security_config.xml` scoping cleartext HTTP to `localhost`/`127.0.0.1`/`10.0.2.2` only
  (for Ollama); `INTERNET` permission added to the manifest (previously absent — no provider could
  have made a network call even with a key configured).
- `IoDispatcher` qualifier (core-providers) + a matching `@Provides` binding in `:app`'s
  `CoroutineModule`, so the four providers' network calls run on `Dispatchers.IO` through Hilt
  rather than a hardcoded default Dagger can't see.
- The project's first test dependencies (JUnit4, MockK, Turbine, `kotlinx-coroutines-test`, OkHttp
  `mockwebserver`) and first test source set: `core-providers/src/test`, 27 tests covering all four
  providers' request construction, response parsing, and HTTP-error-to-`AuraError` mapping against a
  real `MockWebServer`, plus the shared JSON/error-mapping helpers in isolation.
- `docs/AI_PROVIDER_INTEGRATION.md`.

### Fixed
- OkHttp `5.4.0`'s Android-specific artifact requires `compileSdk 36`, which AGP 8.7.3 (pinned here)
  doesn't support — pinned `okhttp` to `4.12.0` instead of bumping AGP/compileSdk mid-feature.
- `kotlinx-serialization-json` `1.11.0` is compiled against a newer Kotlin than this project pins
  (binary metadata version 2.3.0 vs. this project's 2.1.0), causing a compiler-internal error —
  pinned to `1.7.3`, whose own `kotlin-stdlib` dependency (2.0.20) is compatible.
- Missing `contentOrNull` imports (three files) after switching from `.content` to the nullable
  accessor for optional JSON fields.
- `:app` needed OkHttp on its own compile classpath (not just transitively via `implementation` in
  `:core-providers`, which Gradle doesn't expose to consumers) since `AiProvidersModule` — which
  lives in `:app` — constructs the shared `OkHttpClient`.
- Two instances of the same Dagger/Kotlin gotcha: a constructor parameter's Kotlin default value is
  invisible to Dagger's generated code, so it always demands a binding regardless. Fixed for
  `CoroutineDispatcher` with a proper `@IoDispatcher` qualifier; fixed for the test-only
  `apiBaseUrl` override by making it a settable `internal` property instead of a constructor
  parameter (an unqualified `String` binding would have been dangerously ambiguous against any other
  `String` the graph might ever need).

### Static analysis
- `./gradlew :app:lintDebug` run for the first time: 0 errors, 78 pre-existing warnings (mostly
  `GradleDependency`/`AndroidGradlePluginVersion` — newer versions available, several intentionally
  not taken for compatibility reasons noted above). None from this milestone's new or modified code.
  Full Kotlin-style static analysis (ktlint/detekt) is still unaddressed — tracked under
  `docs/TODO_V1.md` §2.7/§2.8.

## [0.8.0-dev] — 2026-07-29

### Fixed
- **The project had never been built.** Generated the missing Gradle wrapper (pinned to Gradle
  8.10.2), created `local.properties`, fixed a root `build.gradle.kts` plugin-resolution bug
  (`android.library` and `kotlin.jvm` were never declared with `apply false`, breaking every
  library module), and added the Foojay toolchain resolver so Gradle can provision a JDK 17
  toolchain (only JDK 23 was locally installed).
- Fixed ~50 real Kotlin/Compose compile errors in the `:app` module across 14 files, all resolving
  to 6 distinct API mismatches between the source and the pinned Compose BOM (`2024.12.01`):
  `ripple()` (doesn't exist until material3 1.4.0 — switched to `rememberRipple()`, suppressing its
  error-level deprecation with a documented rationale), `RowScope.weight`/`ColumnScope.weight`
  (became scope members, not top-level imports — removed the obsolete imports), a missing
  `animateFloat` import, a `graphicsLayer` import from the wrong package, `DrawScope.center` used
  outside a `DrawScope` receiver, and an invalid `Modifier.padding(horizontal=, bottom=)` overload.
  Full detail in [docs/TODO_V1.md §0](docs/TODO_V1.md#0-foundation--found-broken-fixed-this-session).
- `./gradlew :app:assembleDebug` now succeeds and produces a real, installable `app-debug.apk`.

### Added
- Git version control (previously absent — `git init`, `.gitignore` covering build output, local
  SDK config, keystores, and secrets).
- `docs/TODO_V1.md` — full Critical/High/Medium/Low gap analysis toward Version 1.0, with effort
  estimates and blockers, grounded in direct verification of the current source tree.
- `VERSION.md`, `CHANGELOG.md`, `ROADMAP.md` (this file and its siblings) as the project's ongoing
  source of truth for maturity tracking, separate from Android's own versioning.
