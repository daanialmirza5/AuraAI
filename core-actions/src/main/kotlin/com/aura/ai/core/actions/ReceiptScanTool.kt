package com.aura.ai.core.actions

import android.content.Context
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

/** OCR (see [OcrTool]/[extractTextFromImage]) plus lightweight, honestly-heuristic parsing for
 *  the three things someone usually wants off a receipt: merchant, total, date. This is pattern
 *  matching over recognized text, not a trained receipt-understanding model — it will
 *  occasionally miss or misread on unusual receipt layouts, which [ToolResult.summary] states
 *  plainly (via [ReceiptSummary.confidence]) rather than presenting a guess as certain. */
class ReceiptScanTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "scan_receipt"
        override val description = "Extracts merchant, total, and date from a photo of a receipt."
        override val parameters =
            mapOf(
                "imageUri" to ParameterSchema(ParameterType.String, "content:// or file:// URI of the receipt photo", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val uri =
                arguments.parseImageUri()
                    ?: return AuraResult.Failure(AuraError.InvalidRequest("imageUri is required and must be a valid URI."))
            return try {
                val text = context.extractTextFromImage(uri)
                if (text.isBlank()) {
                    return AuraResult.Success(ToolResult(summary = "No text found in the image."))
                }
                val receipt = parseReceipt(text)
                AuraResult.Success(
                    ToolResult(
                        summary = receipt.describe(),
                        data =
                            buildMap {
                                receipt.merchant?.let { put("merchant", it) }
                                receipt.total?.let { put("total", it) }
                                receipt.date?.let { put("date", it) }
                                put("rawText", text)
                            },
                    ),
                )
            } catch (e: IOException) {
                AuraResult.Failure(AuraError.InvalidRequest("Couldn't open that image."))
            } catch (e: Exception) {
                AuraResult.Failure(AuraError.Unknown("Receipt scanning failed.", e))
            }
        }
    }

internal data class ReceiptSummary(
    val merchant: String?,
    val total: String?,
    val date: String?,
) {
    fun describe(): String {
        if (merchant == null && total == null && date == null) {
            return "Couldn't confidently parse this receipt — see the raw extracted text."
        }
        return buildString {
            append(merchant ?: "Unknown merchant")
            total?.let { append(" — total $it") }
            date?.let { append(" — $it") }
        }
    }
}

private val TOTAL_LINE = Regex("""(?i)\btotal\b.*?([$£€]?\s?\d+[.,]\d{2})""")
private val ANY_AMOUNT = Regex("""[$£€]\s?\d+[.,]\d{2}""")
private val DATE_PATTERN = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""")

/** `internal`, not `private`, specifically so it's unit-testable in isolation from ML Kit/Android
 *  — see `ReceiptScanToolTest`. Pure function: text in, best-effort structured guess out. */
internal fun parseReceipt(text: String): ReceiptSummary {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    val total =
        lines.firstNotNullOfOrNull { line ->
            TOTAL_LINE
                .find(line)
                ?.groupValues
                ?.get(1)
                ?.trim()
        }
            ?: ANY_AMOUNT.findAll(text).map { it.value }.maxByOrNull { amount ->
                amount
                    .filter { it.isDigit() || it == '.' || it == ',' }
                    .replace(",", ".")
                    .toDoubleOrNull() ?: 0.0
            }

    val date = DATE_PATTERN.find(text)?.groupValues?.get(1)

    // Receipts conventionally print the merchant name as the first printed line — a real
    // heuristic, not a guarantee (some print a header logo/address block first instead).
    val merchant = lines.firstOrNull { line -> line.none { it.isDigit() } && line.length in 3..40 }

    return ReceiptSummary(merchant = merchant, total = total, date = date)
}
