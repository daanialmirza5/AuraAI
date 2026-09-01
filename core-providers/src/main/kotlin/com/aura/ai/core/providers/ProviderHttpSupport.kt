package com.aura.ai.core.providers

import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.ai.ToolDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** One shared, lenient JSON codec for every provider — none of these APIs are ours to control,
 *  so unknown fields must never fail a response we otherwise understood. */
internal val providerJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

/** Maps a transport-level failure (HTTP status + body, or a thrown exception) onto the closed
 *  [AuraError] set every other AURA subsystem already knows how to handle — the one place any of
 *  the four HTTP-backed providers needs to reason about status codes at all. */
internal object ProviderHttpErrors {
    fun fromStatus(
        providerId: String,
        code: Int,
        retryAfterMillis: Long?,
        body: String?,
    ): AuraError =
        when (code) {
            401, 403 ->
                AuraError.Authentication(
                    "$providerId rejected the request (HTTP $code) — check that the API key is valid.",
                )
            429 ->
                AuraError.RateLimited(
                    "$providerId rate-limited this request (HTTP 429).",
                    retryAfterMillis = retryAfterMillis,
                )
            in 400..499 ->
                AuraError.InvalidRequest(
                    "$providerId rejected the request (HTTP $code)" + (body?.take(300)?.let { ": $it" } ?: "."),
                )
            in 500..599 ->
                AuraError.ProviderUnavailable(
                    providerId,
                    "$providerId is temporarily unavailable (HTTP $code).",
                )
            else -> AuraError.Unknown("$providerId returned an unexpected HTTP $code.")
        }

    fun fromException(
        providerId: String,
        cause: Throwable,
    ): AuraError = AuraError.Network("Could not reach $providerId: ${cause.message ?: cause::class.simpleName}.", cause)
}

/** A JSON-Schema `object` built from a [ToolDescriptor.parameters] map — the one shape every
 *  major function-calling API (OpenAI, Claude, Gemini) accepts as-is or with a thin wrapper. */
internal fun ToolDescriptor.toJsonSchemaParameters(): JsonObject =
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            parameters.forEach { (name, schema) ->
                putJsonObject(name) {
                    put("type", schema.type.toJsonSchemaTypeName())
                    put("description", schema.description)
                    if (schema.enumValues.isNotEmpty()) {
                        put("enum", buildJsonArray { schema.enumValues.forEach { add(JsonPrimitive(it)) } })
                    }
                }
            }
        }
        val required = parameters.filterValues { it.required }.keys
        if (required.isNotEmpty()) {
            put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
        }
    }

private fun ParameterType.toJsonSchemaTypeName(): String =
    when (this) {
        ParameterType.String -> "string"
        ParameterType.Number -> "number"
        ParameterType.Boolean -> "boolean"
        ParameterType.Object -> "object"
        ParameterType.Array -> "array"
    }

private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonObject(
    key: String,
    builderAction: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
) {
    put(key, buildJsonObject(builderAction))
}

/** Parses a `{"key": "value", ...}`-shaped [JsonObject] of tool-call arguments down to
 *  `Map<String, String>` — [com.aura.ai.core.ai.ToolCallRequest.arguments] is deliberately
 *  string-valued (core-tools' [com.aura.ai.core.tools.Tool] takes string args), so richer JSON
 *  values are stringified rather than round-tripped structurally. */
internal fun JsonObject.toStringArgumentMap(): Map<String, String> =
    mapValues { (_, value) ->
        when (value) {
            is JsonPrimitive -> value.content
            else -> value.toString()
        }
    }
