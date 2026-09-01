package com.aura.ai.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.rememberPulse
import com.aura.ai.core.designsystem.theme.rememberRotation

/**
 * `radial-gradient(circle at 35% 30%, ...)` core sphere used for the wordmark orb across
 * Splash / Login / Home / Onboarding / Profile — plus the soft ambient glow the CSS drew
 * via `box-shadow: 0 0 Npx Mpx rgba(accent, a)`. The glow is painted as layered radial
 * gradients rather than a blurred shadow so it renders identically on every API level
 * (Modifier.blur only hardware-accelerates on API 31+).
 */
@Composable
fun GradientOrbCore(
    modifier: Modifier = Modifier,
    size: Dp,
    stops: List<Color> = listOf(AuraColors.AccentLight, AuraColors.Accent, AuraColors.AccentDeep),
    glowColor: Color = AuraColors.Accent,
    glowAlpha: Float = 0.5f,
    glowSpread: Dp = size * 0.4f,
    pulseDurationMillis: Int? = null,
) {
    val pulse = pulseDurationMillis?.let { rememberPulse(it) }
    val scale = pulse?.first?.value ?: 1f
    val animatedGlowAlpha = pulse?.second?.value?.let { it * glowAlpha } ?: glowAlpha

    Box(
        modifier =
            modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.drawWithCache {
                    val r = this.size.minDimension / 2f
                    val glowR = r + glowSpread.toPx()
                    val glowBrush =
                        Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = animatedGlowAlpha), Color.Transparent),
                            center = Offset(this.size.width / 2f, this.size.height / 2f),
                            radius = glowR,
                        )
                    val coreBrush =
                        Brush.radialGradient(
                            colors = stops,
                            center = Offset(this.size.width * 0.35f, this.size.height * 0.30f),
                            radius = r * 1.05f,
                        )
                    onDrawBehind {
                        drawCircle(brush = glowBrush, radius = glowR, center = center)
                        drawCircle(brush = coreBrush, radius = r, center = center)
                    }
                },
    )
}

/** A single continuously-rotating ring — solid or dashed — used behind every orb. */
@Composable
fun RotatingRing(
    modifier: Modifier = Modifier,
    size: Dp,
    color: Color = AuraColors.Accent.copy(alpha = 0.35f),
    strokeWidthDp: Float = 1f,
    durationMillis: Int,
    reverse: Boolean = false,
    dashed: Boolean = false,
) {
    val rotation = rememberRotation(durationMillis, reverse)
    Box(
        modifier =
            modifier
                .size(size)
                .graphicsLayer { rotationZ = rotation.value }
                .drawWithCache {
                    val stroke =
                        Stroke(
                            width = strokeWidthDp.dp2px(this),
                            pathEffect =
                                if (dashed) {
                                    PathEffect.dashPathEffect(floatArrayOf(6f.dp2px(this), 5f.dp2px(this)))
                                } else {
                                    null
                                },
                        )
                    onDrawBehind {
                        drawCircle(color = color, style = stroke)
                    }
                },
    )
}

private fun Float.dp2px(scope: androidx.compose.ui.draw.CacheDrawScope): Float = with(scope) { this@dp2px.dp.toPx() }

/** `auraScan` — a soft highlight band sweeping vertically through a clipped circle. */
@Composable
fun ScanSweep(
    modifier: Modifier = Modifier,
    size: Dp,
    durationMillis: Int,
    color: Color = AuraColors.AccentLight,
) {
    val progress =
        com.aura.ai.core.designsystem.theme
            .rememberScanProgress(durationMillis)
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .drawWithCache {
                    val bandHeight = this.size.height * 0.4f
                    onDrawBehind {
                        val centerY = (progress.value * 0.5f + 0.5f) * (this.size.height + bandHeight) - bandHeight
                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, color.copy(alpha = 0.35f), Color.Transparent),
                                    startY = centerY,
                                    endY = centerY + bandHeight,
                                ),
                            topLeft = Offset(0f, centerY),
                            size =
                                androidx.compose.ui.geometry
                                    .Size(this.size.width, bandHeight),
                        )
                    }
                },
    )
}

/** The layered rings + glow + core used for AURA's primary presence (Splash, Aura tab). */
@Composable
fun AuraOrbStage(
    modifier: Modifier = Modifier,
    size: Dp,
    showDashedRing: Boolean = true,
    showScan: Boolean = false,
    ringDuration: Int = 12000,
    ringReverseDuration: Int = 16000,
    scanDuration: Int = 5000,
    pulseDuration: Int = 3400,
    glowAlpha: Float = 0.4f,
) {
    Box(modifier = modifier.size(size), contentAlignment = androidx.compose.ui.Alignment.Center) {
        RotatingRing(size = size, durationMillis = ringDuration, strokeWidthDp = 1f)
        if (showDashedRing) {
            RotatingRing(
                size = size * (1f - 2 * 0.082f),
                durationMillis = ringReverseDuration,
                reverse = true,
                dashed = true,
                color = AuraColors.Accent.copy(alpha = 0.22f),
            )
        }
        if (showScan) {
            ScanSweep(size = size * (1f - 2 * 0.164f), durationMillis = scanDuration)
        }
        GradientOrbCore(
            size = size * 0.545f,
            stops = listOf(AuraColors.AccentLighter, AuraColors.Accent, AuraColors.AccentDeeper),
            glowAlpha = glowAlpha,
            glowSpread = size * 0.16f,
            pulseDurationMillis = pulseDuration,
        )
    }
}
