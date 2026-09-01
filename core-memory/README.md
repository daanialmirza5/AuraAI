# core-memory

AURA's memory intelligence engine — classification, extraction, ranking, retrieval, semantic
search, context construction, and forgetting, on top of two structured memory stores. Pure
Kotlin/JVM; depends only on `core-ai`. See `docs/MEMORY_ENGINE.md` at the project root for the
full architecture writeup (component map, sequence diagrams, worked examples, future vector
database integration); this file is the implementation-level, package-by-package reference.

## What lives here

```
com.aura.ai.core.memory/
├── ConversationBuffer.kt      raw AiMessage turns, rolling window
├── TextOverlap.kt             Jaccard word-overlap similarity — the shared keyword fallback
├── model/                     MemoryEntry and every type it's built from
├── store/                     WorkingMemoryStore + LongTermMemoryStore
├── classification/            MemoryClassifier — what kind of memory is this?
├── extraction/                MemoryExtractor — turn raw text into MemoryEntry proposals
├── ranking/                   MemoryRanker — 6-factor weighted scoring
├── embedding/                 EmbeddingProvider — provider-agnostic text-to-vector interface
├── retrieval/                 MemoryRetriever — broad candidate gathering
├── search/                    SemanticSearch — hybrid embedding/keyword search
├── context/                   ContextBuilder — retrieve, rank, dedupe, budget-fit for a request
└── lifecycle/                 MemoryLifecycleManager — archive, delete, expire, merge duplicates
```

### Three memory layers, not one

- **`ConversationBuffer`** — raw `AiMessage` turns fed into the next `AIProvider.generate()`
  call. `SlidingWindowConversationBuffer` keeps the last 20 non-system turns (a system prompt, if
  present, is never trimmed).
- **`WorkingMemoryStore`** — session-scoped, TTL-driven `MemoryEntry`s for what's salient *right
  now* ("the user just mentioned Monday's exam"). `InMemoryWorkingMemoryStore` silently excludes
  expired entries at read time.
- **`LongTermMemoryStore`** — durable, searchable, ranked `MemoryEntry`s meant to outlive the
  session. `InMemoryLongTermMemoryStore` is process-lifetime only this phase; not yet persisted
  to disk.

### `MemoryEntry` — the one schema everything shares

Every field the brief specifies (unique ID, timestamp, importance, recency, category, tags,
relationships, embedding placeholder, source, confidence, TTL) lives on `model/MemoryEntry.kt`.
Two axes are orthogonal by design: `MemoryTier` (`Working`/`LongTerm` — how long it's kept) and
`MemoryCategory` (`Preference`/`Goal`/`Project`/`Task`/`Knowledge`/`Episodic`/`General` — what kind
it is). Recency is deliberately *not* a stored field — `MemoryEntry.recencyScore()` computes it
fresh from `lastAccessedAtMillis` on every call, so it can never go stale. Full field-by-field
reference: `docs/MEMORY_SCHEMA.md`.

### Classification and extraction

`RuleBasedMemoryClassifier` scores a fixed set of trigger phrases per category (the same
scored-keyword approach as `core-intent`'s `KeywordIntentRecognizer`) and picks the highest-scoring
category, falling back to `General` when nothing matches confidently. `RuleBasedMemoryExtractor`
splits raw text into clauses, classifies each independently, and — for `Goal` clauses with a
resolvable date phrase ("next Monday," "tomorrow") — attaches a suggested `create_calendar_event`
action by tool *name* only, matching `core-actions`' `CalendarTool` by convention; this module has
no Gradle dependency on `core-actions` or `core-tools`.

### Ranking

`WeightedMemoryRanker` scores every candidate on all 6 factors the brief specifies (recency,
importance, semantic similarity, conversation context, user goals, current task) and combines them
via a tunable `RankingWeights` sum. Semantic similarity tries `EmbeddingProvider.embed()` first and
falls back to `TextOverlap.jaccardSimilarity` when no provider is connected (today, always) — every
sub-score is preserved on the returned `ScoredMemory`, not just the final weighted total.

### Embeddings — provider-agnostic

`EmbeddingProvider` mirrors `core-providers`' `AIProvider` interface pattern: one interface,
swappable local-or-cloud implementations. `NoOpEmbeddingProvider` is the only implementation this
phase ships, and it always fails honestly (`AuraError.ProviderNotConnected`) rather than returning
a fake vector. `VectorMath.cosineSimilarity` is ready for real vectors the moment a provider is
connected.

### Retrieval, semantic search, context building

`MemoryRetriever` gathers a broad candidate pool from both stores. `SemanticSearch` composes
`LongTermMemoryStore` + `MemoryRanker` for meaning-based lookup (not a separate similarity
implementation). `ContextBuilder` is the literal implementation of "before every AI request:
retrieve only relevant memories, construct optimized context, limit context size, avoid duplicate
memories" — retrieve → rank → deduplicate (Jaccard ≥ 0.85) → greedily fit within a character
budget → cap at `maxMemories`.

### Forgetting

`MemoryLifecycleManager` implements all four brief-required operations: `archive`/`delete` are
direct `LongTermMemoryStore` passthroughs; `expireStale()` scans for `MemoryEntry.isExpired` and
archives (never deletes) matches; `mergeDuplicates()` clusters near-duplicate active memories by
content similarity and collapses each cluster into its highest-importance member, unioning in tags
and relationships from the rest before deleting them.

## Relationship to the app's existing Memory feature

The app already has a persisted, Room-backed "Memory" the user sees on the Aura tab (things AURA
has learned, with a forget/✕ action). This module's `LongTermMemoryStore` is a **different,
AI-core-scoped** concept — what the reasoning layer itself reads/writes to build context, not what
's rendered on that screen. They're conceptually related (both are "AURA remembers things") but
intentionally not the same store in this phase. A later phase could back `LongTermMemoryStore` with
the app's real Room table (or a vector-indexed store — see `docs/MEMORY_ARCHITECTURE.md` §8) instead
of `InMemoryLongTermMemoryStore` — a DI swap in `AiCoreModule`, nothing above the interface would
need to change.

## Status

The full pipeline — extraction, classification, ranking, retrieval, context building, forgetting —
runs today, entirely locally, with no network calls. What's genuinely scaffolded rather than real:
embedding-based similarity (`NoOpEmbeddingProvider` always fails honestly, falling back to
keyword/Jaccard overlap everywhere it's needed) and persistence across process restarts (both
stores are in-memory only). Neither is a stub pretending to work — both are honest placeholders for
exactly what the brief asked to leave out this phase.
