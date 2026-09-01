package com.aura.ai.core.planner

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ToolDescriptor
import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.intent.RecognizedIntent
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The default [Planner]. Most intents resolve to a single tool call; a small set of "creation"
 * goals (assignment/report/essay/document/paper — matching the brief's own worked example,
 * "Create my assignment") resolve to the full research → generate → export → save → notify
 * pipeline. [availableTools] is accepted for interface forward-compatibility (a smarter planner
 * would ground itself in what's actually registered) but isn't consulted here — a fixed template
 * doesn't need to look anything up to know what it's going to do.
 */
@Singleton
class TemplatePlanner
    @Inject
    constructor() : Planner {
        private val documentKeywords = listOf("assignment", "report", "essay", "document", "paper", "thesis")

        override suspend fun plan(
            goal: String,
            intent: RecognizedIntent,
            availableTools: List<ToolDescriptor>,
        ): AuraResult<ExecutionPlan> {
            val steps =
                if (isDocumentCreationGoal(goal, intent)) {
                    documentPipeline(goal)
                } else {
                    singleStepPlan(intent, goal)
                }
            return AuraResult.Success(
                ExecutionPlan(
                    id = UUID.randomUUID().toString(),
                    goal = goal,
                    steps = steps,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
        }

        private fun isDocumentCreationGoal(
            goal: String,
            intent: RecognizedIntent,
        ): Boolean {
            val lower = goal.lowercase()
            val mentionsDocument = documentKeywords.any { lower.contains(it) }
            val plausibleIntent = intent.type in setOf(IntentType.Research, IntentType.Coding, IntentType.Unknown)
            return mentionsDocument && plausibleIntent
        }

        /** Matches the brief's own example exactly: Research -> Generate -> Export PDF -> Save -> Notify. */
        private fun documentPipeline(goal: String): List<PlanStep> {
            val fileName =
                goal
                    .trim()
                    .lowercase()
                    .replace(Regex("[^a-z0-9]+"), "_")
                    .trim('_')
                    .ifEmpty { "document" } + ".pdf"

            return listOf(
                PlanStep(
                    id = "1",
                    description = "Research the topic",
                    toolName = "web_search",
                    parameters = mapOf("query" to goal),
                ),
                PlanStep(
                    id = "2",
                    description = "Generate the content",
                    toolName = null, // no local tool can write the document body — this genuinely needs a connected AI provider
                    dependsOn = listOf("1"),
                ),
                PlanStep(
                    id = "3",
                    description = "Export as PDF",
                    toolName = "export_pdf",
                    parameters = mapOf("fileName" to fileName, "content" to "\${step:2.output}"),
                    dependsOn = listOf("2"),
                ),
                PlanStep(
                    id = "4",
                    description = "Save the file",
                    toolName = "save_file",
                    parameters = mapOf("sourcePath" to "\${step:3.output}", "fileName" to fileName),
                    dependsOn = listOf("3"),
                ),
                PlanStep(
                    id = "5",
                    description = "Notify the user",
                    toolName = "notify",
                    parameters = mapOf("title" to "Your document is ready", "message" to "\${step:4.output}"),
                    dependsOn = listOf("4"),
                ),
            )
        }

        private fun singleStepPlan(
            intent: RecognizedIntent,
            goal: String,
        ): List<PlanStep> {
            val slots = intent.slots
            val step =
                when (intent.type) {
                    IntentType.OpenApp ->
                        PlanStep(
                            id = "1",
                            description = "Open ${slots["appName"] ?: "the app"}",
                            toolName = "open_app",
                            parameters = mapOf("appName" to (slots["appName"] ?: goal)),
                        )
                    IntentType.Reminder ->
                        PlanStep(
                            id = "1",
                            description = "Set a reminder",
                            toolName = "set_reminder",
                            parameters = mapOf("message" to (slots["reminderText"] ?: goal)),
                        )
                    IntentType.Calendar ->
                        PlanStep(
                            id = "1",
                            description = "Create a calendar event",
                            toolName = "create_calendar_event",
                            parameters = mapOf("title" to (slots["eventText"] ?: goal)),
                        )
                    IntentType.Navigation ->
                        PlanStep(
                            id = "1",
                            description = "Start navigation",
                            toolName = "navigate",
                            parameters = mapOf("destination" to (slots["destination"] ?: goal)),
                        )
                    IntentType.Email ->
                        PlanStep(
                            id = "1",
                            description = "Compose an email",
                            toolName = "compose_email",
                            parameters = mapOf("recipient" to (slots["recipient"] ?: ""), "subject" to goal),
                        )
                    IntentType.Shopping ->
                        PlanStep(
                            id = "1",
                            description = "Search for the item",
                            toolName = "shopping_search",
                            parameters = mapOf("query" to (slots["item"] ?: goal)),
                        )
                    IntentType.Research ->
                        PlanStep(
                            id = "1",
                            description = "Research the topic",
                            toolName = "web_search",
                            parameters = mapOf("query" to (slots["topic"] ?: goal)),
                        )
                    IntentType.Automation ->
                        PlanStep(
                            id = "1",
                            description = "Run the automation",
                            toolName = "app_automation",
                            parameters = mapOf("request" to goal),
                        )
                    IntentType.Coding ->
                        PlanStep(
                            id = "1",
                            description = "Assist with code — requires a connected AI provider",
                            toolName = null,
                        )
                    IntentType.Conversation, IntentType.Unknown ->
                        PlanStep(
                            id = "1",
                            description = "Respond conversationally",
                            toolName = null,
                        )
                }
            return listOf(step)
        }
    }
