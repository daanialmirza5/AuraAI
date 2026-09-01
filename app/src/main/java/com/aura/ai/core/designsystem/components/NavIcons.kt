package com.aura.ai.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

/** The five bottom-nav glyphs, redrawn path-for-path from the source SVGs (24x24 viewBox). */
enum class NavIconType { Home, Aura, Workspace, Automate, Profile }

@Composable
fun NavGlyph(
    type: NavIconType,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val factor = size.width / 24f
        scale(scaleX = factor, scaleY = factor, pivot = Offset.Zero) {
            when (type) {
                NavIconType.Home -> {
                    val path =
                        Path().apply {
                            moveTo(4f, 11.5f)
                            lineTo(12f, 5f)
                            lineTo(20f, 11.5f)
                            lineTo(20f, 19f)
                            lineTo(19f, 20f)
                            lineTo(14.5f, 20f)
                            lineTo(14.5f, 14f)
                            lineTo(9.5f, 14f)
                            lineTo(9.5f, 20f)
                            lineTo(5f, 20f)
                            lineTo(4f, 19f)
                            close()
                        }
                    drawPath(
                        path,
                        color = tint,
                        style = Stroke(width = 1.6f, join = StrokeJoin.Round, cap = StrokeCap.Round),
                    )
                }

                NavIconType.Aura -> {
                    drawCircle(tint, radius = 5.5f, center = Offset(12f, 12f), style = Stroke(1.6f))
                    drawCircle(tint.copy(alpha = tint.alpha * 0.5f), radius = 9f, center = Offset(12f, 12f), style = Stroke(1f))
                }

                NavIconType.Workspace -> {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(3.5f, 4.5f),
                        size = Size(17f, 15f),
                        cornerRadius = CornerRadius(2f, 2f),
                        style = Stroke(1.6f),
                    )
                    val lines =
                        Path().apply {
                            moveTo(3.5f, 9.5f)
                            lineTo(20.5f, 9.5f)
                            moveTo(8f, 4.5f)
                            lineTo(8f, 3f)
                            moveTo(16f, 4.5f)
                            lineTo(16f, 3f)
                        }
                    drawPath(lines, color = tint, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
                }

                NavIconType.Automate -> {
                    val rays =
                        Path().apply {
                            moveTo(12f, 3f)
                            lineTo(12f, 6f)
                            moveTo(12f, 18f)
                            lineTo(12f, 21f)
                            moveTo(3f, 12f)
                            lineTo(6f, 12f)
                            moveTo(18f, 12f)
                            lineTo(21f, 12f)
                            moveTo(5.6f, 5.6f)
                            lineTo(7.7f, 7.7f)
                            moveTo(16.3f, 16.3f)
                            lineTo(18.4f, 18.4f)
                            moveTo(5.6f, 18.4f)
                            lineTo(7.7f, 16.3f)
                            moveTo(16.3f, 7.7f)
                            lineTo(18.4f, 5.6f)
                        }
                    drawPath(rays, color = tint, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
                    drawCircle(tint, radius = 4f, center = Offset(12f, 12f), style = Stroke(1.6f))
                }

                NavIconType.Profile -> {
                    drawCircle(tint, radius = 3.5f, center = Offset(12f, 8.5f), style = Stroke(1.6f))
                    val shoulders =
                        Path().apply {
                            moveTo(5f, 20f)
                            cubicTo(6f, 16.5f, 9f, 14.5f, 12f, 14.5f)
                            cubicTo(15f, 14.5f, 18f, 16.5f, 19f, 20f)
                        }
                    drawPath(
                        shoulders,
                        color = tint,
                        style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
