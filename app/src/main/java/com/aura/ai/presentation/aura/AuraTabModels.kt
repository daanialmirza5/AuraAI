package com.aura.ai.presentation.aura

enum class AuraSub(
    val key: String,
) {
    Orb("orb"),
    Chat("chat"),
    Memory("memory"),
    ;

    companion object {
        fun fromKey(key: String?): AuraSub = entries.firstOrNull { it.key == key } ?: Orb
    }
}

enum class OrbState { Idle, Listening, Thinking, Speaking }

val OrbState.statusLabel: String
    get() =
        when (this) {
            OrbState.Idle -> "STANDING BY"
            OrbState.Listening -> "LISTENING…"
            OrbState.Thinking -> "PROCESSING"
            OrbState.Speaking -> "RESPONDING"
        }

val OrbState.caption: String
    get() =
        when (this) {
            OrbState.Idle -> "Say \"Hey AURA\" or tap below to start a voice session."
            OrbState.Listening -> "Go ahead — I'm listening."
            OrbState.Thinking -> "Cross-referencing your calendar, memory, and workspace…"
            OrbState.Speaking -> "You have two conflicts tomorrow. I've proposed a fix in your Timeline."
        }

val OrbState.buttonLabel: String
    get() = if (this == OrbState.Idle) "Tap to Speak" else "Listening…"
