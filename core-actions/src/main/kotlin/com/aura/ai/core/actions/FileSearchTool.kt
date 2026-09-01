package com.aura.ai.core.actions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val MAX_RESULTS = 10

/**
 * Searches the device's `Downloads` collection by filename — deliberately scoped, not a general
 * "search all files" tool. `MediaStore.Downloads` is the one collection Android lets any app
 * query by name without a special permission on API 29+ (scoped storage's intentional exception
 * for that collection); a true all-files search would need `MANAGE_EXTERNAL_STORAGE`, a
 * highly-restricted permission with its own Play Store policy review that's out of scope here.
 * API 26–28 (pre-scoped-storage) falls back to `READ_EXTERNAL_STORAGE` against the same
 * collection query, honestly reporting if that permission isn't granted.
 */
class FileSearchTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "file_search"
        override val description = "Searches downloaded files by name."
        override val parameters =
            mapOf(
                "query" to ParameterSchema(ParameterType.String, "Filename or partial filename to search for", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val query = arguments["query"]?.trim().orEmpty()
            if (query.isEmpty()) {
                return AuraResult.Failure(AuraError.InvalidRequest("query is required."))
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                return AuraResult.Failure(AuraError.NotSupported("Storage permission has not been granted."))
            }

            val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.SIZE)
            val matches = mutableListOf<String>()
            context.contentResolver
                .query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
                    arrayOf("%$query%"),
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC",
                )?.use { cursor ->
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    while (cursor.moveToNext() && matches.size < MAX_RESULTS) {
                        matches += cursor.getString(nameColumn)
                    }
                }

            return if (matches.isEmpty()) {
                AuraResult.Success(ToolResult(summary = "No downloaded files match \"$query\"."))
            } else {
                AuraResult.Success(
                    ToolResult(
                        summary = "Found ${matches.size} file(s): ${matches.joinToString(", ")}",
                        data = matches.withIndex().associate { (i, name) -> "file$i" to name },
                    ),
                )
            }
        }
    }
