package com.aura.ai.core.reasoning.goal

import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.retrieval.MemoryRetriever
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGoalManager
    @Inject
    constructor(
        private val memoryRetriever: MemoryRetriever,
    ) : GoalManager {
        private companion object {
            const val RELATED_MEMORY_LIMIT = 10
        }

        override suspend fun establishGoal(rawGoal: String): AuraResult<ActiveGoal> {
            val relatedResult = memoryRetriever.retrieve(rawGoal, limit = RELATED_MEMORY_LIMIT)
            val related =
                when (relatedResult) {
                    is AuraResult.Success -> relatedResult.value
                    is AuraResult.Failure -> return relatedResult
                }

            return AuraResult.Success(
                ActiveGoal(
                    id = UUID.randomUUID().toString(),
                    rawText = rawGoal,
                    relatedMemories = related,
                    establishedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }
