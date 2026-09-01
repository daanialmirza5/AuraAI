package com.aura.ai.core.memory.model

/** *How long* a memory is kept — orthogonal to [MemoryCategory] (*what kind* it is). */
enum class MemoryTier {
    /** Session-scoped, cheap, expected to expire — "the user just said they're stressed about
     *  Monday's exam." Lives in `WorkingMemoryStore`, not the durable store. */
    Working,

    /** Durable, searchable, ranked, persisted across sessions. Lives in `LongTermMemoryStore`. */
    LongTerm,
}
