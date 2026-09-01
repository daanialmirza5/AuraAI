package com.aura.ai.core.actions

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** Shared by [OcrTool] and [ReceiptScanTool] — both need the same "read whatever text is in this
 *  image" step; only what happens to the result differs. On-device (ML Kit's bundled Latin model),
 *  so this never touches the network or costs anything per call, unlike
 *  `com.aura.ai.ai.ImageUnderstandingTool`. */
internal suspend fun Context.extractTextFromImage(uri: Uri): String {
    val image = InputImage.fromFilePath(this, uri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return recognizer.process(image).await().text
}
