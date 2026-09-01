package com.aura.ai.presentation.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.aura.ai.core.designsystem.theme.AuraColors

/** The 4 onboarding glyphs, redrawn from their source 52x52 SVGs. */
enum class OnboardingArt { Presence, Memory, Control, Trust }

@Composable
fun OnboardingArtGlyph(
    art: OnboardingArt,
    modifier: Modifier = Modifier,
) {
    val tint = AuraColors.Accent
    Canvas(modifier = modifier) {
        val factor = size.width / 52f
        scale(scaleX = factor, scaleY = factor, pivot = Offset.Zero) {
            when (art) {
                OnboardingArt.Presence -> {
                    drawCircle(tint, radius = 10f, center = Offset(26f, 26f))
                    drawCircle(tint.copy(alpha = 0.5f), radius = 18f, center = Offset(26f, 26f), style = Stroke(1f))
                    drawCircle(tint.copy(alpha = 0.25f), radius = 24f, center = Offset(26f, 26f), style = Stroke(1f))
                }

                OnboardingArt.Memory -> {
                    val ticks =
                        Path().apply {
                            moveTo(26f, 8f)
                            lineTo(26f, 16f)
                            moveTo(26f, 36f)
                            lineTo(26f, 44f)
                            moveTo(8f, 26f)
                            lineTo(16f, 26f)
                            moveTo(36f, 26f)
                            lineTo(44f, 26f)
                        }
                    drawPath(ticks, tint, style = Stroke(width = 2f, cap = StrokeCap.Round))
                    drawCircle(tint, radius = 9f, center = Offset(26f, 26f), style = Stroke(2f))
                }

                OnboardingArt.Control -> {
                    val cells =
                        listOf(
                            Offset(10f, 10f) to 1f,
                            Offset(28f, 10f) to 0.6f,
                            Offset(10f, 28f) to 0.6f,
                            Offset(28f, 28f) to 1f,
                        )
                    cells.forEach { (topLeft, alpha) ->
                        drawRoundRect(
                            color = tint.copy(alpha = alpha),
                            topLeft = topLeft,
                            size = Size(14f, 14f),
                            cornerRadius = CornerRadius(3f, 3f),
                            style = Stroke(2f),
                        )
                    }
                }

                OnboardingArt.Trust -> {
                    val shield =
                        Path().apply {
                            moveTo(26f, 6f)
                            lineTo(42f, 12f)
                            lineTo(42f, 25f)
                            cubicTo(42f, 37f, 34f, 43f, 26f, 46f)
                            cubicTo(18f, 43f, 10f, 37f, 10f, 25f)
                            lineTo(10f, 12f)
                            close()
                        }
                    drawPath(
                        shield,
                        tint,
                        style = Stroke(width = 2f, join = StrokeJoin.Round, cap = StrokeCap.Round),
                    )
                    val check =
                        Path().apply {
                            moveTo(19f, 26f)
                            lineTo(24f, 31f)
                            lineTo(33f, 20f)
                        }
                    drawPath(
                        check,
                        tint,
                        style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
