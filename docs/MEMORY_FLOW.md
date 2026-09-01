# AURA AI — Memory Flow

Runtime behavior: how a `MemoryEntry` gets created, how it's chosen for context, how it decays and
is eventually forgotten. See [MEMORY_SCHEMA.md](MEMORY_SCHEMA.md) for field definitions and
[MEMORY_ARCHITECTURE.md](MEMORY_ARCHITECTURE.md) for the component map these sequences move
through.

---

## 1. Extraction flow — from raw text to a stored memory

```mermaid
sequenceDiagram
    participant User
    participant Extractor as RuleBasedMemoryExtractor
    participant Classifier as RuleBasedMemoryClassifier
    participant DateParser as DateExpressionParser
    participant Store as LongTermMemoryStore

    User->>Extractor: extract("My exam is next Monday")
    Extractor->>Extractor: split into clauses
    loop each clause
        Extractor->>Classifier: classify(clause)
        Classifier-->>Extractor: MemoryClassification(category, confidence, tags)
        alt category == General AND confidence < 0.5
            Extractor->>Extractor: drop clause (not memory-worthy)
        else
            Extractor->>Extractor: build MemoryEntry(content, category, importance, confidence, tags)
            opt category == Goal
                Extractor->>DateParser: parseEpochMillis(clause)
                DateParser-->>Extractor: epoch millis, or null
                opt date resolved
                    Extractor->>Extractor: tag "calendar", attach suggestedActionToolName = "create_calendar_event"
                end
            end
            Extractor-->>User: MemoryExtractionResult(memory, suggestedActionToolName?, suggestedActionParameters)
        end
    end
    User->>Store: remember(memory)  note: caller's responsibility — extraction only proposes
```

`MemoryExtractor.extract()` does not itself write to any store — it returns proposals. A caller
(today, wherever conversation turns are processed; a future conversation orchestrator) decides
whether to `remember()` the result and whether to actually run `suggestedActionToolName`. This
keeps extraction a pure, side-effect-free function to reason about and test independently of
storage or action execution.

---

## 2. The four worked examples, traced

| Input | Clause classification | Rule that fires | Result |
|---|---|---|---|
| "My birthday is June 20" | `Preference`, confidence ≈ 0.70 | trigger `"my birthday"` | `MemoryEntry(category = Preference, tags = ["preference"])` |
| "I am working on RootAI" | `Project`, confidence ≈ 0.70 | trigger `"i am working on"` | `MemoryEntry(category = Project, tags = ["project"])` |
| "My exam is next Monday" | `Goal`, confidence ≈ 0.70 | triggers `"my exam"` + weekday `"monday"` (2 hits → higher confidence) | `MemoryEntry(category = Goal, tags = ["goal", "calendar"])` **+** `suggestedActionToolName = "create_calendar_event"`, `suggestedActionParameters = {title: "My exam is next Monday", dateMillis: <next Monday, 00:00 local>}` |
| "I prefer Kotlin" | `Preference`, confidence ≈ 0.70 | trigger `"i prefer"` | `MemoryEntry(category = Preference, tags = ["preference"])` |

"My exam is next Monday" is the only one of the four that produces **two** outputs at once — the
`Goal` memory itself, and a suggested `create_calendar_event` action — exactly the brief's "Goal +
Calendar Memory" example. `DateExpressionParser` resolves "next Monday" to the *next upcoming*
Monday from whatever `LocalDate.now()` is at extraction time, matching how the phrase is used in
casual speech; "today" and "tomorrow" are handled the same way.

`RuleBasedMemoryClassifier` scores every category's trigger list against the clause and picks the
highest hit count (ties broken by rule order), the same scored-keyword-matching approach as Phase
3's `KeywordIntentRecognizer` — not first-match, because some phrases genuinely score on more than
one category (e.g. "I need to finish my project" touches both `Task` and `Project`).

---

## 3. Context-building flow — "before every AI request"

```mermaid
sequenceDiagram
    participant Caller
    participant CB as DefaultContextBuilder
    participant Retriever as DefaultMemoryRetriever
    participant WMS as WorkingMemoryStore
    participant LTS as LongTermMemoryStore
    participant Ranker as WeightedMemoryRanker
    participant Embed as EmbeddingProvider

    Caller->>CB: build(ContextRequest(query, maxMemories, maxContextCharacters, ...))
    CB->>Retriever: retrieve(query, limit = 50)
    Retriever->>WMS: getActive()
    WMS-->>Retriever: working memories (priority)
    Retriever->>LTS: recall(query, limit = 50)
    LTS-->>Retriever: long-term matches (touches lastAccessedAtMillis)
    Retriever-->>CB: merged candidates, deduplicated by id

    CB->>Ranker: rank(candidates, RankingContext(query, conversation, goals, task))
    Ranker->>Embed: embed(query)
    alt embedding available
        Embed-->>Ranker: AuraResult.Success(vector)
        Ranker->>Ranker: cosine similarity per memory with a computed embedding
    else not available (today, always)
        Embed-->>Ranker: AuraResult.Failure(ProviderNotConnected)
        Ranker->>Ranker: Jaccard similarity fallback
    end
    Ranker-->>CB: List~ScoredMemory~, sorted by finalScore desc

    CB->>CB: deduplicate() — drop near-duplicates (Jaccard ≥ 0.85), keep best-ranked survivor
    CB->>CB: selectWithinBudget() — greedy walk until maxContextCharacters
    CB->>CB: take(maxMemories)
    CB-->>Caller: BuiltContext(memories, conversationTurns, estimatedCharacterCount, truncated)
```

This is the literal implementation of the brief's four requirements:

- *"Retrieve only relevant memories"* → `MemoryRetriever` casts a wide net, `MemoryRanker` narrows
  it to what's actually relevant to this query/conversation/goal/task.
- *"Construct optimized context"* → the ranked, deduplicated, budget-fit `BuiltContext`.
- *"Limit context size"* → `maxContextCharacters` (a character-count proxy for a token budget —
  see `ContextRequest`'s own doc comment for why it's named "characters" and not "tokens") and
  `maxMemories`, both enforced.
- *"Avoid duplicate memories"* → the `deduplicate()` step, using the same Jaccard-overlap
  machinery every other similarity check in this module shares.

`BuiltContext.truncated` is true only when real candidates were dropped for being *over budget* —
deduplication removing a near-duplicate is deliberately not counted as truncation, since dropping
a redundant copy is a quality improvement, not a budget-forced loss of information.

---

## 4. Lifecycle state transitions

```mermaid
stateDiagram-v2
    [*] --> Active: remember()
    Active --> Archived: archive() / expireStale()
    Active --> [*]: delete()
    Archived --> [*]: delete()
    Archived --> Active: update() with status = Active (manual un-archive)

    note right of Archived
        Excluded from recall()/observeAll()
        by default (includeArchived = false).
        Still present — reversible.
    end note
    note right of [*]
        Deletion is the only
        irreversible transition.
    end note
```

Two of the brief's four forgetting operations are direct `LongTermMemoryStore` passthroughs
(`archive`, `delete`); the other two are policy `DefaultMemoryLifecycleManager` builds on top:

### Expire

```mermaid
sequenceDiagram
    participant Scheduler as Caller (e.g. periodic job)
    participant LM as DefaultMemoryLifecycleManager
    participant LTS as LongTermMemoryStore

    Scheduler->>LM: expireStale()
    LM->>LTS: observeAll(includeArchived = false).first()
    LTS-->>LM: all active entries
    LM->>LM: filter { it.isExpired }
    loop each expired entry
        LM->>LTS: archive(id)
    end
    LM-->>Scheduler: AuraResult.Success(count archived)
```

Expiry **archives**, never deletes — a `ttlMillis` set too aggressively shouldn't mean
permanently lost information. Only an explicit `delete()` call removes data for good.

### Merge duplicates

```mermaid
sequenceDiagram
    participant Caller
    participant LM as DefaultMemoryLifecycleManager
    participant LTS as LongTermMemoryStore

    Caller->>LM: mergeDuplicates(threshold = 0.85)
    LM->>LTS: observeAll(includeArchived = false).first()
    LTS-->>LM: all active entries
    LM->>LM: sort by importance desc
    LM->>LM: greedy-cluster by Jaccard similarity ≥ threshold
    loop each cluster with > 1 member
        LM->>LM: survivor = highest-importance member
        LM->>LM: mergedTags = union of all members' tags
        LM->>LM: mergedRelationships = union, deduped by (targetId, type)
        LM->>LM: mergedConfidence = max across cluster
        LM->>LTS: update(survivor with merged fields)
        loop each non-survivor duplicate
            LM->>LTS: delete(duplicate.id)
        end
    end
    LM-->>Caller: AuraResult.Success(count removed)
```

Clustering is a single greedy pass (each entry joins the first existing cluster its content is
similar enough to, else starts a new one) rather than exhaustive pairwise comparison — appropriate
for the moderate entry counts an in-memory store is designed for; a future persisted store with a
much larger corpus would want this replaced with a proper approximate-duplicate index.

---

## 5. Semantic search flow

```mermaid
sequenceDiagram
    participant Caller
    participant Search as HybridSemanticSearch
    participant LTS as LongTermMemoryStore
    participant Ranker as WeightedMemoryRanker

    Caller->>Search: search(query, limit)
    Search->>LTS: recall(query = "", limit = 200)
    LTS-->>Search: broad candidate pool
    Search->>Ranker: rank(candidates, RankingContext(query = query))
    Ranker-->>Search: List~ScoredMemory~ sorted by relevance
    Search-->>Caller: take(limit)
```

Deliberately a thin composition rather than a third place reimplementing "try embeddings, fall
back to keywords" — that logic lives exactly once, in `WeightedMemoryRanker`'s similarity factor,
and `SemanticSearch` reuses it rather than duplicating it.

---

## 6. Working memory expiry (no lifecycle manager involved)

Unlike long-term memory, working memory needs no explicit expire step — expired entries are
filtered out at read time:

```mermaid
sequenceDiagram
    participant Caller
    participant WMS as InMemoryWorkingMemoryStore

    Caller->>WMS: getActive()
    WMS->>WMS: entries.values.filterNot { it.isExpired }
    WMS-->>Caller: only non-expired entries
```

This is intentionally simpler than long-term expiry: working memory has no "archived" concept to
preserve — it's meant to be cheap and disposable by design (see
[MEMORY_ARCHITECTURE.md §4](MEMORY_ARCHITECTURE.md#4-two-memory-stores-three-memory-layers)), so
silently excluding expired entries is enough.
