package com.aura.ai.core.reasoning.decomposition

import com.aura.ai.core.intent.IntentType
import com.aura.ai.core.intent.RecognizedIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun intent(
    type: IntentType,
    rawText: String,
) = RecognizedIntent(type = type, confidence = 1f, rawText = rawText)

class RuleBasedTaskDecomposerTest {
    private val decomposer = RuleBasedTaskDecomposer()

    @Test
    fun `a document-creation goal decomposes into the five-step research-to-notify pipeline`() {
        val result =
            decomposer.decompose(
                "create my AIML assignment and save it as PDF",
                intent(IntentType.Research, "create my AIML assignment and save it as PDF"),
            )

        assertEquals(5, result.size)
        assertEquals(listOf("web_search", null, "export_pdf", "save_file", "notify"), result.map { it.impliedToolName })
        assertTrue(result[1].requiresAI)
        // Each step after the first depends on the one before it, in order.
        assertEquals(listOf("1"), result[1].dependsOn)
        assertEquals(listOf("4"), result[4].dependsOn)
    }

    @Test
    fun `document keywords only trigger the pipeline for a plausible intent`() {
        val result = decomposer.decompose("open the assignment app", intent(IntentType.OpenApp, "open the assignment app"))

        assertEquals(1, result.size)
        assertEquals("open_app", result.single().impliedToolName)
    }

    @Test
    fun `a goal with no document keyword falls through to a single sub-goal even for a plausible intent`() {
        val result = decomposer.decompose("what's the weather like", intent(IntentType.Research, "what's the weather like"))

        assertEquals(1, result.size)
        assertEquals("web_search", result.single().impliedToolName)
    }

    @Test
    fun `each recognized intent type maps to its own documented tool`() {
        val cases =
            mapOf(
                IntentType.OpenApp to "open_app",
                IntentType.Reminder to "set_reminder",
                IntentType.Calendar to "create_calendar_event",
                IntentType.Navigation to "navigate",
                IntentType.Email to "compose_email",
                IntentType.Shopping to "shopping_search",
                IntentType.Research to "web_search",
                IntentType.Automation to "app_automation",
            )

        cases.forEach { (type, expectedTool) ->
            val result = decomposer.decompose("do something", intent(type, "do something"))
            assertEquals("$type should imply $expectedTool", expectedTool, result.single().impliedToolName)
            assertTrue("$type should not require AI directly", !result.single().requiresAI)
        }
    }

    @Test
    fun `Coding and Conversation and Unknown intents have no implied tool and require AI`() {
        listOf(IntentType.Coding, IntentType.Conversation, IntentType.Unknown).forEach { type ->
            val result = decomposer.decompose("do something", intent(type, "do something"))
            assertNull(result.single().impliedToolName)
            assertTrue(result.single().requiresAI)
        }
    }
}
