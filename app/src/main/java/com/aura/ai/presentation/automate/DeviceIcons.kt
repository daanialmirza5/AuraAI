package com.aura.ai.presentation.automate

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.domain.model.DeviceIcon

@Composable
fun DeviceGlyph(
    icon: DeviceIcon,
    modifier: Modifier = Modifier,
    tint: Color = AuraColors.Accent,
) {
    Canvas(modifier = modifier) {
        val factor = size.width / 24f
        scale(scaleX = factor, scaleY = factor, pivot = Offset.Zero) {
            when (icon) {
                DeviceIcon.Bulb -> drawBulb(tint)
                DeviceIcon.Thermostat -> drawThermostat(tint)
                DeviceIcon.Lock -> drawLock(tint)
                DeviceIcon.Camera -> drawCamera(tint)
                DeviceIcon.Speaker -> drawSpeaker(tint)
            }
        }
    }
}

private fun DrawScope.drawBulb(tint: Color) {
    drawCircle(tint, radius = 5.2f, center = Offset(12f, 10f), style = Stroke(1.6f))
    drawRoundRect(
        color = tint,
        topLeft = Offset(9.5f, 14f),
        size = Size(5f, 3f),
        cornerRadius = CornerRadius(1f, 1f),
        style = Stroke(1.6f),
    )
    val lines =
        Path().apply {
            moveTo(9f, 18f)
            lineTo(15f, 18f)
            moveTo(10f, 21f)
            lineTo(14f, 21f)
        }
    drawPath(lines, tint, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
}

private fun DrawScope.drawThermostat(tint: Color) {
    drawLine(tint, Offset(12f, 3f), Offset(12f, 14f), strokeWidth = 1.6f, cap = StrokeCap.Round)
    drawCircle(tint, radius = 3.5f, center = Offset(12f, 17f), style = Stroke(1.6f))
}

private fun DrawScope.drawLock(tint: Color) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(5f, 11f),
        size = Size(14f, 9f),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(1.6f),
    )
    val shackle =
        Path().apply {
            moveTo(8f, 11f)
            lineTo(8f, 8f)
            arcTo(Rect(Offset(8f, 4f), Size(8f, 8f)), startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = false)
            lineTo(16f, 11f)
        }
    drawPath(shackle, tint, style = Stroke(1.6f))
}

private fun DrawScope.drawCamera(tint: Color) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(3f, 7f),
        size = Size(14f, 10f),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(1.6f),
    )
    val flap =
        Path().apply {
            moveTo(17f, 10f)
            lineTo(21f, 8f)
            lineTo(21f, 16f)
            lineTo(17f, 14f)
        }
    drawPath(flap, tint, style = Stroke(width = 1.6f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}

private fun DrawScope.drawSpeaker(tint: Color) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(6f, 3f),
        size = Size(12f, 18f),
        cornerRadius = CornerRadius(3f, 3f),
        style = Stroke(1.6f),
    )
    drawCircle(tint, radius = 3f, center = Offset(12f, 15f), style = Stroke(1.6f))
    drawCircle(tint, radius = 1f, center = Offset(12f, 7f))
}
