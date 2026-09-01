package com.aura.ai.core.actions

import android.content.Context
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
import javax.inject.Inject

/** Copies a file from cache (typically wherever [ExportPdfTool] just wrote to) into the app's
 *  permanent internal storage — deliberately a separate step from "export," matching the
 *  brief's own pipeline: rendering and persisting are different concerns. */
class SaveFileTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "save_file"
        override val description = "Saves a file from a temporary location into permanent app storage."
        override val parameters =
            mapOf(
                "sourcePath" to ParameterSchema(ParameterType.String, "The temporary file path to save", required = true),
                "fileName" to ParameterSchema(ParameterType.String, "The name to save it under", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> =
            withContext(Dispatchers.IO) {
                val sourcePath = arguments["sourcePath"]?.trim().orEmpty()
                val fileName = arguments["fileName"]?.trim().orEmpty().ifEmpty { "file" }
                if (sourcePath.isEmpty()) {
                    return@withContext AuraResult.Failure(AuraError.InvalidRequest("sourcePath is required."))
                }

                try {
                    val source = File(sourcePath)
                    if (!source.exists()) {
                        return@withContext AuraResult.Failure(AuraError.InvalidRequest("Source file does not exist: $sourcePath"))
                    }
                    val destination = File(context.filesDir, fileName)
                    source.copyTo(destination, overwrite = true)
                    AuraResult.Success(
                        ToolResult(summary = destination.absolutePath, data = mapOf("path" to destination.absolutePath)),
                    )
                } catch (e: Exception) {
                    AuraResult.Failure(AuraError.Unknown("Failed to save file.", e))
                }
            }
    }
