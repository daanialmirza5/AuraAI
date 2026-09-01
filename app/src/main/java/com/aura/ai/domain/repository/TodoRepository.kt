package com.aura.ai.domain.repository

import com.aura.ai.domain.model.Todo
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun observeTodos(): Flow<List<Todo>>

    suspend fun toggleDone(id: String)
}
