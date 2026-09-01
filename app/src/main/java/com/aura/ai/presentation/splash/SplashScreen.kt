package com.aura.ai.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.ai.core.designsystem.components.GradientOrbCore
import com.aura.ai.core.designsystem.components.RotatingRing
import com.aura.ai.core.designsystem.components.ThinProgressBar
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.rememberBootProgress
import com.aura.ai.core.designsystem.theme.rememberFlicker
import com.aura.ai.core.designsystem.theme.rememberPulse

@Composable
fun SplashRoute(
    onFinished: (route: String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val nextRoute by viewModel.nextRoute.collectAsStateWithLifecycle()
    LaunchedEffect(nextRoute) {
        nextRoute?.let(onFinished)
    }
    SplashScreen()
}

@Composable
private fun SplashScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(AuraColors.BackgroundGlowTop, AuraColors.Background)))
                .drawWithCache {
                    val step = 42.dp.toPx()
                    onDrawBehind { drawGridOverlay(step) }
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SplashOrb()

            Text(
                text = "AURA",
                style = AuraTextStyles.splashWordmark,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(top = 34.dp),
            )
            Text(
                text = "AUTONOMOUS REASONING UNIT",
                style = AuraTextStyles.splashSubtitle,
                color = AuraColors.Accent.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 8.dp),
            )

            val boot by rememberBootProgress(2400)
            Box(modifier = Modifier.padding(top = 46.dp).width(180.dp)) {
                ThinProgressBar(progress = boot)
            }

            val flicker by rememberFlicker(2600)
            Text(
                text = "INITIALIZING NEURAL CORE…",
                style = AuraTextStyles.splashStatus,
                color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                modifier =
                    Modifier
                        .padding(top = 12.dp)
                        .graphicsLayer { alpha = flicker },
            )
        }
    }
}

@Composable
private fun SplashOrb() {
    Box(contentAlignment = Alignment.Center) {
        RotatingRing(size = 168.dp, durationMillis = 9000, color = AuraColors.Accent.copy(alpha = 0.35f))
        RotatingRing(
            size = 140.dp,
            durationMillis = 14000,
            reverse = true,
            dashed = true,
            color = AuraColors.Accent.copy(alpha = 0.3f),
        )
        GlowPuck(size = 112.dp)
        GradientOrbCore(
            size = 88.dp,
            stops = listOf(AuraColors.AccentLight, AuraColors.Accent, Color(0xFF453D78)),
            glowAlpha = 0.55f,
            glowSpread = 12.dp,
        )
    }
}

/** The blurred, pulsing middle glow layer unique to the Splash orb (inset 28 of 168). */
@Composable
private fun GlowPuck(size: Dp) {
    val (scaleState, alphaState) = rememberPulse(2400)
    Box(
        modifier =
            Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scaleState.value
                    scaleY = scaleState.value
                    alpha = alphaState.value
                }.drawWithCache {
                    val r = this.size.minDimension / 2f
                    val brush =
                        Brush.radialGradient(
                            colorStops =
                                arrayOf(
                                    0f to Color(0xE6B2ABFC),
                                    0.55f to Color(0x80675AC8),
                                    0.75f to Color.Transparent,
                                ),
                            center = Offset(this.size.width * 0.35f, this.size.height * 0.30f),
                            radius = r,
                        )
                    onDrawBehind { drawCircle(brush = brush, radius = r, center = center) }
                },
    )
}

private fun DrawScope.drawGridOverlay(step: Float) {
    val lineColor = AuraColors.Accent.copy(alpha = 0.05f)
    var x = 0f
    while (x < size.width) {
        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}
