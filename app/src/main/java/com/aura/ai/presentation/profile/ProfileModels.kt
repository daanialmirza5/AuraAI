package com.aura.ai.presentation.profile

import com.aura.ai.core.providers.ProviderId

enum class ProfileSub(
    val key: String,
    val label: String,
) {
    Profile("profile", "Profile"),
    Settings("settings", "Settings"),
    Personalization("personalization", "Personalize"),
    MemoryManager("memorymanager", "Memory Mgr"),
    Analytics("analytics", "Analytics"),
    DeviceHealth("devicehealth", "Device Health"),
    Battery("battery", "Battery"),
    Storage("storage", "Storage"),
    About("about", "About"),
    ;

    companion object {
        fun fromKey(key: String?): ProfileSub = entries.firstOrNull { it.key == key } ?: Profile
    }
}

data class ProfileStat(
    val value: String,
    val label: String,
)

data class SettingsRow(
    val label: String,
    val value: String,
)

data class SettingsGroup(
    val label: String,
    val rows: List<SettingsRow>,
)

/** One row of the real (not sample) "AI Providers" settings section — [connected] reflects
 *  [com.aura.ai.core.ai.AIProvider.isAvailable] at the time the row was last refreshed, not a
 *  live subscription (there is nothing to subscribe to: credentials are a plain suspend get/set,
 *  not a reactive store — see `ProfileViewModel.refreshTick`). */
data class ProviderSettingsRow(
    val id: ProviderId,
    val displayName: String,
    val isLocal: Boolean,
    val connected: Boolean,
    val isActive: Boolean,
    val baseUrl: String? = null,
)

data class MemoryAction(
    val label: String,
    val description: String,
    val action: String,
    val danger: Boolean = false,
)

data class WeeklyBar(
    val day: String,
    val fraction: Float,
)

data class HealthRow(
    val label: String,
    val value: String,
    val positive: Boolean,
)

data class BatteryAppUsage(
    val name: String,
    val percent: Int,
)

data class StorageCategory(
    val name: String,
    val size: String,
    val fraction: Float,
)

object ProfileSampleContent {
    val stats = listOf(ProfileStat("312", "Sessions"), ProfileStat("48", "Memories"), ProfileStat("5", "Devices"))

    val profileLinks = listOf("Linked Accounts", "Privacy & Data", "Subscription — AURA Pro", "Export My Data", "Sign Out")

    val settingsGroups =
        listOf(
            SettingsGroup(
                "AI Behavior",
                listOf(
                    SettingsRow("Personality", "Blended"),
                    SettingsRow("Proactivity", "High"),
                    SettingsRow("Response length", "Concise"),
                ),
            ),
            SettingsGroup(
                "Privacy",
                listOf(
                    SettingsRow("Voice recording retention", "30 days"),
                    SettingsRow("Location precision", "Approximate"),
                ),
            ),
            SettingsGroup(
                "System",
                listOf(
                    SettingsRow("App version", "2.4.1"),
                    SettingsRow("Neural core", "On-device + Cloud"),
                ),
            ),
        )

    val voices = listOf("JARVIS", "FRIDAY", "Neutral")

    val memoryActions =
        listOf(
            MemoryAction("Review recent memories", "6 new entries this week", "REVIEW"),
            MemoryAction("Export memory graph", "JSON, portable format", "EXPORT"),
            MemoryAction("Clear all memory", "Irreversible — starts AURA fresh", "CLEAR", danger = true),
        )

    val weekBars =
        listOf(
            WeeklyBar("M", 0.4f),
            WeeklyBar("T", 0.7f),
            WeeklyBar("W", 0.55f),
            WeeklyBar("T", 0.9f),
            WeeklyBar("F", 0.65f),
            WeeklyBar("S", 0.3f),
            WeeklyBar("S", 0.2f),
        )

    val healthRows =
        listOf(
            HealthRow("CPU Temperature", "38°C", true),
            HealthRow("RAM Usage", "3.1 / 8 GB", false),
            HealthRow("Background Processes", "12 active", false),
            HealthRow("Security Scan", "Clean · 2h ago", true),
        )

    val batteryApps =
        listOf(
            BatteryAppUsage("AURA Core", 34),
            BatteryAppUsage("Camera (Security)", 18),
            BatteryAppUsage("Maps & Location", 12),
            BatteryAppUsage("Messages", 9),
        )

    val storageCategories =
        listOf(
            StorageCategory("Photos & Media", "24.1 GB", 0.34f),
            StorageCategory("AURA Memory & Cache", "2.4 GB", 0.22f),
            StorageCategory("Apps", "16.8 GB", 0.18f),
            StorageCategory("Documents", "9.2 GB", 0.10f),
            StorageCategory("Free space", "57.0 GB", 0.26f),
        )
}
