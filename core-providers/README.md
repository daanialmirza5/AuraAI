# core-providers

Every model backend AURA can talk to, all behind one interface (`AIProvider`, from core-ai) and
one router (`AIProviderManager`). Pure Kotlin/JVM; depends only on core-ai.

## What lives here

- **`AIProviderManager`** — the single point of contact for "generate a completion." Everything
  above this module (core-planner, the app's future ViewModels) talks to this, never to a
  concrete provider. `selectProvider(id)` is the entire surface area of "switching providers."
- **`ScaffoldAIProvider`** — shared base class for every provider below: correctly reports what
  it *would* support once connected (`ProviderCapabilities`), but every call that would need a
  network client fails with `AuraError.ProviderNotConnected` instead of faking a result.
- **`GeminiProvider`, `OpenAIProvider`, `ClaudeProvider`, `OllamaProvider`, `LocalModelProvider`**
  — the 5 concrete (scaffolded) providers, each with capabilities reflecting its real-world
  feature set (Gemini's huge context window, Ollama/LocalModel's `isLocal = true`, etc.) so
  routing decisions can be made correctly the moment a provider actually goes live.
- **`ProviderConfig`** — the *shape* of a provider's configuration (model name, API key, base
  URL). No real key ever lives in this codebase; when a provider is connected, a value here
  comes from encrypted storage supplied by the app, never a source literal.

## ⚠️ Status: intentionally not connected

**No provider in this module makes a network call.** This was explicit in the brief for this
phase ("Do not directly integrate OpenAI, Claude, Gemini, or any cloud model yet... Do not
connect any provider yet"). `generate()`/`generateStream()` on every provider return
`AuraResult.Failure(AuraError.ProviderNotConnected(...))`; `isAvailable()` returns `false` for
all five, honestly.

## What "connecting" a provider actually means, when that phase comes

For a cloud provider (Gemini/OpenAI/Claude): add an HTTP client dependency (Ktor or Retrofit),
implement `generate`/`generateStream`/`isAvailable` for real in that provider's own class (or a
new subclass — `ScaffoldAIProvider` doesn't have to be the parent forever), and supply a real
`ProviderConfig` with an API key from encrypted storage via DI instead of the hardcoded
placeholder `config` each class currently holds.

For Ollama: same shape, pointed at a local/LAN HTTP server instead of a cloud endpoint — no API
key, `capabilities.isLocal = true` already reflects that.

For LocalModel: a genuinely different implementation path (MediaPipe LLM Inference or ONNX
Runtime running in-process) rather than an HTTP client at all — this is the one provider that
would need this module (or a sibling) to become Android-aware to load a bundled/downloaded model
file, which it currently isn't.

**None of this requires touching core-ai, core-intent, core-planner, core-tools, or core-actions.**
That's the point of the architecture this phase built.
