package com.aura.ai.core.memory.model

/**
 * *What kind* of information a memory holds — orthogonal to [MemoryTier] (*how long* it's kept).
 * A [MemoryCategory.Goal] memory can be [MemoryTier.Working] just as easily as
 * [MemoryTier.LongTerm]; the two axes are independent by design.
 */
enum class MemoryCategory {
    /** A standing like/dislike/choice — "I prefer Kotlin," "my birthday is June 20." */
    Preference,

    /** Something the user is working toward with no fixed deliverable — "my exam is next Monday." */
    Goal,

    /** A named, ongoing effort with scope — "I am working on RootAI." */
    Project,

    /** A discrete, actionable to-do, usually short-lived once done. */
    Task,

    /** A durable fact, learned or told, that isn't personal preference — "the meeting room is 4B." */
    Knowledge,

    /** Something that happened — a specific past event or interaction, not a standing fact. */
    Episodic,

    /** Doesn't cleanly fit the above — the classifier's honest fallback rather than a forced,
     *  low-confidence guess into one of the more specific categories. */
    General,

    // The six added for Version 1.0's Knowledge Graph milestone (docs/KNOWLEDGE_GRAPH.md) —
    // together with the existing Project/Task/Goal above, these are the entity types
    // relationships (MemoryRelationship) actually connect: "who," "what effort," "what's due,"
    // "what was discussed," "what was read," "what was produced."

    /** A named individual — "Sarah is my manager." The usual source of `PartOf`/`RelatesTo` edges
     *  onto [Project]/[Meeting] memories ("Sarah" relates to the project she manages). */
    Person,

    /** A specific, referenceable written artifact — a report, a set of notes, a spec. Distinct
     *  from [File]: a Document is authored content; a File is a stored artifact reference. */
    Document,

    /** A specific, usually dated, get-together — "meeting with Sarah on the Q3 report." */
    Meeting,

    /** A specific piece of work with an owner and (usually) a due date — distinct from the more
     *  general [Task] by implying it was *given*, not self-initiated. */
    Assignment,

    /** Something being investigated or read up on, not yet a settled [Knowledge] fact. */
    Research,

    /** A stored artifact reference — a photo, a PDF, an attachment. See [Document]. */
    File,
}
