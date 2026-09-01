package com.aura.ai.plugins

import com.aura.ai.plugin.api.Plugin
import com.aura.ai.plugin.api.PluginContext
import com.aura.ai.plugin.api.PluginError
import com.aura.ai.plugin.api.PluginHealthReport
import com.aura.ai.plugin.api.PluginHealthStatus
import com.aura.ai.plugin.api.PluginManifest
import com.aura.ai.plugin.api.PluginParameterSchema
import com.aura.ai.plugin.api.PluginParameterType
import com.aura.ai.plugin.api.PluginPermission
import com.aura.ai.plugin.api.PluginResult
import com.aura.ai.plugin.api.PluginToolDescriptor
import com.aura.ai.plugin.api.PluginToolResult
import com.aura.ai.plugin.api.PluginVersion
import javax.inject.Inject

/**
 * A second real example plugin — deliberately simpler than [DiceRollerPlugin] (no settings, no
 * intent trigger) to show the SDK doesn't force every plugin through every feature. Registers a
 * capability name — `"TextAnalysis"` — that doesn't match any of `core-capabilities.Capability`'s
 * 11 fixed values, demonstrating that plugin capabilities really are an open vocabulary (see
 * `com.aura.ai.plugin.api.PluginCapabilityRegistrar`'s own doc).
 */
class WordCounterPlugin
    @Inject
    constructor() : Plugin {
        private companion object {
            const val LAST_COUNT_KEY = "lastWordCount"
        }

        override val manifest =
            PluginManifest(
                id = "com.aura.example.wordcounter",
                name = "Word Counter",
                description = "Counts words and characters in a piece of text.",
                version = PluginVersion(1, 0, 0),
                author = "AURA",
                minHostVersion = PluginVersion(1, 0, 0),
                requiredPermissions =
                    setOf(
                        PluginPermission.RegisterTools,
                        PluginPermission.RegisterCapabilities,
                        PluginPermission.AccessStorage,
                    ),
            )

        override suspend fun onLoad(context: PluginContext): PluginResult<Unit> = PluginResult.Success(Unit)

        override suspend fun onEnable(context: PluginContext): PluginResult<Unit> {
            context.capabilities.register("TextAnalysis")
            return context.tools.register(
                PluginToolDescriptor(
                    name = "count_words",
                    description = "Counts words and characters in the given text.",
                    parameters =
                        mapOf(
                            "text" to PluginParameterSchema(PluginParameterType.String, "The text to analyze", required = true),
                        ),
                    handler = { arguments -> count(context, arguments) },
                ),
            )
        }

        private suspend fun count(
            context: PluginContext,
            arguments: Map<String, String>,
        ): PluginResult<PluginToolResult> {
            val text = arguments["text"]
            if (text.isNullOrBlank()) {
                return PluginResult.Failure(PluginError.InvalidRequest("text is required."))
            }

            val wordCount = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            val charCount = text.length
            context.storage.put(LAST_COUNT_KEY, wordCount.toString())

            return PluginResult.Success(
                PluginToolResult(
                    summary = "$wordCount word(s), $charCount character(s).",
                    data = mapOf("words" to wordCount.toString(), "characters" to charCount.toString()),
                ),
            )
        }

        override suspend fun onDisable(context: PluginContext): PluginResult<Unit> = PluginResult.Success(Unit)

        override suspend fun onUnload(context: PluginContext): PluginResult<Unit> = PluginResult.Success(Unit)

        override suspend fun health(): PluginHealthReport =
            PluginHealthReport(PluginHealthStatus.Healthy, "Ready — no external dependency.")
    }
