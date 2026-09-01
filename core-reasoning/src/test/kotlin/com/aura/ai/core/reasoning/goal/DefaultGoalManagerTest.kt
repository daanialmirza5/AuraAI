package com.aura.ai.core.reasoning.goal

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.memory.model.MemoryCategory
import com.aura.ai.core.memory.model.MemoryEntry
import com.aura.ai.core.memory.retrieval.MemoryRetriever
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeMemoryRetriever(
    private val result: AuraResult<List<MemoryEntry>>,
) : MemoryRetriever {
    var lastQuery: String? = null
    var lastLimit: Int? = null

    override suspend fun retrieve(
        query: String,
        limit: Int,
    ): AuraResult<List<MemoryEntry>> {
        lastQuery = query
        lastLimit = limit
        return result
    }
}

class DefaultGoalManagerTest {
    @Test
    fun `establishGoal packages the raw goal with whatever related memories were retrieved`() =
        runTest {
            val memory = MemoryEntry(content = "I prefer Kotlin", category = MemoryCategory.Preference)
            val retriever = FakeMemoryRetriever(AuraResult.Success(listOf(memory)))
            val manager = DefaultGoalManager(retriever)

            val result = manager.establishGoal("write me a script")

            check(result is AuraResult.Success)
            assertEquals("write me a script", result.value.rawText)
            assertEquals(listOf(memory), result.value.relatedMemories)
            assertTrue(result.value.id.isNotBlank())
        }

    @Test
    fun `establishGoal queries the retriever with the raw goal text and a bounded limit`() =
        runTest {
            val retriever = FakeMemoryRetriever(AuraResult.Success(emptyList()))
            val manager = DefaultGoalManager(retriever)

            manager.establishGoal("what's on my calendar today")

            assertEquals("what's on my calendar today", retriever.lastQuery)
            assertEquals(10, retriever.lastLimit)
        }

    @Test
    fun `a retrieval failure propagates as the goal's own failure instead of a partial goal`() =
        runTest {
            val error = AuraError.NotSupported("retrieval unavailable")
            val retriever = FakeMemoryRetriever(AuraResult.Failure(error))
            val manager = DefaultGoalManager(retriever)

            val result = manager.establishGoal("anything")

            assertEquals(AuraResult.Failure(error), result)
        }
}
