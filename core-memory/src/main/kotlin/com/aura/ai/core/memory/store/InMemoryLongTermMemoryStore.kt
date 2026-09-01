package com.aura.ai.core.memory.store

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.MemoryCategory
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.model.MemoryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryLongTermMemoryStore
    @Inject
    constructor() : LongTermMemoryStore {
        private val mutex = Mutex()
        private val entries = MutableStateFlow<Map<String, MemoryEntry>>(emptyMap())

        override suspend fun remember(entry: MemoryEntry): AuraResult<MemoryEntry> =
            mutex.withLock {
                entries.value = entries.value + (entry.id to entry)
                AuraResult.Success(entry)
            }

        override suspend fun update(entry: MemoryEntry): AuraResult<MemoryEntry> =
            mutex.withLock {
                if (!entries.value.containsKey(entry.id)) {
                    return@withLock AuraResult.Failure(AuraError.InvalidRequest("No memory with id ${entry.id}."))
                }
                entries.value = entries.value + (entry.id to entry)
                AuraResult.Success(entry)
            }

        override suspend fun get(id: String): MemoryEntry? = entries.value[id]

        override suspend fun recall(
            query: String,
            limit: Int,
            includeArchived: Boolean,
        ): AuraResult<List<MemoryEntry>> {
            val needle = query.trim().lowercase()
            val matches =
                entries.value.values
                    .filter { includeArchived || it.status == MemoryStatus.Active }
                    .filter { entry ->
                        needle.isEmpty() ||
                            entry.content.lowercase().contains(needle) ||
                            entry.tags.any { it.lowercase().contains(needle) }
                    }.sortedByDescending { it.importance }
                    .take(limit)

            return mutex.withLock {
                val now = System.currentTimeMillis()
                val touched = matches.map { it.copy(lastAccessedAtMillis = now) }
                entries.value = entries.value + touched.associateBy { it.id }
                AuraResult.Success(touched)
            }
        }

        override suspend fun byCategory(
            category: MemoryCategory,
            includeArchived: Boolean,
        ): AuraResult<List<MemoryEntry>> {
            val matches =
                entries.value.values
                    .filter { it.category == category }
                    .filter { includeArchived || it.status == MemoryStatus.Active }
            return AuraResult.Success(matches)
        }

        override suspend fun archive(id: String): AuraResult<Unit> =
            mutex.withLock {
                val existing =
                    entries.value[id]
                        ?: return@withLock AuraResult.Failure(AuraError.InvalidRequest("No memory with id $id."))
                entries.value = entries.value + (id to existing.copy(status = MemoryStatus.Archived))
                AuraResult.Success(Unit)
            }

        override suspend fun delete(id: String): AuraResult<Unit> =
            mutex.withLock {
                entries.value = entries.value - id
                AuraResult.Success(Unit)
            }

        override fun observeAll(includeArchived: Boolean): Flow<List<MemoryEntry>> =
            entries.map { byId -> byId.values.filter { includeArchived || it.status == MemoryStatus.Active } }
    }
