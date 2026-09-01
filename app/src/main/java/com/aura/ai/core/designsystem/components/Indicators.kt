package com.aura.ai.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.rememberEqualizerBarScale

/** Onboarding's step dots — the active one stretches into a short pill. */
@Composable
fun SegmentDots(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(total) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 22.dp else 6.dp, tween(300), label = "dotWidth")
            val color by animateColorAsState(
                if (active) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.2f),
                tween(300),
                label = "dotColor",
            )
            Box(
                modifier =
                    Modifier
                        .width(width)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
            )
        }
    }
}

/** The 9-bar voice equalizer beneath the orb — flat when idle, alive otherwise. */
@Composable
fun EqualizerBars(
    active: Boolean,
    modifier: Modifier = Modifier,
    count: Int = 9,
) {
    Row(
        modifier = modifier.height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(count) { index ->
            val scale = rememberEqualizerBarScale(index, active)
            Box(
                modifier =
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .scaleY(scale.value)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AuraColors.Accent),
            )
        }
    }
}

private fun Modifier.scaleY(scale: Float): Modifier = this.scale(scaleX = 1f, scaleY = scale)

/** A thin fill bar — the splash boot progress, memory-usage bar, storage bar. */
@Composable
fun ThinProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = AuraColors.TextPrimary.copy(alpha = 0.12f),
    brush: Brush = Brush.horizontalGradient(listOf(AuraColors.Accent, AuraColors.AccentLight)),
    height: Dp = 6.dp,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(trackColor),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(brush),
        )
    }
}

/** A ring gauge — Device Health score, matching `conic-gradient`. */
@Composable
fun CircularGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 14.dp,
    activeColor: Color = AuraColors.Success,
    trackColor: Color = AuraColors.TextPrimary.copy(alpha = 0.1f),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke,
            )
        }
        content()
    }
}
