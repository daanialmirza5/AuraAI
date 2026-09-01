# core-plugin

The host's side of the plugin sandbox boundary — real access to `core-tools`/`core-intent`, held
here and nowhere a plugin can reach it. Pure Kotlin/JVM; depends on `plugin-api`, `core-ai`,
`core-tools`, `core-intent`. See `docs/PLUGIN_SDK.md` and `docs/PLUGIN_SECURITY.md` at the project
root for the full picture; this file is the implementation-level reference.

## What lives here

```
com.aura.ai.core.plugin/
├── HostSdk.kt                the running host's own PluginVersion
├── model/                     PluginState, PluginRecord — host-side bookkeeping
├── compatibility/               PluginCompatibilityChecker — version-range validation
├── permission/                   PluginPermissionPolicy — declared vs. granted permissions
├── registry/                      PluginRegistry — the plugin-system analogue of AgentRegistry
├── registrar/                      Real implementations of the plugin-api registrar interfaces,
│                                   including PluginBackedTool (the core-tools.Tool adapter) and
│                                   the independent, string-keyed PluginCapabilityRegistry /
│                                   PluginIntentRegistry
├── intent/                         PluginAwareIntentRecognizer — the IntentRecognizer decorator
├── storage/, settings/              PluginStorageHost / PluginSettingsHost — namespaced-by-plugin-id
├── event/                           DefaultPluginEventBus — the plugin-facing pub/sub bus
├── context/                         DefaultPluginContext + its factory — the sandbox boundary itself
└── health/                          PluginHealthMonitor — queries every loaded plugin's health() on demand
```

### `PluginBackedTool` — dynamic tool registration made real

Wraps a plugin's `PluginToolDescriptor` as an ordinary `core-tools.Tool`. Once registered,
`core-actions.ActionEngine`, `core-agents.ToolBackedAgent`, and `core-orchestrator`'s dispatch
can't tell it apart from a built-in tool — genuinely invokable through the exact same path.

### `PluginAwareIntentRecognizer` — dynamic intent registration made real

Decorates `core-intent.KeywordIntentRecognizer` rather than replacing it: the built-in recognizer
always gets first attempt, and a plugin's trigger phrases are only consulted once it's fallen all
the way through to its own honest "I don't know" (`Conversation`, confidence ≤ 0.3). A match
reports `IntentType.Automation` with the plugin's id and intent id in `RecognizedIntent.slots`.
Bound in `:app`'s `AiCoreModule` in place of `KeywordIntentRecognizer` directly — a one-line swap,
same pattern every phase since Phase 3.

### Why plugin capabilities are their own registry

`PluginCapabilityRegistry` is deliberately independent of `core-capabilities.CapabilityRegistry` —
a plugin can't be constrained to the host's closed, 11-value `Capability` enum decided before the
plugin existed. It's a genuine, working, string-keyed, bidirectional registry in its own right;
see `docs/PLUGIN_API.md` for the honest gap this creates (not yet consulted by
`core-orchestrator`'s agent-selection closure).

## Status

Every component here is real and runs today, entirely locally. `DefaultPluginPermissionPolicy`'s
"grant everything declared" default is intentionally permissive for this phase's plugin source
(compiled into the app) — see its own doc for exactly where a stricter policy plugs in later.
