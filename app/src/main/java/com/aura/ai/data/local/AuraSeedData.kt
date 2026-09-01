package com.aura.ai.data.local

import com.aura.ai.data.local.entity.AutomationEntity
import com.aura.ai.data.local.entity.ChatMessageEntity
import com.aura.ai.data.local.entity.DeviceEntity
import com.aura.ai.data.local.entity.MemoryEntity
import com.aura.ai.data.local.entity.TodoEntity

/** The exact default state AURA AI.dc.html's root component ships with on first boot. */
object AuraSeedData {
    val devices =
        listOf(
            DeviceEntity("lights", "Living Room Lights", "bulb", isOn = true, meta = "80% · Warm", sortOrder = 0),
            DeviceEntity("thermo", "Thermostat", "temp", isOn = true, meta = "21.5°C", sortOrder = 1),
            DeviceEntity("lock", "Front Door Lock", "lock", isOn = true, meta = "Locked", sortOrder = 2),
            DeviceEntity("cam", "Security Cameras", "cam", isOn = false, meta = "4 offline", sortOrder = 3),
            DeviceEntity("speaker", "Living Room Speaker", "speaker", isOn = true, meta = "Playing · Ambient", sortOrder = 4),
        )

    val automations =
        listOf(
            AutomationEntity(
                "morning",
                "Morning Boot Sequence",
                "Lights 40% · Weather brief · Calendar read",
                isOn = true,
                sortOrder = 0,
            ),
            AutomationEntity(
                "leave",
                "Away Mode",
                "Lock doors · Arm cameras · Lower thermostat",
                isOn = true,
                sortOrder = 1,
            ),
            AutomationEntity(
                "focus",
                "Focus Shield",
                "Silence notifications · Status: Do Not Disturb",
                isOn = false,
                sortOrder = 2,
            ),
            AutomationEntity(
                "night",
                "Night Protocol",
                "Dim lights · Lock check · Set alarm",
                isOn = true,
                sortOrder = 3,
            ),
        )

    fun initialChatMessage(nowMillis: Long) =
        ChatMessageEntity(
            sender = "ai",
            text = "I've reviewed your calendar for tomorrow — three conflicts resolved. Anything else, sir?",
            timestampMillis = nowMillis,
        )

    val memories =
        listOf(
            MemoryEntity(
                "sleep-briefing",
                "Prefers concise morning briefings",
                "Under 30 seconds, top 3 items only.",
                "Learned · 14 days ago",
                0,
            ),
            MemoryEntity(
                "workout",
                "Works out Tue/Thu 6:30am",
                "AURA avoids scheduling before 8am those days.",
                "Learned · 22 days ago",
                1,
            ),
            MemoryEntity(
                "halcyon",
                "Project “Halcyon” is high priority",
                "Related messages surfaced above routine mail.",
                "Pinned by you",
                2,
            ),
            MemoryEntity(
                "video-calls",
                "Dislikes video calls after 6pm",
                "AURA proposes async alternatives past that hour.",
                "Learned · 6 days ago",
                3,
            ),
            MemoryEntity(
                "geofence",
                "Home is geofenced at 40.71, -74.00",
                "Used for Away Mode & commute predictions.",
                "System · Location",
                4,
            ),
        )

    val todos =
        listOf(
            TodoEntity("halcyon-copy", "Approve Halcyon rollout copy", "Due today", done = false, priority = "HIGH", sortOrder = 0),
            TodoEntity("legal-nda", "Reply to legal re: NDA", "Due today", done = false, priority = "MEDIUM", sortOrder = 1),
            TodoEntity("flights", "Book flights — offsite", "Due tomorrow", done = true, priority = "DONE", sortOrder = 2),
            TodoEntity("memory-review", "Review AURA memory log", "Weekly", done = false, priority = "LOW", sortOrder = 3),
            TodoEntity("lock-firmware", "Renew smart lock firmware", "Due Fri", done = false, priority = "LOW", sortOrder = 4),
            TodoEntity("q3-brief", "Send Q3 brief to team", "Due Mon", done = true, priority = "DONE", sortOrder = 5),
        )
}
