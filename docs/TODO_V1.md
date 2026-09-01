# AURA AI — Path to Version 1.0

A ground-truth inventory of what stands between the current codebase and a real, installable,
usable Version 1.0 — not a re-statement of the architecture docs (those describe what's designed
correctly), but the gap between "designed" and "works on a device." Every claim below was verified
directly against the repository (file search, dependency resolution, or an actual build), not
inferred from prior phase documentation.

See [PLATFORM_REVIEW.md](PLATFORM_REVIEW.md) for the architectural audit this builds on.

---

## 0. Foundation — found broken, fixed this session

Before any feature work could be evaluated honestly, one fact had to be established: **had this
project ever actually been compiled?** It had not. Three independent, stacked failures meant
`AuraApplication` had never run on a device or emulator, and no `./gradlew` command had ever
succeeded:

| # | What was broken | Fix applied |
|---|---|---|
| 1 | No `gradlew`/`gradlew.bat`/`gradle-wrapper.jar` existed at all | Generated the wrapper, pinned to Gradle 8.10.2 (matching the AGP 8.7.3 / Kotlin 2.1.0 toolchain already declared in `libs.versions.toml`) |
| 2 | No `local.properties` — no SDK path | Created it, pointing at the already-installed SDK at `%LOCALAPPDATA%\Android\Sdk` |
| 3 | Root `build.gradle.kts` declared `apply false` for `android.application`, `kotlin.android`, `kotlin.compose`, `ksp`, `hilt` — but **not** `android.library` or `kotlin.jvm`, the two plugins every one of the 16 library modules actually applies | Added both missing `apply false` declarations. Without this, Gradle fails plugin resolution with "already on the classpath with an unknown version" the moment more than one library module is evaluated — this alone made a from-scratch build impossible |
| 4 | No Gradle Java toolchain (17) resolvable on this machine (only JDK 23 and a JRE 8 installed) and no toolchain auto-provisioning configured | Added the Foojay toolchain resolver plugin to `settings.gradle.kts` so Gradle downloads a matching JDK 17 automatically |
| 5 | `:app:compileDebugKotlin` failed with ~50 real compiler errors across 14 presentation/design-system files | See below — all fixed |

The ~50 compile errors were not random breakage — they resolved to **6 distinct root causes**,
each a genuine API mismatch between the source (apparently written against an assumed-newer or
assumed-older Compose surface) and the Compose BOM (`2024.12.01`) actually pinned in
`libs.versions.toml`:

| Root cause | Symptom | Fix | Files |
|---|---|---|---|
| `androidx.compose.material3.ripple.ripple()` doesn't exist until material3 1.4.0; the pinned BOM resolves material3 1.3.1 | `Unresolved reference 'ripple'` | Switched to `androidx.compose.material.ripple.rememberRipple()` (present, functional, but deprecated at **error** level in material-ripple 1.7.6 — suppressed per-file with a documented `@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")`, since no working non-deprecated replacement exists in this BOM) | 10 files |
| `RowScope.weight()`/`ColumnScope.weight()` became **interface members** as of Compose Foundation ~1.6, not top-level extension functions — the old `import androidx.compose.foundation.layout.weight` now binds to an unrelated *internal* property of the same name | `Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file` | Deleted the obsolete import; `weight()` now resolves automatically as a scope member | 8 files |
| `InfiniteTransition.animateFloat` needs an explicit import (`androidx.compose.animation.core.animateFloat`) — never imported | `Unresolved reference 'animateFloat'` + cascading "cannot infer type" errors | Added the import | 2 files |
| `Modifier.graphicsLayer{}` lives in `androidx.compose.ui.graphics`, not `androidx.compose.ui.draw` | `Unresolved reference 'graphicsLayer'` | Fixed the import package | 1 file |
| `DrawScope.center` is a `DrawScope` member — used outside `onDrawBehind{}`, inside the size/density-only `CacheDrawScope` receiver, where it doesn't exist | `Unresolved reference 'center'` | Computed the offset manually from `this.size` instead of relying on the member | 1 file |
| `Modifier.padding(horizontal =, bottom =)` — not a real overload (only `horizontal+vertical` or all four edges) | `None of the following candidates is applicable` | Changed to `padding(start =, end =, bottom =)` | 1 file |

**Verification:** `./gradlew :app:compileDebugKotlin` now succeeds cleanly across all 18 modules.
This is the actual, load-bearing precondition for every item below — none of it was previously
possible to verify by running the app, only by reading the source.

**Also fixed:** the repository had no version control (`git init` was never run). A `.gitignore`
(excluding `local.properties`, keystores, secrets, build output, generated Room schemas) and an
initial commit now exist — required groundwork for item 8 (CI/CD) and for this work itself to be
safely checkpointed going forward.

---

## 1. How to read the rest of this document

- **Effort** is calendar time for one focused engineer, assuming the Critical-tier items are done
  in priority order (later estimates assume earlier infrastructure — e.g. tests — already exists).
- **Blocker** means something beyond writing code: a secret only the user holds, an external
  account, a product decision, or a policy/compliance step.
- Every "already exists" claim was checked against the current source tree just now, in this
  session — not carried over from earlier phase docs, which in a few places (Phase 6/7 READMEs)
  describe intent rather than the code's current, buildable state.

---

## 2. Critical — required to call this Version 1.0

### 2.1 AI Provider Integration — ✅ Done (2026-07-29)
**Effort spent: ~1 day (Claude, OpenAI, Gemini, Ollama).**

Real HTTP-backed `generate`/`generateStream` now ships for four of the five providers: Claude
(Anthropic Messages API, SSE), OpenAI (Chat Completions, SSE), Gemini (`generateContent`/
`streamGenerateContent`, SSE), Ollama (local HTTP, newline-delimited JSON, no key). API keys are
stored via `EncryptedSharedPreferences` (`AndroidProviderCredentialStore`), entered through a real
Settings → AI Providers UI, `INTERNET` is declared, and a scoped `network_security_config.xml`
permits Ollama's loopback cleartext traffic without weakening the default HTTPS-only policy for
everything else. 27 tests (`core-providers/src/test`, the project's first test source set) cover
request shape, response parsing, and error mapping for all four against a real `MockWebServer`.
Full detail: [AI_PROVIDER_INTEGRATION.md](AI_PROVIDER_INTEGRATION.md).

`LocalModelProvider` (on-device inference via MediaPipe LLM Inference or ONNX Runtime) remains
**not** included — it's a different kind of work (bundling or downloading a model file, not an HTTP
client) and stays tracked separately under High (2.11).

**Discovered and fixed along the way (not part of the original estimate):** the project had never
actually been built end-to-end before this milestone — §0's fixes were necessary preconditions, not
optional. Additionally, wiring real dependencies surfaced three more real bugs no one had hit yet:
an AGP/compileSdk ceiling conflict from OkHttp 5.x's Android-specific artifact (resolved by pinning
`okhttp` to `4.12.0`), a Kotlin binary-metadata version mismatch from `kotlinx-serialization-json`
`1.11.0` requiring a newer Kotlin than this project pins (resolved by pinning to `1.7.3`), and two
separate instances of the same Dagger/Kotlin gotcha — a constructor parameter's Kotlin default value
is invisible to Dagger's generated code, so `CoroutineDispatcher` needed a proper `@IoDispatcher`
qualifier and a test-only `apiBaseUrl` override had to become a settable `internal` property instead
of a constructor parameter, not an unqualified (and dangerously ambiguous) `String` binding.

### 2.2 Voice Runtime — ✅ Done (2026-07-29)
**Effort spent: ~1 day.**

The orb is a real push-to-talk voice interface now: `SpeechRecognizer`-based streaming
speech-to-text (with live partial transcripts), `TextToSpeech` output honoring the persisted
`AssistantVoice` persona (via pitch/rate mapping — no per-persona synthesized model), tap-triggered
interruption (barge-in), automatic conversation continuation (bounded by the recognizer's own
silence timeout), transient audio focus, and a fresh mic-permission check at the point of use. Full
detail, including what was deliberately scoped out (acoustic barge-in, wake-word listening) and why:
[VOICE_RUNTIME.md](VOICE_RUNTIME.md). 10 new tests (`app/src/test`, the `:app` module's first test
source set) cover every piece of pure logic involved. Along the way, a full project-wide
`./gradlew lintDebug` run (previously only ever run scoped to `:app`) surfaced two real,
pre-existing `MissingPermission` risks in `core-actions` (`NotifyTool`, `ReminderBroadcastReceiver`)
— both already checked permission correctly at runtime but tripped a documented Lint limitation for
`NotificationManagerCompat.notify`; fixed with justified `@SuppressLint` annotations, not code
changes, since the underlying logic was already correct.

### 2.3 Android Automation — ✅ Done (2026-07-29)
**Effort spent: ~1 day.**

Eight new tools (`open_settings`, `clipboard`, `share`, `file_search`, `media_control`,
`schedule_action`, `read_screen`, `device_navigate`), a `Tool.requiresConfirmation` gate
(`ActionEngine.execute()` refuses sensitive tools; `executeConfirmed()` — reached only from a
direct UI tap — doesn't), the project's first WorkManager usage (`schedule_action` +
`DeferredActionWorker`, Hilt-integrated via `HiltWorkerFactory`), and a narrowly-scoped
`AuraAccessibilityService` (reads on-screen text, presses back/home/recents — no gesture dispatch
or simulated taps on other apps, a deliberate scope boundary, not an oversight). Full detail:
[ANDROID_AUTOMATION.md](ANDROID_AUTOMATION.md).

Along the way, adding WorkManager surfaced a real Room-version-drift build-stability issue
(fixed: Room bumped to 2.8.4, `kotlinx-serialization-json` aligned to 1.8.1, schema export turned
off since there are no migrations yet to justify the fragility) and the fact that
`assembleRelease` had never once succeeded in this project — fixed with the standard, AGP-generated
`-dontwarn` rules for Tink's error-prone annotations. See ANDROID_AUTOMATION.md §5.

### 2.4 Vision AI — ✅ Done (2026-07-29)
**Effort spent: ~1 day.**

Four new tools: `ocr`, `scan_barcode` (covers QR too — one ML Kit API handles both), `scan_receipt`
(OCR + heuristic total/date/merchant parsing, honestly non-guaranteed), and `understand_image`
(routes to whichever connected provider supports vision, via `AIProviderManager` — the same call
path `CodingAgent` already uses for text). `AiMessage` gained two nullable, fully-additive fields
(`imageBase64`, `imageMimeType`); Claude/OpenAI/Gemini each map them into their own real vision
wire format. A camera-capture Quick Action (`FileProvider`, `CAMERA` permission) feeds `ocr` for
the "photograph something, extract its text" case — true edge-detected multi-page document
scanning wasn't built (see [VISION_RUNTIME.md §4](VISION_RUNTIME.md#4-document-scanner--what-shipped-what-didnt)).
`VisionAgent` (core-agents, Phase 6) still fails immediately by design — nothing in this milestone
routes through it; the new tools are reached the same way every other tool is, through
`AgentSelectionEngine`/`ActionEngine`. Full detail: [VISION_RUNTIME.md](VISION_RUNTIME.md).

### 2.5 Knowledge Graph — ✅ Done (2026-07-29)
**Effort spent: ~1 day.**

`MemoryEntry.relationships`/`RelationshipType` existed since Phase 4 but nothing ever populated or
queried them — this milestone is that missing other half, not a new schema.
`KnowledgeGraph`/`DefaultKnowledgeGraph` (query: `relatedTo`, transitive `neighborhood`) sits over
the existing `LongTermMemoryStore`, no `GraphNode`/`GraphEdge` table. `RelationshipLinker` (pure,
word-overlap heuristic, honestly not real entity resolution) auto-links a newly-extracted memory to
existing `Project`/`Person` memories, wired into `MemoryAgent.remember()` — six new
`MemoryCategory` values (`Person`, `Document`, `Meeting`, `Assignment`, `Research`, `File`) plus
real classifier trigger phrases make the brief's eight named entity types (with `Project`/`Task`,
already existing) actually reachable from conversation text. 12 new tests. Persistence remains
deferred to the same already-tracked High-priority "Persistent core-memory storage" item — see
[KNOWLEDGE_GRAPH.md §6](KNOWLEDGE_GRAPH.md#6-whats-still-honestly-not-done). Full detail:
[KNOWLEDGE_GRAPH.md](KNOWLEDGE_GRAPH.md).

### 2.6 Workflow Engine — ✅ Done (2026-07-29)
**Effort spent: ~1 day.**

A persisted `Workflow` model (Room-backed, `WorkflowStep`s using the same `(toolName, arguments)`
shape every tool call already uses), `WorkflowManager` coordinating persistence +
`WorkflowScheduler` (`WorkManager` `PeriodicWorkRequest`, Daily/Weekly) + `WorkflowExecutor`
(stop-on-first-failure, matching `PlanExecutor`'s own convention), and a new "Workflows" section in
the Automate tab: the five named brief examples as one-tap presets, plus enable/disable/delete/run-
now for whatever the user adds. Custom step-by-step editing wasn't built — presets plus
enable/disable/delete is what "Editing" means today. 9 new tests. Full detail:
[WORKFLOW_ENGINE.md](WORKFLOW_ENGINE.md).

### 2.7 Testing — ✅ Done (2026-07-29)
**Effort spent: ~1 day for the highest-blast-radius targets (see below); the original 8–10 day
estimate assumed meaningful coverage across all 18 modules, which this pass deliberately did not
attempt — see "what's still untested" below.**

`core-reasoning`, `core-agents`, and `core-orchestrator` — the highest-value/highest-blast-radius
targets this section originally called out by name — went from zero tests to 74 (44/12/18
respectively), on top of the 70 tests four other modules already had (`core-providers`,
`:app`, `core-actions`, `core-memory`). Full detail: [TESTING.md](TESTING.md).

**What was covered, following the exact priority this section specified**: `DefaultDecisionEngine`
and `DefaultConfidenceEvaluator` (the reasoning engine's core pure logic), `DefaultConstraintEngine`,
`RuleBasedTaskDecomposer`, `DefaultGoalManager`, `DefaultCapabilityResolver`,
`DefaultAgentSelectionEngine` (the capability-closure resolution this section named directly),
`DefaultResultAggregator`, `DefaultTaskDispatcher` (retry/timeout logic), and the shared
`ToolBackedAgent`/`DefaultAgentRegistry` logic underneath five of AURA's nine agents.

**What's still untested, honestly**: `DefaultReasoningEngine` and `DefaultAgentOrchestrator`/
`DefaultExecutionCoordinator` (the top-level classes composing everything above — properly
integration-test targets, not another unit-test file each); `ConversationPipeline`'s own routing
decision (this section's other named target — not reached this pass); individual agent-specific
logic in the 9 concrete agents beyond their shared base; repository/DAO round-trips; any
Compose/UI test (no device/emulator in this environment). No Jacoco coverage tooling is configured,
so no line-coverage percentage is claimed anywhere in this project — see `TESTING.md` §4.

### 2.8 CI/CD — ✅ Done (2026-07-29)
`.github/workflows/ci.yml` runs `ktlintCheck`, `detekt`, `test`, `lintDebug`, `assembleDebug`, and
`assembleRelease` (three parallel jobs) on every push/PR to `main`. `ktlint`/`detekt` were both
added for real across all 18 modules, not just referenced in the workflow — see
[CI_CD.md](CI_CD.md) §2 for how ~460 pre-existing findings across ~8 milestones of code were
handled (ktlint auto-fixed; detekt baselined) without either a multi-day retroactive cleanup or
silently skipping the tools.

**What's still genuinely blocked, not solved**: this repository has no configured git remote (see
§0 — version control itself was only initialized this session). A CI provider needs somewhere to
actually run; pushing `ci.yml` doesn't execute it anywhere until the repository is hosted. Verified
locally instead, by running the exact command sequence the workflow runs, end-to-end, in one pass.
See [CI_CD.md](CI_CD.md) §3.

### 2.9 Security — ✅ Done (2026-07-29)
**Original estimate: 3–4 days.** Largely landed incidentally as part of earlier milestones —
`EncryptedSharedPreferences` for API keys, `networkSecurityConfig`, and ProGuard rules were all
built during AI Provider Integration (§2.1) and Android Automation (§2.3), not deferred to this
item as originally planned. What remained once this item was picked up directly: an actual
line-by-line *review* of all of the above plus logging, backup, data deletion, and export/import —
which surfaced and fixed three real gaps (backup not excluding the encrypted credential file, a
plugin-logging facade that could leak to release logcat, and a "Clear all memory" UI control that
was completely unwired despite claiming to be a real, irreversible action). Full detail, including
what was found but deliberately deferred (export/import): [SECURITY.md](SECURITY.md).

### 2.10 Production Release — ✅ Done (2026-07-30)
Everything engineering could deliver without the app publisher's own secrets/accounts is done: an
optional release signing config (`keystore.properties`, gitignored, absent today — `release`
correctly stays unsigned, exactly as before), verified `assembleRelease` *and* `bundleRelease`
builds, a dependency-free `LocalCrashLogger` uncaught-exception hook, a real Profile → About
screen, `docs/PRIVACY_POLICY.md` (built from `docs/SECURITY.md`'s actual findings, covering every
declared permission), and `docs/THIRD_PARTY_LICENSES.md`. App icons/adaptive icons/splash assets
were found already real, not placeholders, and needed no work.

**Genuinely still blocked, not solved**: a real release keystore (the user's own secret — cannot
be generated and held on their behalf without them owning and backing it up) and a Google Play
Console developer account (the user's own external account, $25 registration) — both named
explicitly in this project's own stated exception for blockers requiring a human decision. Full
detail: [PRODUCTION_RELEASE.md](PRODUCTION_RELEASE.md).

### 2.11 Performance — ✅ Done (2026-07-29)
**Not one of this section's original 10 Critical items** (this doc's §2 tracks the first,
higher-level milestone brief's exact 10-item list). The later, more detailed "AURA FINAL VERSION
1.0 COMPLETION" brief added it as its own required Milestone 7, between Testing and Security —
included here for completeness, in that sequence position.

Static, code-level review only — no device/emulator exists in this environment, so no measured
runtime profiling (no Macrobenchmark, no Perfetto trace) was possible. Three real, safe fixes
applied: `ConversationPipeline.run()` (the CPU-bound intent/memory/reasoning pipeline) moved onto
`Dispatchers.Default`; an index added to `chat_messages.timestampMillis` (the one unbounded-growth
table, sorted on every read); a Compose recomposition-stability fix in the debug `TraceOverlay`.
One real finding — `AuraApplication`'s eager `Set<Tool>`/`Set<Agent>` Hilt injection — documented
but deliberately left unfixed: each constructor is individually cheap, and a proper fix means a
real DI redesign this brief's own "do not redesign the architecture" instruction argues against
making without a device to confirm it helps. Full detail: [PERFORMANCE.md](PERFORMANCE.md).

---

## 3. High — expected soon after 1.0, not blocking it

| Item | Why it's not Critical | Effort |
|---|---|---|
| **2.11 LocalModelProvider (on-device inference)** | A genuinely different implementation (MediaPipe/ONNX bundling a model file) from the HTTP-based providers in 2.1 — the app is fully usable with cloud providers alone | 5–7 days |
| **Persistent `core-memory`/`core-plugin` storage** | Currently in-memory only (Platform Review finding 5, still true) — real but survivable for a 1.0 that expects fresh installs | 3–4 days |
| **Plugin marketplace `install`/`checkForUpdates`** | Honestly `NotSupported` today (Phase 7's own scope boundary) — the 2 bundled example plugins (`DiceRollerPlugin`, `WordCounterPlugin`) already prove the runtime works | 4–5 days |
| **Conversation/memory export & backup** | Data-loss risk without it, but not a launch blocker | 2–3 days |
| **Rate limiting / per-provider usage & cost visibility** | Matters once 2.1 makes real API calls with real billing | 2 days |
| **Biometric unlock wiring** | `USE_BIOMETRIC` is declared and `biometric` is a dependency, but nothing in `presentation/login` calls the `BiometricPrompt` API yet | 1–2 days |

## 4. Medium

- Accessibility (TalkBack/screen-reader) pass over the existing Compose UI — none of it has been audited for semantics/content descriptions beyond a few explicit `contentDescription` params already present.
- Tablet/foldable responsive layouts (current UI assumes a single phone form factor).
- Localization — all strings are hardcoded English.
- Notification channel customization UI (`NotificationChannels.kt` exists but is not user-configurable).
- Onboarding polish and empty-state illustrations.
- Dynamic plugin code loading — explicitly out of scope by Phase 7's own design (security boundary), not a gap.

## 5. Low

- Wear OS companion.
- Home-screen widgets.
- Multi-account/profile support.
- Analytics dashboards beyond basic crash reporting.

---

## 6. Milestone discipline for everything above

Per item completed in the Critical tier, before moving to the next:

1. **Static analysis** — `./gradlew ktlintCheck detekt` (both need adding to the project; neither exists yet — first Critical-tier PR should add them since every subsequent PR depends on them existing).
2. **Tests** — `./gradlew test` (meaningful only once 2.7 exists; until then, this step is "does it still compile and manually trace correctly").
3. **Architecture review** — re-run the same import-boundary grep sweep used in the Platform Review audit; no new module may introduce a cyclic or upward dependency.
4. **Documentation** — update the relevant `docs/*.md` for the subsystem touched.
5. **CHANGELOG.md** — one entry per completed item.
6. **ROADMAP.md** — move the item from "planned" to "done," re-sequence what's next.
7. **VERSION.md** — bump the pre-1.0 version number; 1.0.0 is reserved for when every Critical item above is done.

---

## 7. Priority order (as specified)

1. AI Provider Integration — **✅ done 2026-07-29** (4 of 5 providers real; `LocalModelProvider` deferred to 2.11)
2. Voice Runtime — **✅ done 2026-07-29** (push-to-talk; acoustic barge-in and wake-word listening deliberately deferred, see VOICE_RUNTIME.md §8)
3. Android Automation — **✅ done 2026-07-29** (8 new tools, confirmation gate, WorkManager, scoped accessibility service; gesture/tap simulation deliberately deferred, see ANDROID_AUTOMATION.md §4)
4. Vision AI — **✅ done 2026-07-29** (4 new tools, `AiMessage` image fields, camera capture; edge-detected document scanning deliberately deferred, see VISION_RUNTIME.md §4)
5. Knowledge Graph — **✅ done 2026-07-29** (relationship query/auto-link over the existing `MemoryEntry.relationships` field, not a new schema; real entity resolution deliberately deferred, see KNOWLEDGE_GRAPH.md §3/§6)
6. Workflow Engine — **✅ done 2026-07-29** (built immediately after Vision AI, per the "AURA FINAL VERSION 1.0 COMPLETION" milestone brief's own numbering — Milestone 4 there, ahead of Knowledge Graph — rather than this list's original 5/6 ordering; see WORKFLOW_ENGINE.md)
7. Testing — **✅ done 2026-07-29** (74 new tests across the three modules that had zero — `core-reasoning`, `core-orchestrator`, `core-agents` — prioritized by blast radius exactly as this section specified; no coverage-percentage tooling exists in this project, so none is claimed, see TESTING.md §4)
8. CI/CD — **✅ done 2026-07-29** (`.github/workflows/ci.yml`; real ktlint/detekt added across all 18 modules, not just referenced; one genuine blocker flagged, not solved — no git remote configured, so nothing actually runs the workflow yet — see CI_CD.md §3)
9. Security — **✅ done 2026-07-29** (three real fixes: unwired "Clear all memory" now actually deletes data behind a confirmation dialog, the encrypted API-key store excluded from Android backup/device-transfer, the plugin log sink is a real no-op in release builds; export/import found unwired too, deliberately deferred to the already-tracked separate roadmap item rather than rushed — see SECURITY.md §5)
10. Production Release — **✅ done 2026-07-30** (signing config wired but no keystore exists — the user's own secret; crash-logging hook added without a third-party account; About/Privacy Policy/Licenses added; app icons/splash confirmed already real; a Play Console account remains the user's own — see PRODUCTION_RELEASE.md §6)

**All ten Critical items toward Version 1.0 are now done — see [`AURA_V1_COMPLETION_REPORT.md`](../AURA_V1_COMPLETION_REPORT.md) for the full picture.**
