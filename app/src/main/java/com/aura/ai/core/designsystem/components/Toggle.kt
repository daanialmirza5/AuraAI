@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // rememberRipple: no drop-in replacement exists in the pinned Compose BOM yet — see docs/PLATFORM_REVIEW.md.

package com.aura.ai.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aura.ai.core.designsystem.theme.AuraColors

/** The 44x26 pill switch used throughout Permissions / Automations / Smart Home. */
@Composable
fun AuraToggleSwitch(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val trackColor by animateColorAsState(
        targetValue = if (checked) AuraColors.Accent else AuraColors.TextPrimary.copy(alpha = 0.15f),
        animationSpec = tween(200),
        label = "toggleTrack",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = tween(200),
        label = "toggleKnob",
    )
    Box(
        modifier =
            modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(trackColor)
                .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
                .toggleable(
                    value = checked,
                    onValueChange = { onCheckedChange() },
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = AuraColors.Accent),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 2.dp)
                    .offset(x = knobOffset)
                    .size(22.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Color.White),
        )
    }
}
