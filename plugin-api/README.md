# plugin-api

The SDK a plugin author's code compiles against. Pure Kotlin/JVM, **zero project dependencies** —
not even `core-ai`. See `docs/PLUGIN_API.md` at the project root for the full type reference and
`docs/PLUGIN_SECURITY.md` for why "zero dependencies" is the whole isolation model, not
decoration.

## What lives here

- **`Plugin`** — the interface every plugin implements: `manifest` + 4 lifecycle callbacks
  (`onLoad`/`onEnable`/`onDisable`/`onUnload`) + `health()`.
- **`PluginManifest`** / **`PluginVersion`** / **`PluginPermission`** — what a plugin declares
  about itself before any of its code runs.
- **`PluginContext`** — the *only* handle a plugin receives into the host: `tools`,
  `capabilities`, `intents`, `storage`, `settings`, `events`, `log`.
- **`PluginToolRegistrar`** / **`PluginCapabilityRegistrar`** / **`PluginIntentRegistrar`** —
  "Dynamic Tool/Capability/Intent Registration," the plugin-facing half.
- **`PluginStorage`** / **`PluginSettings`** — scoped, per-plugin persistence and
  user-configurable values.
- **`PluginEvent`** / **`PluginEventBus`** — the plugin-facing event vocabulary, independent of
  `core-events.AuraEvent`.
- **`PluginResult`** / **`PluginError`** — this module's own result type, mirroring
  `core-ai.AuraResult`'s shape without depending on it.

## Why zero dependencies

A `Plugin` implementation compiled only against this module has no import path to any internal
AURA type — `core-tools`, `core-memory`, `core-agents`, none of it. That's not enforced by
convention or a lint rule; it's enforced by Gradle module boundaries, which is what makes it real.
Every type here that mirrors a `core-*` shape (`PluginResult` vs. `AuraResult`,
`PluginParameterType` vs. `ParameterType`) is a deliberate, independent duplicate rather than a
shared import, for exactly this reason.

## Status

Fully real — every interface here has a genuine, working implementation one layer up in
`core-plugin`/`plugin-runtime`. Nothing in this module is a placeholder.
