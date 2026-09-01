# plugin-runtime

The plugin lifecycle engine. Pure Kotlin/JVM; depends on `plugin-api`, `core-plugin`, `core-ai`,
`core-events`. See `docs/PLUGIN_RUNTIME.md` at the project root for the full sequence diagrams and
state machine; this file is the quick reference.

## What lives here

- **`PluginRuntime`** / **`DefaultPluginRuntime`** — runs the strict `onLoad → onEnable →
  (onDisable → onEnable)* → onUnload` sequence, retaining one `DefaultPluginContext` per plugin
  across enable/disable cycles, and unconditionally cleaning up every tool/capability/intent
  registration on `disable`/`unload` regardless of what the plugin's own callback did.
- **`PluginUpdateManager`** / **`DefaultPluginUpdateManager`** — "Plugin Updates": hot-swaps an
  installed plugin for a newer, compatible version, preserving whether it was enabled across the
  swap.

## The event bridge

Every lifecycle transition publishes twice: once as a `plugin-api.PluginEvent` (what a plugin
itself can observe, via `PluginContext.events`), and once as the corresponding
`core-events.AuraEvent` (`PluginLoadedEvent`/`PluginEnabledEvent`/etc. — 6 new cases added to
`AuraEvent`'s sealed hierarchy this phase, purely additively). This is the one place those two
independent event vocabularies meet.

## Status

Fully real. The one deliberate scope boundary: `plugin-loader`'s only plugin source this phase is
plugins compiled into the app binary — see `docs/PLUGIN_RUNTIME.md §5` for why genuine dynamic
code loading is a different, harder security problem this phase doesn't attempt, and what's
already in place (the `PluginSource` seam) for when it's built properly.
