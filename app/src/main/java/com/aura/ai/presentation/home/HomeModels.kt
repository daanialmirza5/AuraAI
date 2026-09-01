package com.aura.ai.presentation.home

enum class HomeSub { Dashboard, Notifications }

enum class TimelineDot { Accent, Neutral }

data class TimelineItem(
    val time: String,
    val title: String,
    val subtitle: String,
    val dot: TimelineDot,
)

data class NotificationSummaryItem(
    val count: Int,
    val app: String,
    val summary: String,
)

/** Illustrative content the source ships statically (no backing notification listener
 *  or LLM summarizer was in scope) — kept as plain sample data, not fake persistence. */
object HomeSampleContent {
    val timeline =
        listOf(
            TimelineItem("08:00", "Morning Boot Sequence ran", "Lights, weather, and calendar briefed", TimelineDot.Accent),
            TimelineItem("10:30", "Design Review", "Focus Shield suggested — declined", TimelineDot.Neutral),
            TimelineItem("13:15", "Deep Work Block", "2h · AURA muted non-urgent pings", TimelineDot.Neutral),
            TimelineItem("18:00", "Away Mode primed", "Leaving geofence detected at 17:52", TimelineDot.Accent),
        )

    val notifications =
        listOf(
            NotificationSummaryItem(6, "Messages", "Team thread — ship date moved to Friday, 2 need replies"),
            NotificationSummaryItem(3, "Calendar", "Two meetings overlap at 2pm — AURA proposes a reschedule"),
            NotificationSummaryItem(4, "Smart Home", "Front camera flagged motion 3x, all identified as courier"),
            NotificationSummaryItem(1, "Banking", "Unusual charge flagged — confirm or dispute"),
        )
}
