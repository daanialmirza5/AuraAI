# core-ai

The foundation every other AURA core module depends on, directly or transitively. Nothing in
this module depends on Android — it's a plain Kotlin/JVM module, deliberately, so it (and
anything built purely on top of it) can be unit tested without an emulator.

## What lives here

| File | What it is |
|---|---|
| `AuraResult.kt` | The result type every suspend function across every core module returns — `Success<T>` / `Failure(AuraError)` — instead of throwing. |
| `AuraError.kt` | The closed set of failure reasons (`Network`, `Authentication`, `RateLimited`, `ProviderNotConnected`, `RequiresProvider`, ...). |
| `AiMessage.kt` | A provider-agnostic chat turn (`role`, `content`) — distinct from the app's own persisted `ChatMessage`. |
| `ToolDescriptor.kt` / `ParameterSchema` | The provider-facing shape of a callable tool — what core-tools' `Tool` interface reduces to when advertised in a `GenerationRequest`. |
| `ProviderCapabilities.kt` | What a provider can do (`supportsStreaming`, `supportsTools`, `supportsVision`, `isLocal`, `maxContextTokens`). |
| `Generation.kt` | `GenerationRequest` / `GenerationResponse` / `GenerationChunk` / `ToolCallRequest` — the wire-level request/response shape every provider speaks. |
| `AIProvider.kt` | The one interface every model backend implements. |

## Why `ToolDescriptor` lives here, not in core-tools

core-tools depends on core-ai (its `Tool.toDescriptor()` maps into this module's type). If
`ToolDescriptor` lived in core-tools instead, core-ai would have to depend on core-tools to put
it in a `GenerationRequest`, and core-tools already depends on core-ai — a cycle. Keeping the
*shape* of a tool description in the foundation module, while the *behavior* (`Tool.execute`)
lives in core-tools, is what keeps the dependency graph a clean DAG.

## Status

Pure data types and one interface — nothing to "connect." This module doesn't change when a
provider goes live in a later phase; core-providers does.
