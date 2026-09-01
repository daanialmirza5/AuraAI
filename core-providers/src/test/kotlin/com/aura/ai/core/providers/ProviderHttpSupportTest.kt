package com.aura.ai.core.providers

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.ai.ToolDescriptor
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure, provider-agnostic helpers every HTTP-backed provider shares — tested directly since
 *  a mistake here would silently corrupt every provider's tool-calling and error handling at once. */
class ProviderHttpSupportTest {
    @Test
    fun `fromStatus maps every documented status range to the matching AuraError`() {
        assertTrue(ProviderHttpErrors.fromStatus("p", 401, null, null) is AuraError.Authentication)
        assertTrue(ProviderHttpErrors.fromStatus("p", 403, null, null) is AuraError.Authentication)
        assertTrue(ProviderHttpErrors.fromStatus("p", 429, null, null) is AuraError.RateLimited)
        assertTrue(ProviderHttpErrors.fromStatus("p", 400, null, null) is AuraError.InvalidRequest)
        assertTrue(ProviderHttpErrors.fromStatus("p", 404, null, null) is AuraError.InvalidRequest)
        assertTrue(ProviderHttpErrors.fromStatus("p", 500, null, null) is AuraError.ProviderUnavailable)
        assertTrue(ProviderHttpErrors.fromStatus("p", 503, null, null) is AuraError.ProviderUnavailable)
        assertTrue(ProviderHttpErrors.fromStatus("p", 200, null, null) is AuraError.Unknown)
    }

    @Test
    fun `fromException preserves the cause`() {
        val cause = java.io.IOException("timeout")
        val error = ProviderHttpErrors.fromException("Claude", cause) as AuraError.Network
        assertEquals(cause, error.cause)
        assertTrue(error.message.contains("Claude"))
    }

    @Test
    fun `toJsonSchemaParameters produces a valid object schema with required fields`() {
        val descriptor =
            ToolDescriptor(
                name = "search",
                description = "Search the web",
                parameters =
                    mapOf(
                        "query" to ParameterSchema(ParameterType.String, "search text", required = true),
                        "limit" to ParameterSchema(ParameterType.Number, "max results"),
                    ),
            )

        val schema = descriptor.toJsonSchemaParameters()

        assertEquals("object", schema["type"]?.jsonPrimitive?.content)
        val properties = schema["properties"]!!.jsonObject
        assertEquals("string", properties["query"]!!.jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("number", properties["limit"]!!.jsonObject["type"]?.jsonPrimitive?.content)
        val required = schema["required"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("query"), required)
    }

    @Test
    fun `toJsonSchemaParameters omits required when nothing is required`() {
        val descriptor = ToolDescriptor(name = "noop", description = "does nothing")
        val schema = descriptor.toJsonSchemaParameters()
        assertEquals(null, schema["required"])
    }

    @Test
    fun `toStringArgumentMap stringifies non-primitive values instead of throwing`() {
        val obj =
            providerJson
                .parseToJsonElement(
                    """{"name": "Ann", "age": 30, "tags": ["a", "b"]}""",
                ).jsonObject

        val map = obj.toStringArgumentMap()

        assertEquals("Ann", map["name"])
        assertEquals("30", map["age"])
        assertTrue(map["tags"]!!.contains("a"))
    }
}
