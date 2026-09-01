package com.aura.ai.presentation.permissions

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.aura.ai.core.designsystem.components.drawBellGlyph
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.domain.model.AppPermission

@Composable
fun PermissionGlyph(
    permission: AppPermission,
    modifier: Modifier = Modifier,
) {
    val tint = AuraColors.Accent
    Canvas(modifier = modifier) {
        val factor = size.width / 24f
        scale(scaleX = factor, scaleY = factor, pivot = Offset.Zero) {
            when (permission) {
                AppPermission.Microphone -> drawMic(tint)
                AppPermission.Notifications -> drawBell(tint)
                AppPermission.Calendar -> drawCalendar(tint)
                AppPermission.Location -> drawPin(tint)
                AppPermission.Contacts -> drawContacts(tint)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMic(tint: Color) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(8.5f, 2f),
        size = Size(7f, 9.5f),
        cornerRadius = CornerRadius(3.5f, 3.5f),
        style = Stroke(1.6f),
    )
    drawArc(
        color = tint,
        startAngle = 180f,
        sweepAngle = -180f,
        useCenter = false,
        topLeft = Offset(5.5f, 4.5f),
        size = Size(13f, 13f),
        style = Stroke(width = 1.6f, cap = StrokeCap.Round),
    )
    drawLine(tint, Offset(12f, 17.5f), Offset(12f, 21f), strokeWidth = 1.6f, cap = StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBell(tint: Color) {
    drawBellGlyph(tint)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCalendar(tint: Color) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(4f, 5.5f),
        size = Size(16f, 14f),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(1.6f),
    )
    val ticks =
        Path().apply {
            moveTo(4f, 10f)
            lineTo(20f, 10f)
            moveTo(8f, 3.5f)
            lineTo(8f, 6.5f)
            moveTo(16f, 3.5f)
            lineTo(16f, 6.5f)
        }
    drawPath(ticks, tint, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPin(tint: Color) {
    val pin =
        Path().apply {
            moveTo(12f, 21f)
            cubicTo(12f, 21f, 19f, 14.4f, 19f, 9f)
            arcTo(Rect(Offset(5f, 2f), Size(14f, 14f)), startAngleDegrees = 0f, sweepAngleDegrees = -180f, forceMoveTo = false)
            cubicTo(5f, 14.4f, 12f, 21f, 12f, 21f)
            close()
        }
    drawPath(pin, tint, style = Stroke(width = 1.6f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    drawCircle(tint, radius = 2.5f, center = Offset(12f, 9f), style = Stroke(1.6f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawContacts(tint: Color) {
    drawCircle(tint, radius = 3.2f, center = Offset(12f, 8f), style = Stroke(1.6f))
    val shoulders =
        Path().apply {
            moveTo(5f, 20f)
            cubicTo(6.2f, 16f, 9f, 14f, 12f, 14f)
            cubicTo(15f, 14f, 17.8f, 16f, 19f, 20f)
        }
    drawPath(
        shoulders,
        tint,
        style =
            Stroke(
                width = 1.6f,
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
    )
}
