package com.aura.ai.core.memory.model

enum class MemorySourceType {
    /** Extracted from something the user said. */
    Conversation,

    /** Derived from other memories rather than stated directly (see [MemoryRelationship]). */
    Inference,

    /** Created by AURA itself (e.g. a system note), not attributed to the user. */
    System,

    /** Brought in from outside AURA — a future import/sync path. */
    Import,
}

/** [reference] is source-specific and optional: the conversation turn id for [MemorySourceType.Conversation],
 *  the memory id(s) it was inferred from for [MemorySourceType.Inference], etc. */
data class MemorySource(
    val type: MemorySourceType,
    val reference: String? = null,
)
