@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.presentation.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.ai.core.designsystem.components.GradientOrbCore
import com.aura.ai.core.designsystem.components.RotatingRing
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.InterFontFamily
import com.aura.ai.core.security.BiometricAuthenticator

@Composable
fun LoginRoute(
    onAuthenticated: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.authenticated) {
        if (uiState.authenticated) onAuthenticated()
    }

    val activity = LocalContext.current as FragmentActivity
    val authenticator = remember(activity) { BiometricAuthenticator(activity) }

    val requestAuth: () -> Unit = {
        viewModel.onAuthenticationStarted()
        if (authenticator.isAvailable()) {
            authenticator.authenticate(
                onSuccess = viewModel::onAuthenticationSucceeded,
                onError = viewModel::onAuthenticationError,
                onFailed = viewModel::onAuthenticationFailed,
            )
        } else {
            // No enrolled biometric/device credential on this hardware — don't strand the user.
            viewModel.onAuthenticationSucceeded()
        }
    }

    LoginScreen(uiState = uiState, onRequestAuth = requestAuth)
}

@Composable
private fun LoginScreen(
    uiState: LoginUiState,
    onRequestAuth: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(AuraColors.BackgroundGlowTop, AuraColors.Background)))
                .padding(horizontal = 28.dp)
                .padding(top = 96.dp, bottom = 48.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
            ) {
                GradientOrbCore(
                    size = 76.dp,
                    glowAlpha = 0.5f,
                    glowSpread = 20.dp,
                    stops = listOf(AuraColors.AccentLight, AuraColors.Accent, Color(0xFF403876)),
                )
                Text(
                    text = "Authenticate",
                    style = AuraTextStyles.headingXl,
                    color = AuraColors.TextPrimary,
                    modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
                )
                Text(
                    text = "Biometric handshake required to link AURA to this device",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = AuraColors.TextPrimary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                CredentialCard(label = "EMAIL", value = "operator@aura.ai")
                Spacer(modifier = Modifier.height(16.dp))
                CredentialCard(label = "PASSCODE", value = "••••••••", letterSpaced = true)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(color = AuraColors.Accent),
                                onClick = onRequestAuth,
                            ).padding(vertical = 12.dp),
                ) {
                    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        RotatingRing(size = 72.dp, durationMillis = 6000, strokeWidthDp = 2f)
                        FingerprintGlyph(modifier = Modifier.size(30.dp))
                    }
                    Text(
                        text = if (uiState.isAuthenticating) "VERIFYING…" else "TOUCH TO VERIFY",
                        style = AuraTextStyles.micro,
                        color = AuraColors.Accent,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    uiState.errorMessage?.let {
                        Text(
                            text = it,
                            style = AuraTextStyles.caption,
                            color = AuraColors.Danger,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            Text(
                text = "Use passcode instead",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = AuraColors.TextPrimary.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(color = AuraColors.TextPrimary),
                            onClick = onRequestAuth,
                        ),
            )
        }
    }
}

@Composable
private fun CredentialCard(
    label: String,
    value: String,
    letterSpaced: Boolean = false,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AuraColors.SurfaceGlass.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontSize = 11.sp,
            color = AuraColors.TextPrimary.copy(alpha = 0.45f),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = value,
            style = AuraTextStyles.body,
            color = AuraColors.TextPrimary,
            letterSpacing = if (letterSpaced) 0.3.em else TextUnit.Unspecified,
        )
    }
}

@Composable
private fun FingerprintGlyph(modifier: Modifier = Modifier) {
    val tint = AuraColors.Accent
    Canvas(modifier = modifier) {
        val factor = size.width / 24f
        scale(scaleX = factor, scaleY = factor, pivot = Offset.Zero) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(7f, 2f),
                size = Size(10f, 8.5f),
                cornerRadius = CornerRadius(5f, 5f),
                style = Stroke(1.6f),
            )
            // Bottom semicircle: Compose sweep angles go clockwise from 0=east, so
            // 90=south — sweeping from 180 (west) by -180 passes through south, not north.
            drawArc(
                color = tint,
                startAngle = 180f,
                sweepAngle = -180f,
                useCenter = false,
                topLeft = Offset(4.5f, 4.5f),
                size = Size(15f, 15f),
                style = Stroke(width = 1.6f, cap = StrokeCap.Round),
            )
            drawLine(
                color = tint,
                start = Offset(12f, 19.5f),
                end = Offset(12f, 22f),
                strokeWidth = 1.6f,
                cap = StrokeCap.Round,
            )
        }
    }
}
