package com.aura.ai.domain.model

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Encodes [WorkflowStep]s as a single `String` for Room storage — deliberately not
 * kotlinx.serialization: this project's own build history (`docs/ANDROID_AUTOMATION.md` §5) hit
 * real, hard-to-pin-down version-drift issues introducing kotlinx-serialization into a KSP
 * processor classpath, and `:app` doesn't otherwise need it. Every component is percent-encoded
 * (`java.net.URLEncoder`, plain JDK, no Android/Room-processor interaction at all) specifically so
 * a `,`, `=`, or `;` inside a tool argument value can never be confused with a delimiter.
 *
 * Pure functions — no Room, no Android — so they're directly unit-testable; see
 * `WorkflowStepEncodingTest`.
 */
internal object WorkflowStepEncoding {
    private const val STEP_DELIMITER = ";;"
    private const val NAME_DELIMITER = "|"
    private const val ARG_DELIMITER = ","
    private const val KV_DELIMITER = "="

    fun encode(steps: List<WorkflowStep>): String =
        steps.joinToString(STEP_DELIMITER) { step ->
            val args = step.arguments.entries.joinToString(ARG_DELIMITER) { (k, v) -> "${enc(k)}$KV_DELIMITER${enc(v)}" }
            "${enc(step.toolName)}$NAME_DELIMITER$args"
        }

    fun decode(raw: String): List<WorkflowStep> {
        if (raw.isBlank()) return emptyList()
        return raw.split(STEP_DELIMITER).map { stepText ->
            val (nameEncoded, argsText) = stepText.split(NAME_DELIMITER, limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            val arguments =
                if (argsText.isEmpty()) {
                    emptyMap()
                } else {
                    argsText.split(ARG_DELIMITER).associate { pair ->
                        val (k, v) = pair.split(KV_DELIMITER, limit = 2)
                        dec(k) to dec(v)
                    }
                }
            WorkflowStep(toolName = dec(nameEncoded), arguments = arguments)
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun dec(value: String): String = URLDecoder.decode(value, "UTF-8")
}
