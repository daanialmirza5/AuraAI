package com.aura.ai.core.ai

/** JSON-Schema-ish primitive types — deliberately the same small set every major function-calling
 *  API (OpenAI/Gemini/Claude) already normalizes to, so a [ToolDescriptor] maps onto any of them
 *  without translation once core-providers is actually connected. */
enum class ParameterType { String, Number, Boolean, Object, Array }

data class ParameterSchema(
    val type: ParameterType,
    val description: String,
    val required: Boolean = false,
    val enumValues: List<String> = emptyList(),
)

/**
 * The provider-facing description of one callable capability — what core-tools' `Tool` interface
 * is reduced to when it's advertised to an [AIProvider] as part of a [GenerationRequest]. Kept in
 * core-ai (rather than core-tools) specifically so core-ai never has to depend on core-tools —
 * core-tools depends on core-ai and maps `Tool -> ToolDescriptor`, not the other way around.
 */
data class ToolDescriptor(
    val name: String,
    val description: String,
    val parameters: Map<String, ParameterSchema> = emptyMap(),
)
