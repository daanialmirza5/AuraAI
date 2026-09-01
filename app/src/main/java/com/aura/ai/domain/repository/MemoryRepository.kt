package com.aura.ai.domain.repository

import com.aura.ai.domain.model.Memory
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun observeMemories(): Flow<List<Memory>>

    suspend fun forget(id: String)

    /** Irreversible — deletes every stored [Memory]. Backs the Profile screen's "Clear all
     *  memory" action, which is why it exists as its own explicit method rather than callers
     *  looping [forget] over [observeMemories]'s current value. */
    suspend fun forgetAll()
}
