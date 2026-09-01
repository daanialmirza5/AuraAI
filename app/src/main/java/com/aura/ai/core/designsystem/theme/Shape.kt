package com.aura.ai.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radii used across the screens — the design leans on a handful of concrete values
 *  rather than the abstract --radius-sm/md/lg scale (those back the web component
 *  library, not the phone-frame screens themselves). */
object AuraRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 14.dp
    val xl = 16.dp
    val xxl = 18.dp
    val xxxl = 22.dp
    val pill = 26.dp
    val Circle = CircleShape
}

object AuraShapesTokens {
    val xs = RoundedCornerShape(AuraRadius.xs)
    val sm = RoundedCornerShape(AuraRadius.sm)
    val md = RoundedCornerShape(AuraRadius.md)
    val lg = RoundedCornerShape(AuraRadius.lg)
    val xl = RoundedCornerShape(AuraRadius.xl)
    val xxl = RoundedCornerShape(AuraRadius.xxl)
    val xxxl = RoundedCornerShape(AuraRadius.xxxl)
    val pill = RoundedCornerShape(AuraRadius.pill)
    val circle = CircleShape
}

val AuraShapes =
    Shapes(
        extraSmall = AuraShapesTokens.xs,
        small = AuraShapesTokens.sm,
        medium = AuraShapesTokens.md,
        large = AuraShapesTokens.xl,
        extraLarge = AuraShapesTokens.xxxl,
    )
