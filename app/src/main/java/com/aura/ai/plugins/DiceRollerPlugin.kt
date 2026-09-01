package com.aura.ai.plugins

import com.aura.ai.plugin.api.Plugin
import com.aura.ai.plugin.api.PluginContext
import com.aura.ai.plugin.api.PluginError
import com.aura.ai.plugin.api.PluginHealthReport
import com.aura.ai.plugin.api.PluginHealthStatus
import com.aura.ai.plugin.api.PluginIntentDescriptor
import com.aura.ai.plugin.api.PluginManifest
import com.aura.ai.plugin.api.PluginParameterSchema
import com.aura.ai.plugin.api.PluginParameterType
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginResult
import com.aura.ai.plugin.api.PluginSettingDescriptor
import com.aura.ai.plugin.api.PluginSettingType
import com.aura.ai.plugin.api.PluginToolDescriptor
import com.aura.ai.plugin.api.PluginToolResult
import com.aura.ai.plugin.api.PluginVersion
import javax.inject.Inject
import kotlin.random.Random

/**
 * A real, working example plugin — proves the SDK end to end rather than just describing it.
 * Registers one tool ("roll_dice"), one capability ("DiceRolling"), one intent trigger, one
 * setting (the default die size), and tracks a running total in its own scoped storage. Every
 * one of the brief's "Implement" items this plugin can exercise, it does.
 */
class DiceRollerPlugin
    @Inject
    constructor() : Plugin {
        private companion object {
            const val DEFAULT_SIDES_KEY = "defaultSides"
            const val TOTAL_ROLLS_KEY = "totalRolls"
        }

        override val manifest =
            PluginManifest(
                id = "com.aura.example.diceroller",
                name = "Dice Roller",
                description = "Rolls virtual dice on request.",
                version = PluginVersion(1, 0, 0),
                author = "AURA",
                minHostVersion = PluginVersion(1, 0, 0),
                requiredPermissions =
                    setOf(
                        PluginPermission.RegisterTools,
                        PluginPermission.RegisterCapabilities,
                        PluginPermission.RegisterIntents,
                        PluginPermission.AccessStorage,
                        PluginPermission.AccessSettings,
                    ),
            )

        override suspend fun onLoad(context: PluginContext): PluginResult<Unit> {
            context.settings.declare(
                listOf(
                    PluginSettingDescriptor(
                        key = DEFAULT_SIDES_KEY,
                        label = "Default die size",
                        type = PluginSettingType.Number,
                        defaultValue = "6",
                        description = "How many sides a roll uses when none is specified.",
                    ),
                ),
            )
            return PluginResult.Success(Unit)
        }

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
                    parameters =
                        mapOf(
                            "sides" to PluginParameterSchema(PluginParameterType.Number, "Number of sides on the die"),
                        ),
                    handler = { arguments -> roll(context, arguments) },
                ),
            )
        }

        private suspend fun roll(
            context: PluginContext,
            arguments: Map<String, String>,
        ): PluginResult<PluginToolResult> {
            val sides =
                arguments["sides"]?.toIntOrNull()
                    ?: context.settings.get(DEFAULT_SIDES_KEY)?.toIntOrNull()
                    ?: 6
            if (sides < 2) {
                return PluginResult.Failure(PluginError.InvalidRequest("A die needs at least 2 sides."))
            }

            val result = Random.nextInt(1, sides + 1)
            val total = ((context.storage.get(TOTAL_ROLLS_KEY)?.toIntOrNull() ?: 0) + 1)
            context.storage.put(TOTAL_ROLLS_KEY, total.toString())
            context.log("Rolled a d$sides -> $result (roll #$total)")

            return PluginResult.Success(
                PluginToolResult(
                    summary = "Rolled a $result on a d$sides.",
                    data = mapOf("result" to result.toString(), "sides" to sides.toString(), "totalRolls" to total.toString()),
                ),
            )
        }

        override suspend fun onDisable(context: PluginContext): PluginResult<Unit> = PluginResult.Success(Unit)

        override suspend fun onUnload(context: PluginContext): PluginResult<Unit> = PluginResult.Success(Unit)

        override suspend fun health(): PluginHealthReport =
            PluginHealthReport(PluginHealthStatus.Healthy, "Ready — no external dependency.")
    }
