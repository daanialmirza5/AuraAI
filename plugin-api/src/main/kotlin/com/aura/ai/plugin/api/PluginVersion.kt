package com.aura.ai.plugin.api

/**
 * Plain semantic versioning (`major.minor.patch`) — used for both a plugin's own [PluginManifest.version]
 * and the host SDK version range a plugin declares compatibility with
 * (`PluginManifest.minHostVersion`/`maxHostVersion`). See `docs/PLUGIN_SDK.md` for how
 * `core-plugin`'s compatibility checker uses this.
 */
data class PluginVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<PluginVersion> {
    override fun compareTo(other: PluginVersion): Int =
        compareValuesBy(
            this,
            other,
            PluginVersion::major,
            PluginVersion::minor,
            PluginVersion::patch,
        )

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /** Returns `null` rather than throwing on a malformed string — a bad version string in a
         *  manifest is a validation failure `core-plugin`'s compatibility checker should report
         *  cleanly, not an exception that crashes plugin loading. */
        fun parse(text: String): PluginVersion? {
            val parts = text.trim().split(".")
            if (parts.size != 3) return null
            val (major, minor, patch) = parts.map { it.toIntOrNull() ?: return null }
            return PluginVersion(major, minor, patch)
        }
    }
}
