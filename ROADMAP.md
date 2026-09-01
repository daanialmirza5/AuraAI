# AURA AI — Roadmap

**All ten Critical items toward Version 1.0 are done.** See
[`AURA_V1_COMPLETION_REPORT.md`](AURA_V1_COMPLETION_REPORT.md) for the full picture — what
shipped, what's verified, and what's next. Full detail, effort estimates, and blockers for every
item below live in [docs/TODO_V1.md](docs/TODO_V1.md); this file is the short status view.

## Done — Version 1.0

- [x] **Foundation** — the project now actually builds (`./gradlew assembleDebug` succeeds; it
      never had before this milestone). Git version control initialized. Full gap analysis written
      (`docs/TODO_V1.md`).
- [x] **1. AI Provider Integration** — real HTTP-backed Claude/OpenAI/Gemini/Ollama (SSE/NDJSON
      streaming, full error mapping), encrypted API key storage, working Settings UI, 27 tests
      against a real `MockWebServer`. `LocalModelProvider` deferred to High (on-device inference is
      different work). See [docs/AI_PROVIDER_INTEGRATION.md](docs/AI_PROVIDER_INTEGRATION.md).
- [x] **2. Voice Runtime** — push-to-talk voice over the existing `ConversationPipeline`: streaming
      STT with live partial transcripts, persona-mapped TTS, tap-triggered interruption, automatic
      conversation continuation, audio focus handling. Acoustic (no-tap) barge-in and wake-word
      listening deliberately deferred — see [docs/VOICE_RUNTIME.md](docs/VOICE_RUNTIME.md) §3/§8.
- [x] **3. Android Automation** — 8 new tools (settings, clipboard, share, file search, media
      control, WorkManager scheduling, scoped accessibility read/navigate), a confirmation gate
      for sensitive tools, `assembleRelease` fixed and verified for the first time. Gesture/tap
      simulation on other apps deliberately deferred — see
      [docs/ANDROID_AUTOMATION.md](docs/ANDROID_AUTOMATION.md) §4.
- [x] **4. Vision AI** — 4 new tools (`ocr`, `scan_barcode`, `scan_receipt`, `understand_image`),
      `AiMessage` extended with optional image fields (Claude/OpenAI/Gemini each map their own
      real vision wire format), a camera-capture Quick Action. Edge-detected multi-page document
      scanning deliberately deferred — see [docs/VISION_RUNTIME.md](docs/VISION_RUNTIME.md) §4.
- [x] **6. Workflow Engine** — persisted `Workflow` model, `WorkManager`-scheduled Daily/Weekly
      triggers, stop-on-first-failure execution, the five named brief examples as one-tap presets
      in a new Automate tab "Workflows" section. Built ahead of Knowledge Graph per this
      milestone's own numbering — see [docs/WORKFLOW_ENGINE.md](docs/WORKFLOW_ENGINE.md).
- [x] **5. Knowledge Graph** — `KnowledgeGraph` query layer (`relatedTo`, transitive
      `neighborhood`) over `MemoryEntry.relationships` (existed since Phase 4, never used until
      now — not a new schema), `RelationshipLinker` auto-linking new memories to `Project`/`Person`
      matches, 6 new `MemoryCategory` entity types with real classifier trigger phrases. Real
      entity resolution (vs. word-overlap) deliberately deferred — see
      [docs/KNOWLEDGE_GRAPH.md](docs/KNOWLEDGE_GRAPH.md) §3.
- [x] **7. Testing** — 74 new tests across `core-reasoning` (44), `core-orchestrator` (18), and
      `core-agents` (12) — the three modules that had zero, prioritized by blast radius: the
      decision engine, confidence scoring, constraint checking, agent selection, result
      aggregation, and task dispatch (retries/timeouts) that everything else in the multi-agent
      pipeline depends on. Found and documented one real, narrow gap in `DefaultCapabilityResolver`
      along the way. No coverage-percentage tooling exists in this project; deliberately not
      claimed — see [docs/TESTING.md](docs/TESTING.md) §4.
- [x] **Performance** — not one of the original 10 Critical items (added by the later, more
      detailed milestone brief as its own Milestone 7). Static, code-level review only — no
      device/emulator available. Fixed: `ConversationPipeline.run()` moved onto
      `Dispatchers.Default` (CPU-bound intent/memory/reasoning work was running on Main), an index
      on `chat_messages.timestampMillis`, a Compose recomposition fix in the debug `TraceOverlay`.
      One finding — `AuraApplication`'s eager tool/agent injection — documented, deliberately not
      fixed blind. See [docs/PERFORMANCE.md](docs/PERFORMANCE.md).
- [x] **9. Security** — reviewed permissions, API-key storage, logging, secrets-in-source, network
      security config, backup, ProGuard rules, data deletion, export/import, third-party sharing.
      Three real fixes: "Clear all memory" (Profile screen) had no `onClick` at all despite
      claiming to be a real, irreversible action — now actually deletes data behind a confirmation
      dialog; the encrypted API-key store excluded from Android backup/device-transfer; the plugin
      log sink is a real no-op in release builds. Export/import found unwired too, deliberately
      deferred to the already-tracked separate roadmap item below rather than rushed. See
      [docs/SECURITY.md](docs/SECURITY.md).

- [x] **8. CI/CD** — `.github/workflows/ci.yml` (static analysis, tests, build in parallel jobs).
      Real `ktlint`/`detekt` added across all 18 modules — `ktlintFormat` fixed every
      auto-fixable violation, `detekt` baselines ~460 pre-existing findings while failing on
      anything new. Verified locally by running the CI-equivalent sequence end-to-end. One genuine
      blocker flagged, not solved: no git remote is configured, so nothing actually runs the
      workflow yet — a hosting decision only the user can make. See
      [docs/CI_CD.md](docs/CI_CD.md).
- [x] **10. Production Release** — optional release signing wired from `keystore.properties`
      (absent — release stays unsigned exactly as before), `assembleRelease`/`bundleRelease` both
      verified, a dependency-free `LocalCrashLogger` hook, a real Profile → About screen,
      `docs/PRIVACY_POLICY.md` and `docs/THIRD_PARTY_LICENSES.md`. App icons/adaptive icons/splash
      confirmed already real. Genuinely still blocked, not solved: a real release keystore and a
      Google Play Console account — both the publisher's own secrets/accounts. See
      [docs/PRODUCTION_RELEASE.md](docs/PRODUCTION_RELEASE.md).

## High — soon after 1.0, not blocking it

- [ ] On-device `LocalModelProvider` (MediaPipe/ONNX — different work from the HTTP providers above).
- [ ] Persistent `core-memory`/`core-plugin` storage (currently in-memory only).
- [ ] Plugin marketplace `install`/`checkForUpdates` (honestly unsupported today).
- [ ] Conversation/memory export & backup.
- [ ] Per-provider usage/cost visibility.
- [ ] Biometric unlock wiring (`USE_BIOMETRIC` declared, dependency present, nothing calls it yet).

## Medium / Low

See [docs/TODO_V1.md §4–5](docs/TODO_V1.md#4-medium) — accessibility pass, responsive layouts,
localization, notification customization, Wear OS, widgets, multi-account, analytics dashboards.

---

*Last updated: 2026-07-30, alongside VERSION.md 1.0.0.*
