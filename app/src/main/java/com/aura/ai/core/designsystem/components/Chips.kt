@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles
import com.aura.ai.core.designsystem.theme.InterFontFamily

/**
 * The horizontally-scrolling sub-tab pill row used at the top of Aura / Workspace /
 * Automate / Profile — the `sub` selector that swaps the content beneath it.
 */
@Composable
fun <T> SubTabChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            SubTabChip(text = label(item), isSelected = item == selected, onClick = { onSelect(item) })
        }
    }
}

@Composable
fun SubTabChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bg = if (isSelected) AuraColors.Accent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (isSelected) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.55f)
    val border = if (isSelected) AuraColors.Accent.copy(alpha = 0.4f) else AuraColors.TextPrimary.copy(alpha = 0.1f)
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = AuraColors.Accent),
                    onClick = onClick,
                ).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = AuraTextStyles.micro, color = fg)
    }
}

/** A compact tonal tag, e.g. status labels ("ACTIVE", "LIMITED", HIGH/MED/LOW priority). */
@Composable
fun StatusTag(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        color = color,
        modifier = modifier,
    )
}
