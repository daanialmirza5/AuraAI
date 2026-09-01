package com.aura.ai.domain.model

import java.util.UUID

/**
 * The five named examples from the Version 1.0 brief, built strictly from tools that actually
 * exist (`notify`, `media_control`, `shopping_search` — see `docs/ANDROID_AUTOMATION.md`) rather
 * than inventing capabilities (a real weather/calendar-read/AI-summary tool) this codebase
 * doesn't have yet. Each preset is honestly modest today; richer content is a natural extension
 * once a summary-generating tool exists, tracked in `docs/WORKFLOW_ENGINE.md` §4, not silently
 * pretended into being here.
 */
object WorkflowPresets {
    fun all(): List<Workflow> =
        listOf(
            Workflow(
                id = UUID.randomUUID().toString(),
                name = "Morning Brief",
                description = "A gentle daily nudge to start the day.",
                trigger = WorkflowTrigger.Daily,
                triggerHour = 7,
                triggerMinute = 30,
                steps =
                    listOf(
                        WorkflowStep("notify", mapOf("title" to "Morning Brief", "message" to "Good morning! Here's your day ahead.")),
                    ),
            ),
            Workflow(
                id = UUID.randomUUID().toString(),
                name = "Study Routine",
                description = "Pauses playback and clears the runway for focused study time.",
                trigger = WorkflowTrigger.Manual,
                steps =
                    listOf(
                        WorkflowStep("media_control", mapOf("action" to "pause")),
                        WorkflowStep("notify", mapOf("title" to "Study time", "message" to "Focus mode — playback paused.")),
                    ),
            ),
            Workflow(
                id = UUID.randomUUID().toString(),
                name = "Shopping Routine",
                description = "Opens a shopping search for your usual list.",
                trigger = WorkflowTrigger.Manual,
                steps =
                    listOf(
                        WorkflowStep("shopping_search", mapOf("query" to "groceries")),
                    ),
            ),
            Workflow(
                id = UUID.randomUUID().toString(),
                name = "Daily Review",
                description = "An evening prompt to reflect on the day.",
                trigger = WorkflowTrigger.Daily,
                triggerHour = 21,
                triggerMinute = 0,
                steps =
                    listOf(
                        WorkflowStep(
                            "notify",
                            mapOf(
                                "title" to "Daily Review",
                                "message" to "How did today go? A quick reflection now saves a rushed one later.",
                            ),
                        ),
                    ),
            ),
            Workflow(
                id = UUID.randomUUID().toString(),
                name = "Weekly Planning",
                description = "A Sunday-evening prompt to plan the week ahead.",
                trigger = WorkflowTrigger.Weekly,
                triggerHour = 18,
                triggerMinute = 0,
                triggerDayOfWeek = 7,
                steps =
                    listOf(
                        WorkflowStep("notify", mapOf("title" to "Weekly Planning", "message" to "Time to plan your week ahead.")),
                    ),
            ),
        )
}
