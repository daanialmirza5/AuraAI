package com.aura.ai.core.memory.store

import com.aura.ai.core.memory.model.MemoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryWorkingMemoryStore
    @Inject
    constructor() : WorkingMemoryStore {
        private val entries = MutableStateFlow<Map<String, MemoryEntry>>(emptyMap())

        /** [MutableStateFlow.update] rather than a plain `entries.value = entries.value + ...`
         *  read-modify-write — the plain form is a lost-update race if two callers mutate
         *  concurrently (whichever assignment lands second silently discards the first's change);
         *  `update`'s internal compare-and-set retry loop is what actually makes a `@Singleton`
         *  mutable store safe to call from more than one coroutine at once, the same guarantee
         *  `InMemoryLongTermMemoryStore` gets from its `Mutex`. */
        override fun setActive(entry: MemoryEntry) {
            entries.update { it + (entry.id to entry) }
        }

        override fun getActive(): List<MemoryEntry> = entries.value.values.filterNot { it.isExpired }

        override fun remove(id: String) {
            entries.update { it - id }
        }

        override fun clear() {
            entries.value = emptyMap()
        }

        override fun observeActive(): Flow<List<MemoryEntry>> = entries.map { byId -> byId.values.filterNot { it.isExpired } }
    }
