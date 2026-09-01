# AURA AI — Plugin Guide

How to write an AURA plugin, using the real, working `DiceRollerPlugin` as the worked example
throughout. See [PLUGIN_API.md](PLUGIN_API.md) for the full type reference and
[PLUGIN_SECURITY.md](PLUGIN_SECURITY.md) for what a plugin can and can't do.

---

## 1. Implement `Plugin`

```kotlin
class DiceRollerPlugin @Inject constructor() : Plugin {
    override val manifest = PluginManifest(
        id = "com.aura.example.diceroller",
        name = "Dice Roller",
        description = "Rolls virtual dice on request.",
        version = PluginVersion(1, 0, 0),
        author = "AURA",
        minHostVersion = PluginVersion(1, 0, 0),
        requiredPermissions = setOf(
            PluginPermission.RegisterTools,
            PluginPermission.RegisterCapabilities,
            PluginPermission.RegisterIntents,
            PluginPermission.AccessStorage,
            PluginPermission.AccessSettings,
        ),
    )
    // onLoad / onEnable / onDisable / onUnload / health below
}
```

`id` should be reverse-DNS-style and globally unique — it's the key every registry
(`PluginRegistry`, `PluginStorageHost`, `PluginCapabilityRegistry`) uses. Declare every permission
your plugin will ever need in `requiredPermissions` up front — see
[PLUGIN_SECURITY.md §3](PLUGIN_SECURITY.md#3-permission-enforcement) for why a call using an
undeclared permission fails rather than silently doing nothing.

---

## 2. Declare settings in `onLoad`

```kotlin
override suspend fun onLoad(context: PluginContext): PluginResult<Unit> {
    context.settings.declare(
        listOf(
            PluginSettingDescriptor(
                key = "defaultSides",
                label = "Default die size",
                type = PluginSettingType.Number,
                defaultValue = "6",
                description = "How many sides a roll uses when none is specified.",
            ),
        ),
    )
    return PluginResult.Success(Unit)
}
```

`onLoad` runs once, before anything is registered — the right place for setup that doesn't yet
need to be "live." `declare` doesn't set a value; it registers the *schema* so
`context.settings.get("defaultSides")` has a default to fall back to before any user ever changes it.

---

## 3. Register a tool, a capability, and an intent in `onEnable`

```kotlin
override suspend fun onEnable(context: PluginContext): PluginResult<Unit> {
    context.capabilities.register("DiceRolling")

    context.intents.register(
        PluginIntentDescriptor(
            id = "roll_dice",
            description = "Roll a die",
            triggerPhrases = listOf("roll a die", "roll the dice", "roll dice"),
            impliedToolName = "roll_dice",
        ),
    )

    return context.tools.register(
        PluginToolDescriptor(
            name = "roll_dice",
            description = "Rolls a die and returns the result.",
            parameters = mapOf(
                "sides" to PluginParameterSchema(PluginParameterType.Number, "Number of sides on the die"),
            ),
            handler = { arguments -> roll(context, arguments) },
        ),
    )
}
```

Returning the *tool* registration's `PluginResult` from `onEnable` means a permission failure on
the most important registration is what actually surfaces as this plugin's enable failure —
reasonable for a plugin whose primary purpose is that one tool. A plugin offering several
equally-important registrations should check each result explicitly instead.

---

## 4. Write the tool's handler — storage, settings, and honest failure

```kotlin
private suspend fun roll(context: PluginContext, arguments: Map<String, String>): PluginResult<PluginToolResult> {
    val sides = arguments["sides"]?.toIntOrNull()
        ?: context.settings.get("defaultSides")?.toIntOrNull()
        ?: 6
    if (sides < 2) {
        return PluginResult.Failure(PluginError.InvalidRequest("A die needs at least 2 sides."))
    }

    val result = Random.nextInt(1, sides + 1)
    val total = (context.storage.get("totalRolls")?.toIntOrNull() ?: 0) + 1
    context.storage.put("totalRolls", total.toString())
    context.log("Rolled a d$sides -> $result (roll #$total)")

    return PluginResult.Success(
        PluginToolResult(
            summary = "Rolled a $result on a d$sides.",
            data = mapOf("result" to result.toString(), "sides" to sides.toString(), "totalRolls" to total.toString()),
        ),
    )
}
```

The handler reads an explicit argument first, falls back to the declared setting, then a hardcoded
default — a normal, three-tier "most specific wins" pattern. `context.storage` persists a running
count across calls; `context.log` is the only logging a plugin gets (see
[PLUGIN_API.md §3](PLUGIN_API.md#3-plugincontext--the-one-handle-into-the-host)), automatically
tagged with the plugin's own id.

---

## 5. `onDisable`, `onUnload`, and `health`

```kotlin
override suspend fun onDisable(context: PluginContext): PluginResult<Unit> = PluginResult.Success(Unit)
override suspend fun onUnload(context: PluginContext): PluginResult<Unit> = PluginResult.Success(Unit)

override suspend fun health(): PluginHealthReport =
    PluginHealthReport(PluginHealthStatus.Healthy, "Ready — no external dependency.")
```

`DiceRollerPlugin` doesn't need to manually unregister its tool/capability/intent in `onDisable` —
`plugin-runtime` does that unconditionally regardless of what these callbacks do (see
[PLUGIN_RUNTIME.md §3](PLUGIN_RUNTIME.md#3-pluginruntime--the-lifecycle-engine)). `onDisable`/`onUnload`
are for releasing anything the plugin *itself* holds — a network connection, a background job — of
which this plugin has none. `health()` can be as simple as "always healthy" for a plugin with no
external dependency to check; a plugin wrapping a flaky resource should report `Degraded`/`Unavailable`
honestly when that resource isn't working, the same way `core-agents.CodingAgent.health()` does.

---

## 6. Register the plugin

```kotlin
// app/src/main/java/com/aura/ai/ai/di/AiPluginModule.kt
@Binds
@IntoSet
abstract fun bindDiceRollerPlugin(impl: DiceRollerPlugin): Plugin
```

One `@Binds @IntoSet` line — the same multibinding pattern `AiToolsModule`/`AiAgentsModule` use
for `Tool`/`Agent`. `AuraApplication.onCreate` already calls `pluginLoader.loadAll()`
unconditionally; nothing else needs to change for a newly-registered plugin to load, get checked
for compatibility, receive its granted permissions, and enable itself at the next app start.

---

## 7. A second, simpler example

`WordCounterPlugin` (also in `app/src/main/java/com/aura/ai/plugins/`) shows the SDK doesn't force
every feature on every plugin — no settings, no intent trigger, just one tool and one capability.
Worth reading alongside `DiceRollerPlugin` to see what's genuinely optional versus required.

## 8. Checklist

- [ ] `manifest.id` is unique and reverse-DNS-style.
- [ ] `requiredPermissions` lists everything the plugin will ever call through `PluginContext`.
- [ ] Settings are `declare`d in `onLoad`, not `onEnable`.
- [ ] Registrations happen in `onEnable`, nowhere else.
- [ ] Tool handlers validate their own arguments and return `PluginError.InvalidRequest` rather
      than throwing.
- [ ] `health()` returns quickly and never throws.
- [ ] The plugin is bound with `@Binds @IntoSet abstract fun ...: Plugin` in `AiPluginModule`.
