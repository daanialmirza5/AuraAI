package com.aura.ai.data.repository

import com.aura.ai.data.local.dao.ChatMessageDao
import com.aura.ai.data.local.entity.ChatMessageEntity
import com.aura.ai.domain.model.ChatMessage
import com.aura.ai.domain.model.MessageSender
import com.aura.ai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private fun ChatMessageEntity.toDomain() =
    ChatMessage(
        id = id,
        sender = if (sender == "user") MessageSender.User else MessageSender.Ai,
        text = text,
        timestampMillis = timestampMillis,
    )

@Singleton
class ChatRepositoryImpl
    @Inject
    constructor(
        private val dao: ChatMessageDao,
    ) : ChatRepository {
        override fun observeMessages(): Flow<List<ChatMessage>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun appendMessage(
            sender: MessageSender,
            text: String,
        ): ChatMessage {
            val entity =
                ChatMessageEntity(
                    sender = if (sender == MessageSender.User) "user" else "ai",
                    text = text,
                    timestampMillis = System.currentTimeMillis(),
                )
            val id = dao.insert(entity)
            return entity.copy(id = id).toDomain()
        }
    }
