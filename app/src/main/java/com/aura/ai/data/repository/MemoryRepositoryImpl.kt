package com.aura.ai.data.repository

import com.aura.ai.data.local.dao.MemoryDao
import com.aura.ai.data.local.entity.MemoryEntity
import com.aura.ai.domain.model.Memory
import com.aura.ai.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private fun MemoryEntity.toDomain() = Memory(id = id, title = title, detail = detail, tag = tag)

@Singleton
class MemoryRepositoryImpl
    @Inject
    constructor(
        private val dao: MemoryDao,
    ) : MemoryRepository {
        override fun observeMemories(): Flow<List<Memory>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun forget(id: String) = dao.delete(id)

        override suspend fun forgetAll() = dao.deleteAll()
    }
