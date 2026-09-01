package com.aura.ai.presentation.automate

enum class AutomateSub(
    val key: String,
    val label: String,
) {
    Automation("automation", "Automations"),
    Workflows("workflows", "Workflows"),
    SmartHome("smarthome", "Smart Home"),
    Plugins("plugins", "Plugins"),
    Marketplace("marketplace", "Marketplace"),
    ;

    companion object {
        fun fromKey(key: String?): AutomateSub = entries.firstOrNull { it.key == key } ?: Automation
    }
}

enum class PluginStatus { Active, Limited, Connect }

data class Plugin(
    val name: String,
    val description: String,
    val status: PluginStatus,
)

data class MarketplaceSkill(
    val name: String,
    val description: String,
)

object AutomateSampleContent {
    val plugins =
        listOf(
            Plugin("Google Workspace", "Gmail, Docs, Sheets sync", PluginStatus.Active),
            Plugin("Spotify", "Music & ambient control", PluginStatus.Active),
            Plugin("GitHub", "Repo events for coding assistant", PluginStatus.Limited),
            Plugin("Notion", "Notes & knowledge base", PluginStatus.Connect),
            Plugin("Uber", "Ride booking from routines", PluginStatus.Connect),
        )

    val marketplace =
        listOf(
            MarketplaceSkill("Concierge Persona", "Warmer, hospitality-tuned tone"),
            MarketplaceSkill("Financial Analyst", "Portfolio-aware briefings"),
            MarketplaceSkill("Study Buddy", "Spaced repetition + quizzing"),
            MarketplaceSkill("Night Shift Mode", "Reversed sleep/wake automations"),
        )
}
