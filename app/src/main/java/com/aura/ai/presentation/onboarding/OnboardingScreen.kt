@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.ai.core.designsystem.components.AuraGradientButton
import com.aura.ai.core.designsystem.components.SegmentDots
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.rememberFloatBob

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    LaunchedEffect(finished) {
        if (finished) onFinished()
    }
    OnboardingScreen(uiState = uiState, onNext = viewModel::onNext, onSkip = viewModel::onSkip)
}

@Composable
private fun OnboardingScreen(
    uiState: OnboardingUiState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(AuraColors.BackgroundGlowTop, AuraColors.Background))),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 64.dp, end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                Text(
                    text = "SKIP",
                    style = AuraTextStyles.label,
                    color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                    modifier =
                        Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = rememberRipple(color = AuraColors.TextPrimary),
                                onClick = onSkip,
                            ).padding(horizontal = 6.dp, vertical = 12.dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FloatingArtBadge(art = uiState.current.art)

                Text(
                    text = uiState.current.kicker.uppercase(),
                    style = AuraTextStyles.kicker,
                    color = AuraColors.Accent,
                    modifier = Modifier.padding(top = 32.dp, bottom = 14.dp),
                )

                AnimatedContent(
                    targetState = uiState.step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboardingTitle",
                ) { step ->
                    Text(
                        text = uiState.slides[step].title,
                        style = AuraTextStyles.headingHero,
                        color = AuraColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                }

                Text(
                    text = uiState.current.body,
                    style = AuraTextStyles.body,
                    color = AuraColors.TextPrimary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp).widthIn(max = 280.dp),
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 56.dp)) {
                SegmentDots(
                    total = uiState.slides.size,
                    current = uiState.step,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp),
                )
                AuraGradientButton(text = uiState.ctaLabel, onClick = onNext)
            }
        }
    }
}

@Composable
private fun FloatingArtBadge(art: OnboardingArt) {
    val bob by rememberFloatBob(5000)
    Box(
        modifier =
            Modifier
                .size(120.dp)
                .drawWithCache {
                    val brush =
                        Brush.radialGradient(
                            colorStops =
                                arrayOf(
                                    0f to Color(0x47B2ABFC),
                                    0.7f to Color(0x0D9184D9),
                                    1f to Color.Transparent,
                                ),
                            center = Offset(this.size.width * 0.35f, this.size.height * 0.30f),
                            radius = this.size.minDimension * 0.75f,
                        )
                    onDrawBehind { drawCircle(brush = brush, radius = this.size.minDimension / 2f, center = center) }
                },
        contentAlignment = Alignment.Center,
    ) {
        OnboardingArtGlyph(
            art = art,
            modifier = Modifier.size(52.dp).offset(y = bob.dp),
        )
    }
}
