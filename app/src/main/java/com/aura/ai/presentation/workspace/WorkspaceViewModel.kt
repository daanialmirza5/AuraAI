package com.aura.ai.presentation.workspace

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.domain.model.Todo
import com.aura.ai.domain.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class WorkspaceUiState(
    val sub: WorkspaceSub = WorkspaceSub.Timeline,
    val todos: ImmutableList<Todo> = persistentListOf(),
)

@HiltViewModel
class WorkspaceViewModel
    @Inject
    constructor(
        private val todoRepository: TodoRepository,
    ) : ViewModel() {
        private val sub = MutableStateFlow(WorkspaceSub.Timeline)

        val uiState: StateFlow<WorkspaceUiState> =
            combine(
                sub,
                todoRepository.observeTodos().map { it.toPersistentList() },
            ) { sub, todos ->
                WorkspaceUiState(sub = sub, todos = todos)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkspaceUiState())

        fun setSub(value: WorkspaceSub) {
            sub.value = value
        }

        fun toggleTodo(id: String) {
            viewModelScope.launch { todoRepository.toggleDone(id) }
        }
    }
