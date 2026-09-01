package com.aura.ai.presentation.workspace

enum class WorkspaceSub(
    val key: String,
    val label: String,
) {
    Timeline("timeline", "Timeline"),
    Planner("planner", "Planner"),
    Calendar("calendar", "Calendar"),
    Todo("todo", "To-Do"),
    Notes("notes", "Notes"),
    Reminders("reminders", "Reminders"),
    Files("files", "Files"),
    Docgen("docgen", "Docs"),
    Coding("coding", "Code"),
    Research("research", "Research"),
}

enum class DotTone { Accent, Neutral, Warning }

data class TimelineEntry(
    val time: String,
    val title: String,
    val subtitle: String,
    val dot: DotTone,
)

data class PlanBlock(
    val color: DotTone,
    val title: String,
    val time: String,
    val tag: String,
)

data class CalendarDay(
    val dow: String,
    val num: Int,
    val isSelected: Boolean,
)

data class CalendarEvent(
    val time: String,
    val title: String,
    val meta: String,
    val conflict: Boolean,
)

data class NoteItem(
    val title: String,
    val body: String,
)

data class ReminderItem(
    val title: String,
    val whenText: String,
)

data class StorageStat(
    val value: String,
    val label: String,
)

data class WorkspaceFile(
    val name: String,
    val meta: String,
    val ext: String,
    val tone: DotTone,
)

data class DraftDoc(
    val title: String,
    val meta: String,
)

data class ResearchSource(
    val index: Int,
    val title: String,
    val domain: String,
)

/** Illustrative dashboard content the source ships as static sample data (never wired
 *  to any handler in AuraWorkspace.dc.html) — kept as plain constants, not fake persistence. */
object WorkspaceSampleContent {
    val timeline =
        listOf(
            TimelineEntry("06:30", "Workout window", "Recovered from yesterday · suggested light cardio", DotTone.Accent),
            TimelineEntry("08:00", "Morning Boot Sequence", "Briefed weather, calendar, top 3 priorities", DotTone.Accent),
            TimelineEntry("09:30", "Deep Work: Halcyon spec", "90min focus block, DND engaged", DotTone.Neutral),
            TimelineEntry("11:15", "Standup", "15min · auto-joined, notes drafted", DotTone.Neutral),
            TimelineEntry("13:15", "Lunch + reset", "Suggested walk based on step count", DotTone.Neutral),
            TimelineEntry("14:00", "Conflict: Design Review vs 1:1", "AURA proposes moving 1:1 to 15:30", DotTone.Warning),
            TimelineEntry("18:00", "Away Mode primed", "Geofence exit predicted 17:52", DotTone.Accent),
        )

    val planBlocks =
        listOf(
            PlanBlock(DotTone.Accent, "Deep Work — Halcyon Spec", "09:00–10:30", "High energy"),
            PlanBlock(DotTone.Neutral, "Buffer", "10:30–10:45", "Recovery"),
            PlanBlock(DotTone.Accent, "Halcyon Review", "10:45–11:30", "Moved from 14:00"),
            PlanBlock(DotTone.Neutral, "Inbox & replies", "13:15–13:45", "Low energy"),
            PlanBlock(DotTone.Accent, "Deep Work — Research", "15:30–17:00", "High energy"),
        )

    val calendarDays =
        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            .mapIndexed { i, dow -> CalendarDay(dow, 21 + i, isSelected = i == 2) }

    val calendarEvents =
        listOf(
            CalendarEvent("09:00", "Design Review", "Halcyon · Room 4B", conflict = false),
            CalendarEvent("14:00", "1:1 with Priya", "Video call", conflict = true),
            CalendarEvent("14:00", "Sprint Planning", "Team Halcyon", conflict = true),
            CalendarEvent("17:30", "Dinner — Mika", "Personal", conflict = false),
        )

    val notes =
        listOf(
            NoteItem("Halcyon naming ideas", "Aurora, Meridian, Halcyon (kept)…"),
            NoteItem("Offsite packing", "Charger, badge, the good headphones"),
            NoteItem("Gift ideas — Mika", "Vinyl, the jacket she liked"),
            NoteItem("Reading queue", "Two papers AURA flagged on batteries"),
        )

    val reminders =
        listOf(
            ReminderItem("Take out recycling", "Tonight, 8:00 PM"),
            ReminderItem("Call mom", "Tomorrow, 12:00 PM"),
            ReminderItem("Renew passport", "In 3 weeks"),
            ReminderItem("Standing desk — stand up", "Every 45 min"),
            ReminderItem("Water the plants", "Sat, 9:00 AM"),
        )

    val storageStats = listOf(StorageStat("128 GB", "Total"), StorageStat("71 GB", "Used"), StorageStat("57 GB", "Free"))

    val files =
        listOf(
            WorkspaceFile("Halcyon_Brief_v3.pdf", "2.4 MB · Edited 2h ago", "PDF", DotTone.Warning),
            WorkspaceFile("Q3_Rollout_Deck.pptx", "18 MB · Yesterday", "PPT", DotTone.Warning),
            WorkspaceFile("aura_memory_export.json", "440 KB · 3 days ago", "JSON", DotTone.Accent),
            WorkspaceFile("Offsite_Photos", "212 items · Folder", "DIR", DotTone.Accent),
            WorkspaceFile("Voice_Note_0731.m4a", "1.2 MB · Last week", "AUD", DotTone.Neutral),
            WorkspaceFile("Design_System.fig", "80 MB · 2 weeks ago", "FIG", DotTone.Warning),
        )

    val docTypes = listOf("Brief", "Report", "Email", "Slides")

    val drafts =
        listOf(
            DraftDoc("Halcyon Q3 Project Brief", "Generated 12 min ago · 1 page"),
            DraftDoc("Reply to Legal — NDA terms", "Generated yesterday"),
            DraftDoc("Team Update — Sprint 14", "Generated 3 days ago"),
        )

    val researchSources =
        listOf(
            ResearchSource(1, "Thermal limits in Li-ion fast charge cycles", "ieee.org"),
            ResearchSource(2, "Adaptive charging curves — 2026 survey", "nature.com"),
            ResearchSource(3, "Battery health telemetry patterns", "arxiv.org"),
            ResearchSource(4, "Consumer device charging safety standards", "iec.ch"),
        )
}
