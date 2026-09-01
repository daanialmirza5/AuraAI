package com.aura.ai.core.actions

import android.content.Context
import com.aura.ai.core.ai.AuraError
import com.aura.ai.core.ai.AuraResult
import com.aura.ai.core.ai.ParameterSchema
import com.aura.ai.core.ai.ParameterType
import com.aura.ai.core.tools.Tool
import com.aura.ai.core.tools.ToolResult
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject

/** Scans QR codes and traditional barcodes from an image via ML Kit's on-device scanner — one
 *  API covers both formats, so this is "QR scanner" and "barcode scanner" from the brief
 *  together, not two separate tools. */
class BarcodeScanTool
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Tool {
        override val name = "scan_barcode"
        override val description = "Scans QR codes and barcodes from an image."
        override val parameters =
            mapOf(
                "imageUri" to ParameterSchema(ParameterType.String, "content:// or file:// URI of the image", required = true),
            )

        override suspend fun execute(arguments: Map<String, String>): AuraResult<ToolResult> {
            val uri =
                arguments.parseImageUri()
                    ?: return AuraResult.Failure(AuraError.InvalidRequest("imageUri is required and must be a valid URI."))
            return try {
                val image = InputImage.fromFilePath(context, uri)
                val barcodes = BarcodeScanning.getClient().process(image).await()
                val values = barcodes.mapNotNull { it.rawValue }
                if (values.isEmpty()) {
                    AuraResult.Success(ToolResult(summary = "No QR code or barcode found in the image."))
                } else {
                    AuraResult.Success(
                        ToolResult(
                            summary = values.joinToString("; "),
                            data = values.withIndex().associate { (i, v) -> "code$i" to v },
                        ),
                    )
                }
            } catch (e: IOException) {
                AuraResult.Failure(AuraError.InvalidRequest("Couldn't open that image."))
            } catch (e: Exception) {
                AuraResult.Failure(AuraError.Unknown("Barcode scanning failed.", e))
            }
        }
    }
