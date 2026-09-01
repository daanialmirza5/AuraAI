@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.core.designsystem.theme.AuraTextStyles

data class BottomNavEntry(
    val icon: NavIconType,
    val label: String,
    val route: String,
)

private val NavAccent = AuraColors.Accent
private val NavDim = AuraColors.TextPrimary.copy(alpha = 0.42f)

/** The frosted pill nav bar anchored to the bottom of the app shell, 5 destinations. */
@Composable
fun AuraBottomNavBar(
    items: List<BottomNavEntry>,
    selectedRoute: String,
    onSelect: (BottomNavEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF141623).copy(alpha = 0.86f))
                    .border(1.dp, AuraColors.Accent.copy(alpha = 0.22f), RoundedCornerShape(26.dp))
                    .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            items.forEach { entry ->
                val selected = entry.route == selectedRoute
                val tint = if (selected) NavAccent else NavDim
                val interactionSource = remember { MutableInteractionSource() }
                Column(
                    modifier =
                        Modifier
                            .widthIn(min = 44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = rememberRipple(color = AuraColors.Accent),
                                onClick = { onSelect(entry) },
                            ).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    NavGlyph(type = entry.icon, tint = tint, modifier = Modifier.size(22.dp))
                    Text(text = entry.label, style = AuraTextStyles.navLabel, color = tint)
                }
            }
        }
    }
}
