package com.aura.ai.core.actions

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Renders plain text into a real, multi-page PDF via Android's native [PdfDocument] — no third
 * party library, no permission needed since it writes to the app's own cache directory. This is
 * the "Export PDF" step of the document pipeline; a genuinely working tool even though the
 * "Generate" step immediately before it (in [com.aura.ai.core.planner.TemplatePlanner]) has no
 * local implementation and requires a connected provider to actually produce [content].
 */
class ExportPdfTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        private companion object {
            const val PAGE_WIDTH = 595 // A4 at 72dpi
            const val PAGE_HEIGHT = 842
            const val MARGIN = 40f
        }

        override val name = "export_pdf"
        override val description = "Renders text content into a PDF file."
        override val parameters =
            mapOf(
                "fileName" to ParameterSchema(ParameterType.String, "The PDF file name", required = true),
                "content" to ParameterSchema(ParameterType.String, "The text content to render", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> =
            withContext(Dispatchers.IO) {
                val fileName = arguments["fileName"]?.trim().orEmpty().ifEmpty { "document.pdf" }
                val content = arguments["content"].orEmpty()
                if (content.isBlank()) {
                    return@withContext AuraResult.Failure(AuraError.InvalidRequest("content is required."))
                }

                try {
                    val paint = Paint().apply { textSize = 12f }
                    val lineHeight = paint.textSize + 6f
                    val maxTextWidth = PAGE_WIDTH - MARGIN * 2
                    val lines = wrapText(content, paint, maxTextWidth)

                    val document = PdfDocument()
                    var pageNumber = 1
                    var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    var canvas = page.canvas
                    var y = MARGIN + lineHeight

                    for (line in lines) {
                        if (y > PAGE_HEIGHT - MARGIN) {
                            document.finishPage(page)
                            pageNumber += 1
                            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                            canvas = page.canvas
                            y = MARGIN + lineHeight
                        }
                        canvas.drawText(line, MARGIN, y, paint)
                        y += lineHeight
                    }
                    document.finishPage(page)

                    val outputFile = File(context.cacheDir, fileName)
                    FileOutputStream(outputFile).use { document.writeTo(it) }
                    document.close()

                    AuraResult.Success(
                        ToolResult(summary = outputFile.absolutePath, data = mapOf("path" to outputFile.absolutePath)),
                    )
                } catch (e: Exception) {
                    AuraResult.Failure(AuraError.Unknown("Failed to export PDF.", e))
                }
            }

        private fun wrapText(
            text: String,
            paint: Paint,
            maxWidth: Float,
        ): List<String> {
            val result = mutableListOf<String>()
            for (paragraph in text.split("\n")) {
                if (paragraph.isEmpty()) {
                    result.add("")
                    continue
                }
                var current = StringBuilder()
                for (word in paragraph.split(" ")) {
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                        result.add(current.toString())
                        current = StringBuilder(word)
                    } else {
                        current = StringBuilder(candidate)
                    }
                }
                if (current.isNotEmpty()) result.add(current.toString())
            }
            return result
        }
    }
