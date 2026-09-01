# AURA AI — Phase 4: Memory Intelligence Engine

**What this phase is:** the transformation of AURA from a conversation system into a
memory-driven AI assistant — automatic memory extraction and classification, multi-factor
ranking, provider-agnostic embeddings, semantic search, context construction, and memory
forgetting, all built inside the existing `core-memory` module. **What this phase is not:** a
connection to any cloud AI provider or embedding model. Every seam where one would plug in is
real, typed, and DI-wired; none of them call out to anything yet.

This is the cross-cutting entry point into Phase 4's three companion documents:

- **[MEMORY_SCHEMA.md](MEMORY_SCHEMA.md)** — the `MemoryEntry` field reference and every type it's
  built from.
- **[MEMORY_ARCHITECTURE.md](MEMORY_ARCHITECTURE.md)** — the component map, how the 14
  brief-requested items map to actual packages, and the future vector-database migration path.
- **[MEMORY_FLOW.md](MEMORY_FLOW.md)** — sequence diagrams for extraction, context building,
  semantic search, and forgetting; state transitions; the four worked examples traced end to end.

---

## 1. What changed

Phase 3 gave AURA a `MemoryStore` that could hold and substring-match flat text entries. Phase 4
replaces it entirely with a structured, multi-layered memory system:

| Phase 3 | Phase 4 |
|---|---|
| `MemoryStore` — one flat interface, substring search only | `LongTermMemoryStore` + `WorkingMemoryStore` — two stores, one per durability tier |
| No classification | `MemoryClassifier` — rule-based, 6 content categories + fallback |
| No extraction | `MemoryExtractor` — automatic, from raw utterances, clause-by-clause |
| No ranking | `MemoryRanker` — 6-factor weighted scoring |
| No embeddings | `EmbeddingProvider` — provider-agnostic interface, honest no-op default |
| No semantic search | `SemanticSearch` — hybrid embedding/keyword, composed from ranking |
| No context construction | `ContextBuilder` — retrieve, rank, dedupe, budget-truncate |
| No forgetting | `MemoryLifecycleManager` — archive, delete, expire, merge duplicates |

The old `MemoryEntry`/`MemoryStore`/`InMemoryMemoryStore` trio was deleted outright rather than
kept alongside the new schema — nothing in the app consumed it beyond a Hilt binding, and keeping
two competing memory schemas around would only invite them to drift out of sync.

---

## 2. The four worked examples — verified

| Input | Resolves to |
|---|---|
| "My birthday is June 20" | `Preference` memory |
| "I am working on RootAI" | `Project` memory |
| "My exam is next Monday" | `Goal` memory **+** suggested `create_calendar_event` action |
| "I prefer Kotlin" | `Preference` memory |

Traced rule-by-rule in [MEMORY_FLOW.md §2](MEMORY_FLOW.md#2-the-four-worked-examples-traced).
Verified by tracing `RuleBasedMemoryClassifier`'s trigger-matching logic by hand against each
input — this phase has no automated test suite to run these through mechanically (consistent with
Phases 1–3; no test infrastructure has been introduced in this codebase yet).

---

## 3. Design principles carried over from Phase 3

- **Honest failure over fake success.** `NoOpEmbeddingProvider` returns
  `AuraError.ProviderNotConnected` from every method, exactly like Phase 3's
  `ScaffoldAIProvider` — never a fabricated vector that would silently corrupt every downstream
  similarity score.
- **Try the real thing, gracefully fall back.** Every similarity computation
  (`WeightedMemoryRanker`, `HybridSemanticSearch`, `DefaultContextBuilder`'s deduplication) tries
  `EmbeddingProvider.embed()` first and falls back to `TextOverlap.jaccardSimilarity` when it
  fails — today, always. Swapping in a real provider changes the quality of every one of these
  computations without changing a single call site.
- **Provider-agnostic by construction.** `EmbeddingProvider` mirrors `core-providers`'
  `AIProvider` interface pattern precisely — see
  [MEMORY_ARCHITECTURE.md §5](MEMORY_ARCHITECTURE.md#5-embeddings-provider-agnostic-by-design).
- **Bridge to actions by name, not by dependency.** `MemoryExtractionResult.suggestedActionToolName`
  is a plain `String` ("create_calendar_event"), matching Phase 3's `CalendarTool` by convention —
  `core-memory` still has no Gradle dependency on `core-actions` or `core-tools`.
- **Computed, not cached-and-stale.** Recency is a function of `lastAccessedAtMillis`, not a
  stored field — see [MEMORY_SCHEMA.md §2](MEMORY_SCHEMA.md#2-why-recency-is-a-function-not-a-field).

---

## 4. What's real today vs. scaffolded for later

See [MEMORY_ARCHITECTURE.md §7](MEMORY_ARCHITECTURE.md#7-whats-real-vs-scaffolded) for the full
table. In short: the entire memory *pipeline* — extraction, classification, ranking, retrieval,
context building, forgetting — runs today, entirely locally, with no network calls. What's
missing is exactly what the brief asked to leave out: a connected embedding model, a connected
cloud AI provider, and persistence across process restarts (both stores are in-memory this phase,
same as Phase 3's `MemoryStore` was).

## 5. Future vector database integration

Covered in full in [MEMORY_ARCHITECTURE.md §8](MEMORY_ARCHITECTURE.md#8-future-vector-database-integration).
The short version: `EmbeddingProvider` is the seam for the model, `LongTermMemoryStore` is the
seam for where vectors live and how they're searched, and every consumer of both
(`MemoryRanker`, `SemanticSearch`, `ContextBuilder`, `MemoryLifecycleManager`) is written against
the interfaces, not the in-memory implementations — so connecting a real vector database later is
expected to be a two-`@Binds`-line change, not a rewrite.

---

## 6. Where to look next

- Implementation-level detail per component: `core-memory/README.md`.
- DI wiring: `app/src/main/java/com/aura/ai/ai/di/AiCoreModule.kt` (Phase 4 bindings are grouped
  under the `// --- Phase 4: Memory Intelligence ---` marker).
- The Phase 3 architecture this phase builds on: [PHASE3_CORE_INTELLIGENCE.md](PHASE3_CORE_INTELLIGENCE.md).
