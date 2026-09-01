package com.aura.ai.data.repository

import com.aura.ai.data.local.dao.TodoDao
import com.aura.ai.data.local.entity.TodoEntity
import com.aura.ai.domain.model.Todo
import com.aura.ai.domain.model.TodoPriority
import com.aura.ai.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private fun TodoEntity.toDomain() =
    Todo(
        id = id,
        title = title,
        meta = meta,
        done = done,
        priority =
            when (priority) {
                "HIGH" -> TodoPriority.High
                "MEDIUM" -> TodoPriority.Medium
                "DONE" -> TodoPriority.Done
                else -> TodoPriority.Low
            },
    )

@Singleton
class TodoRepositoryImpl
    @Inject
    constructor(
        private val dao: TodoDao,
    ) : TodoRepository {
        override fun observeTodos(): Flow<List<Todo>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun toggleDone(id: String) = dao.toggleDone(id)
    }
