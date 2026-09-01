package com.aura.ai.core.memory.model

/** The lifecycle state a [MemoryEntry] is in — see `MemoryLifecycleManager` for the transitions
 *  between these ([MemoryStatus.Active] -> [MemoryStatus.Archived] via archive/expire; either
 *  state can be hard-deleted outright). Archived memories are excluded from retrieval/ranking
 *  by default but aren't gone — that's what makes archiving reversible and deletion not. */
enum class MemoryStatus { Active, Archived }
