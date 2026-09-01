package com.aura.ai.domain.model

enum class MessageSender { User, Ai }

data class ChatMessage(
    val id: Long = 0L,
    val sender: MessageSender,
    val text: String,
    val timestampMillis: Long,
)
