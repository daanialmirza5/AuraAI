package com.aura.ai.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.aura.ai.core.designsystem.theme.AuraColors

/** Small reusable 24x24-viewBox glyphs shared across Home / Workspace / Automate / Profile / Aura. */

@Composable
fun ChevronRightIcon(
    modifier: Modifier = Modifier,
    tint: Color = AuraColors.Accent,
) {
    Canvas(modifier = modifier) {
        withIconScale {
            val path =
                Path().apply {
                    moveTo(9f, 6f)
                    lineTo(15f, 12f)
                    lineTo(9f, 18f)
                }
            drawPath(path, tint, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun ChevronLeftIcon(
    modifier: Modifier = Modifier,
    tint: Color = AuraColors.TextPrimary,
) {
    Canvas(modifier = modifier) {
        withIconScale {
            val path =
                Path().apply {
                    moveTo(15f, 6f)
                    lineTo(9f, 12f)
                    lineTo(15f, 18f)
                }
            drawPath(path, tint, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun CloseIcon(
    modifier: Modifier = Modifier,
    tint: Color = AuraColors.TextPrimary.copy(alpha = 0.4f),
) {
    Canvas(modifier = modifier) {
        withIconScale {
            drawLine(tint, Offset(6f, 6f), Offset(18f, 18f), strokeWidth = 1.6f, cap = StrokeCap.Round)
            drawLine(tint, Offset(18f, 6f), Offset(6f, 18f), strokeWidth = 1.6f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun CheckIcon(
    modifier: Modifier = Modifier,
    tint: Color = AuraColors.OnAccent,
) {
    Canvas(modifier = modifier) {
        withIconScale {
            val path =
                Path().apply {
                    moveTo(5f, 12f)
                    lineTo(10f, 17f)
                    lineTo(19f, 7f)
                }
            drawPath(path, tint, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun SendIcon(
    modifier: Modifier = Modifier,
    tint: Color = AuraColors.OnAccent,
) {
    Canvas(modifier = modifier) {
        withIconScale {
            val path =
                Path().apply {
                    moveTo(4f, 20f)
                    lineTo(20f, 12f)
                    lineTo(4f, 4f)
                    lineTo(6.5f, 12f)
                    close()
                }
            drawPath(path, tint)
        }
    }
}

@Composable
fun BellIcon(
    modifier: Modifier = Modifier,
    tint: Color = AuraColors.TextPrimary,
) {
    Canvas(modifier = modifier) {
        withIconScale { drawBellGlyph(tint) }
    }
}

/** Shared with PermissionIcons.kt's Notifications row glyph — same source SVG path. */
fun DrawScope.drawBellGlyph(tint: Color) {
    val bell =
        Path().apply {
            moveTo(6f, 17f)
            lineTo(6f, 11.5f)
            arcTo(Rect(Offset(6f, 5.5f), Size(12f, 12f)), startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = false)
            lineTo(18f, 17f)
            lineTo(19.5f, 19f)
            lineTo(4.5f, 19f)
            close()
        }
    drawPath(bell, tint, style = Stroke(width = 1.6f, join = StrokeJoin.Round))
    drawArc(
        color = tint,
        startAngle = 180f,
        sweepAngle = -180f,
        useCenter = false,
        topLeft = Offset(10f, 18f),
        size = Size(4f, 4f),
        style = Stroke(width = 1.6f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.withIconScale(block: DrawScope.() -> Unit) {
    val factor = size.width / 24f
    scale(scaleX = factor, scaleY = factor, pivot = Offset.Zero, block = block)
}
