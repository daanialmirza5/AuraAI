package com.aura.ai.core.reasoning.decomposition

import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.intent.RecognizedIntent
import com.aura.ai.core.reasoning.model.SubGoal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The default [TaskDecomposer]. Mirrors `com.aura.ai.core.planner.TemplatePlanner`'s own
 * document-creation detection (same keyword list, same worked example — "create my assignment,"
 * "create my AIML assignment and save it as PDF") because the two are describing the same real
 * pipeline from two different layers: this module decomposes *what* needs to happen before any
 * tool is chosen, `TemplatePlanner` decides *how* once it plans. Deliberately not read from
 * `TemplatePlanner` itself — core-reasoning composing `Planner` at the end of a reasoning pass
 * (see `ReasoningEngine`) is the actual integration point; this class only needs to reach the
 * same conclusion about shape, not share code with it.
 *
 * A `SubGoal` with [SubGoal.impliedToolName] `null` needs a connected AI provider — the same
 * convention `com.aura.ai.core.planner.PlanStep` uses for a `null` `toolName`.
 */
@Singleton
class RuleBasedTaskDecomposer
    @Inject
    constructor() : TaskDecomposer {
        private val documentKeywords = listOf("assignment", "report", "essay", "document", "paper", "thesis")

        override fun decompose(
            goal: String,
            intent: RecognizedIntent,
        ): List<SubGoal> = if (isDocumentCreationGoal(goal, intent)) documentPipeline() else singleSubGoal(intent)

        private fun isDocumentCreationGoal(
            goal: String,
            intent: RecognizedIntent,
        ): Boolean {
            val lower = goal.lowercase()
            val mentionsDocument = documentKeywords.any { lower.contains(it) }
            val plausibleIntent = intent.type in setOf(IntentType.Research, IntentType.Coding, IntentType.Unknown)
            return mentionsDocument && plausibleIntent
        }

        private fun documentPipeline(): List<SubGoal> =
            listOf(
                SubGoal(id = "1", description = "Research the topic", priority = 1, impliedToolName = "web_search"),
                SubGoal(
                    id = "2",
                    description = "Generate the content",
                    priority = 2,
                    requiresAI = true,
                    dependsOn = listOf("1"),
                ),
                SubGoal(
                    id = "3",
                    description = "Export as PDF",
                    priority = 3,
                    impliedToolName = "export_pdf",
                    dependsOn = listOf("2"),
                ),
                SubGoal(
                    id = "4",
                    description = "Save the file",
                    priority = 4,
                    impliedToolName = "save_file",
                    dependsOn = listOf("3"),
                ),
                SubGoal(
                    id = "5",
                    description = "Notify the user",
                    priority = 5,
                    impliedToolName = "notify",
                    dependsOn = listOf("4"),
                ),
            )

        private fun singleSubGoal(intent: RecognizedIntent): List<SubGoal> {
            val (description, toolName) =
                when (intent.type) {
                    IntentType.OpenApp -> "Open the requested app" to "open_app"
                    IntentType.Reminder -> "Set a reminder" to "set_reminder"
                    IntentType.Calendar -> "Create a calendar event" to "create_calendar_event"
                    IntentType.Navigation -> "Start navigation" to "navigate"
                    IntentType.Email -> "Compose an email" to "compose_email"
                    IntentType.Shopping -> "Search for the item" to "shopping_search"
                    IntentType.Research -> "Research the topic" to "web_search"
                    IntentType.Automation -> "Run the automation" to "app_automation"
                    IntentType.Coding -> "Assist with code" to null
                    IntentType.Conversation, IntentType.Unknown -> "Respond conversationally" to null
                }
            return listOf(
                SubGoal(
                    id = "1",
                    description = description,
                    priority = 1,
                    requiresAI = toolName == null,
                    impliedToolName = toolName,
                ),
            )
        }
    }
