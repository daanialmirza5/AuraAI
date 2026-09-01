package com.aura.ai.core.memory.model

enum class RelationshipType {
    /** Generic association — the default when a stronger relation isn't known. */
    RelatesTo,

    /** This memory only makes sense in light of the target — e.g. a Task that depends on a Goal. */
    DependsOn,

    /** This memory replaces the target as the current truth — e.g. a corrected preference. */
    Supersedes,

    /** This memory is a component of a larger one — e.g. a Task that's part of a Project. */
    PartOf,

    /** This memory conflicts with the target — surfaced during ranking/forgetting rather than
     *  silently resolved, since deciding which one is right isn't this layer's job. */
    Contradicts,
}

data class MemoryRelationship(
    val targetMemoryId: String,
    val type: RelationshipType,
)
