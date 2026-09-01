@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraRadius
import com.aura.ai.core.designsystem.theme.AuraTextStyles

private fun Modifier.describedAs(description: String?): Modifier =
    if (description != null) this.semantics { contentDescription = description } else this

/** The primary pill CTA used for Continue / Get Started / Activate AURA. */
@Composable
fun AuraGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
                .drawBehind {
                    drawRoundRect(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(AuraColors.Accent.copy(alpha = 0.35f), Color.Transparent),
                                radius = size.maxDimension,
                            ),
                        cornerRadius = CornerRadius(26.dp.toPx()),
                    )
                }.clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(AuraColors.Accent, AuraColors.AccentButtonEnd)))
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = AuraColors.OnAccent),
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon?.invoke()
            Text(text = text, style = AuraTextStyles.buttonLabel, color = AuraColors.OnAccent)
        }
    }
}

/** A translucent glass square-ish icon button, e.g. the notification bell on Home. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    cornerRadius: Dp = AuraRadius.xl,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(size)
                .describedAs(contentDescription)
                .clip(RoundedCornerShape(cornerRadius))
                .background(AuraColors.SurfaceGlass.copy(alpha = 0.7f))
                .drawBehind {
                    drawRoundRect(
                        color = AuraColors.Accent.copy(alpha = 0.2f),
                        cornerRadius = CornerRadius(cornerRadius.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }.clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = AuraColors.Accent),
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A small square glass back-button (32dp visible, 48dp touch target) used atop sub-screens
 *  like Notification Summary. */
@Composable
fun GlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Back",
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(32.dp)
                .describedAs(contentDescription)
                .clip(RoundedCornerShape(10.dp))
                .background(AuraColors.SurfaceGlass.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = AuraColors.Accent),
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The circular gradient send button in the chat composer. */
@Composable
fun AccentCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(size)
                .describedAs(contentDescription)
                .clip(CircleShape)
                .background(AuraColors.Accent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = AuraColors.OnAccent),
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) { content() }
}
