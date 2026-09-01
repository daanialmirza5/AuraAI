package com.aura.ai.domain.model

enum class TodoPriority { High, Medium, Low, Done }

data class Todo(
    val id: String,
    val title: String,
    val meta: String,
    val done: Boolean,
    val priority: TodoPriority,
)
