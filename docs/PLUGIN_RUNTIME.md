# AURA AI — Plugin Runtime

How a plugin actually gets from "compiled into the app" to "running, registered, and observable"
— `plugin-loader`, `plugin-runtime`, and the parts of `core-plugin` they drive. See
[PLUGIN_API.md](PLUGIN_API.md) for the types referenced throughout and
[PLUGIN_SECURITY.md](PLUGIN_SECURITY.md) for the isolation guarantees this machinery upholds.

---

## 1. Startup sequence

```mermaid
sequenceDiagram
    participant App as AuraApplication.onCreate
    participant Loader as DefaultPluginLoader
    participant Source as InMemoryPluginSource
    participant Checker as PluginCompatibilityChecker
    participant Policy as PluginPermissionPolicy
    participant Runtime as DefaultPluginRuntime
    participant Plugin as Plugin (e.g. DiceRollerPlugin)

    App->>Loader: loadAll()
    Loader->>Source: discover()
    Source-->>Loader: List~Plugin~ (from Set~Plugin~ multibinding)
    loop each discovered plugin
        Loader->>Checker: check(manifest)
        alt incompatible
            Checker-->>Loader: Incompatible(reason)
            Loader-->>App: PluginLoadResult(success=false)
        else compatible
            Loader->>Policy: decideGrants(manifest)
            Policy-->>Loader: Set~PluginPermission~
            Loader->>Runtime: load(plugin, grants)
            Runtime->>Plugin: onLoad(context)
            Runtime->>Loader: PluginResult
            Loader->>Runtime: enable(pluginId)
            Runtime->>Plugin: onEnable(context)
            Note over Plugin: registers tools/capabilities/intents here
            Runtime-->>Loader: PluginResult
            Loader-->>App: PluginLoadResult(success=true)
        end
    end
```

Every discovered plugin is checked, granted, loaded, and enabled — automatically, at the same
moment `core-tools.ToolRegistry` and `core-agents.AgentRegistry` are populated in
`AuraApplication.onCreate`. `DiceRollerPlugin` and `WordCounterPlugin` genuinely go through this
exact sequence every time the app starts.

---

## 2. Compatibility and versioning

`PluginCompatibilityChecker.check` runs before a single line of plugin code executes:

1. `manifest.id`/`manifest.name` must not be blank.
2. `HostSdk.VERSION >= manifest.minHostVersion`.
3. If `manifest.maxHostVersion` is set, `HostSdk.VERSION <= manifest.maxHostVersion`.

`HostSdk.VERSION` (`core-plugin.HostSdk`) is the running host's own SDK version — bumped when a
`plugin-api` change could break an existing plugin's assumptions. A plugin with no `maxHostVersion`
is implicitly compatible with every future host version, which is the honest default: most
`plugin-api` changes are additive and don't break existing plugins, so requiring every plugin to
guess an upper bound would be needless friction.

---

## 3. `PluginRuntime` — the lifecycle engine

```mermaid
flowchart TD
    A[load] -->|onLoad succeeds| B[state = Loaded]
    A -->|onLoad fails| F[state = Failed<br/>PluginFailedEvent]
    B --> C[enable]
    C -->|onEnable succeeds| D[state = Enabled]
    C -->|onEnable fails| F
    D --> E[disable]
    E --> G["onDisable called<br/>THEN unconditional cleanup:<br/>tools.unregisterAll<br/>capabilities.unregisterAll<br/>intents.unregisterAll"]
    G --> H[state = Disabled]
    H --> C
    H --> I[unload]
    D --> I
    I --> J["onUnload called<br/>+ same unconditional cleanup<br/>+ context discarded"]
    J --> K[state = Unloaded]
```

The "unconditional cleanup" step is deliberate defensive design: `disable`/`unload` never trust a
plugin's own `onDisable`/`onUnload` to have actually cleaned up after itself. Every tool name the
plugin ever registered (tracked by `DefaultPluginToolRegistrar`, since `core-tools.ToolRegistry`
itself has no concept of "which plugin owns this tool"), every capability, every intent — all
unregistered by the host, every time, regardless of what the plugin's callback did or whether it
even succeeded. A misbehaving plugin can fail to clean up; it can't leave an orphaned registration
behind.

One `DefaultPluginContext` is created per plugin at `load` time and retained across
`enable`/`disable` cycles — a plugin's own storage and settings, and its tool registrar's
bookkeeping, survive being disabled and re-enabled.

---

## 4. Dynamic Intent Registration, in practice

```mermaid
sequenceDiagram
    participant User
    participant Recognizer as PluginAwareIntentRecognizer
    participant Keyword as KeywordIntentRecognizer
    participant Registry as PluginIntentRegistry

    User->>Recognizer: recognize("roll the dice")
    Recognizer->>Keyword: recognize("roll the dice")
    Keyword-->>Recognizer: RecognizedIntent(Conversation, confidence=0.3)
    Note over Recognizer: Built-in recognizer found nothing real —<br/>only its own honest fallback
    Recognizer->>Registry: all()
    Registry-->>Recognizer: [(diceroller, "roll a die"/"roll dice"/...)]
    Recognizer->>Recognizer: "roll the dice" contains "roll dice"? no.<br/>contains "roll a die"? no.<br/>(trigger phrases must appear verbatim)
    Note over Recognizer: If a trigger matched:<br/>RecognizedIntent(Automation,<br/>slots={pluginId, pluginIntentId, pluginToolName})
```

`PluginAwareIntentRecognizer` decorates `KeywordIntentRecognizer` rather than replacing it — the
built-in recognizer always gets first attempt, and a plugin's trigger phrases are only ever
consulted once the built-in one has genuinely found nothing (its own `Conversation`, confidence
≤ 0.3 fallback). This ordering is what makes "dynamic" safe: a plugin can add new recognized
phrases, but can never shadow one the host already understands.

---

## 5. Why compiled-in plugins, not dynamic code loading

`plugin-loader`'s only `PluginSource` this phase, `InMemoryPluginSource`, discovers plugins from a
Hilt `Set<Plugin>` multibinding — the exact mechanism `core-tools`/`core-agents` already use for
`Tool`/`Agent`. Every plugin known to this host got there by being compiled directly into the app
binary.

This is a deliberate scope boundary, not an oversight. Genuine dynamic code loading — parsing a
manifest and instantiating a class from a `.dex`/`.jar` an untrusted third party provided at
runtime — is a materially different, much harder security problem: it means executing
arbitrary code inside the app's own process, which Android provides no built-in sandboxing for
short of a separate process plus a real permission-brokered IPC boundary. Building that safely is
a substantial engineering and security-review effort in its own right, and doing it *partially* —
loading external code without the isolation to back it up — would be worse than not building it
at all: it would look secure without being secure.

What *is* real: the [`PluginSource`](PLUGIN_API.md) interface is the seam a future dynamic-loading
implementation plugs into, and every piece of machinery downstream of "here's a `Plugin`
instance" — compatibility checking, permission grants, sandboxed context, lifecycle, dynamic
registration, events, storage, settings, health, updates — is already real and doesn't care where
the `Plugin` instance came from.

---

## 6. Plugin Events — the bridge

```mermaid
sequenceDiagram
    participant Runtime as DefaultPluginRuntime
    participant PluginBus as core-plugin.DefaultPluginEventBus
    participant HostBus as core-events.EventBus

    Note over Runtime: on every lifecycle transition
    Runtime->>PluginBus: publishLifecycleEvent(PluginEvent.Loaded(id))
    Runtime->>HostBus: publish(PluginLoadedEvent(id))
```

Two independent event vocabularies, bridged in exactly one place. `plugin-api.PluginEvent` is
what a plugin itself can observe (via `PluginContext.events`) — it never depends on `core-events`,
same isolation rule as everything else in that module. `core-events.AuraEvent` gained 6 new
plugin-lifecycle cases this phase (`PluginLoadedEvent`, `PluginEnabledEvent`, `PluginDisabledEvent`,
`PluginUnloadedEvent`, `PluginFailedEvent`, `PluginHealthChangedEvent`) — a purely additive change
to the sealed hierarchy Phase 6 established, verified safe by confirming no exhaustive `when`
anywhere in the codebase pattern-matches over every `AuraEvent` subtype. `DefaultPluginRuntime`
publishes both forms on every transition, so a plugin observing its own lifecycle and the rest of
the host observing *all* plugin activity never have to know about each other's event system.

---

## 7. Plugin Updates

```mermaid
flowchart TD
    A["update(newPlugin, grants)"] --> B{Compatible with host?}
    B -- no --> R1[Failure: incompatible]
    B -- yes --> C{Existing version installed?}
    C -- no --> G[load + enable new plugin]
    C -- yes --> D{New version > installed version?}
    D -- no --> R2[Failure: not newer]
    D -- yes --> E{Existing was Enabled?}
    E -- yes --> F1[disable existing]
    F1 --> F2[unload existing]
    E -- no --> F2
    F2 --> G
    G --> H{Existing was Enabled?}
    H -- yes --> I[enable new instance]
    H -- no --> J[leave Loaded, not enabled]
```

`PluginUpdateManager` never silently downgrades or breaks compatibility — both are checked before
anything about the existing installation is touched. If the outgoing version was actively enabled,
the net effect from a caller's perspective is "same plugin id, newer code, briefly interrupted for
the swap" — not a gap where the plugin appears to vanish.
