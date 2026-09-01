package com.aura.ai.plugin.api

/**
 * What a plugin declares about one intent it wants recognized. [triggerPhrases] follow the same
 * plain substring-match convention `core-intent.KeywordIntentRecognizer`'s own trigger lists use.
 * [impliedToolName], if set, is the plugin's own registered tool (see [PluginToolRegistrar]) that
 * satisfies this intent — the same `SubGoal.impliedToolName`/`PlanStep.toolName` convention used
 * everywhere else in this codebase for "this is the tool that handles this."
 */
data class PluginIntentDescriptor(
    val id: String,
    val description: String,
    val triggerPhrases: List<String>,
    val impliedToolName: String? = null,
)

/**
 * "Dynamic Intent Registration." `core-intent.IntentType` is a fixed, closed enum — a plugin
 * can't add a new case to it any more than it could add a new `Capability`. Instead, a
 * plugin-registered intent is recognized *alongside* the built-in ones by
 * `core-plugin.PluginAwareIntentRecognizer`, which reports a match as
 * `IntentType.Automation` carrying the plugin's own [PluginIntentDescriptor.id] in
 * `RecognizedIntent.slots` — see `docs/PLUGIN_SDK.md` for exactly how that mapping works and why
 * `Automation` is the honest, closest-fit bucket for "a plugin, not a built-in capability,
 * handles this."
 */
interface PluginIntentRegistrar {
    fun register(descriptor: PluginIntentDescriptor): PluginResult<Unit>

    fun unregister(id: String): PluginResult<Unit>
}
