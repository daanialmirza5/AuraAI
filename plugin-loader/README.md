# plugin-loader

Turns "plugins compiled into this app" into "plugins actually running." Pure Kotlin/JVM; depends
on `plugin-api`, `core-plugin`, `plugin-runtime`, `core-ai`. See `docs/PLUGIN_RUNTIME.md §1` at
the project root for the full startup sequence diagram.

## What lives here

- **`PluginSource`** — where `Plugin` instances come from. `InMemoryPluginSource` is the only
  implementation this phase ships: a Hilt `Set<Plugin>` multibinding, the same discovery pattern
  `core-tools`/`core-agents` already use for `Tool`/`Agent`. The interface is the seam a future
  dynamic-loading source would implement without touching anything else.
- **`PluginLoader`** / **`DefaultPluginLoader`** — discovers every `PluginSource`, checks each
  candidate against `core-plugin.PluginCompatibilityChecker`, decides its permission grants via
  `core-plugin.PluginPermissionPolicy`, and hands it to `plugin-runtime.PluginRuntime` to load and
  enable.

## Called from

`com.aura.ai.AuraApplication.onCreate` — `pluginLoader.loadAll()`, at the same moment
`ToolRegistry`/`AgentRegistry` are populated.

## Status

Fully real for its declared scope: every plugin compiled into the app binary is discovered,
validated, and loaded automatically at startup, with a full, honest result
(`PluginLoadResult`) per plugin — success or the specific reason it wasn't.
