@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraRadius
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.InterFontFamily

/** The translucent `rgba(35,37,50,alpha)` panel that backs almost every list row and card. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = AuraRadius.xl,
    fillAlpha: Float = 0.5f,
    borderColor: Color = AuraColors.TextPrimary.copy(alpha = 0.06f),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(AuraColors.SurfaceGlass.copy(alpha = fillAlpha))
                .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = rememberRipple(color = AuraColors.Accent),
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
    ) { content() }
}

/** A 2-up stat tile, e.g. Home's Systems grid or Profile's session/memory/device counters. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AuraColors.TextPrimary,
    valueFontSize: TextUnit = 20.sp,
    onClick: (() -> Unit)? = null,
) {
    GlassSurface(modifier = modifier, cornerRadius = AuraRadius.xl, onClick = onClick) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = AuraTextStyles.eyebrow,
                color = AuraColors.TextPrimary.copy(alpha = 0.5f),
            )
            Text(
                text = value,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = valueFontSize,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** The small uppercase "Daily Timeline" / "Systems" section headers. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = AuraTextStyles.sectionLabel,
        color = AuraColors.TextPrimary.copy(alpha = 0.4f),
        modifier = modifier,
    )
}
