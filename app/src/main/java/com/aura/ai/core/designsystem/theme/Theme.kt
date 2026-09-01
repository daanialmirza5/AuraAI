package com.aura.ai.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AuraDarkColorScheme =
    darkColorScheme(
        primary = AuraColors.Accent,
        onPrimary = AuraColors.OnAccent,
        primaryContainer = AuraColors.AccentDeep,
        onPrimaryContainer = AuraColors.AccentLight,
        secondary = AuraColors.Accent2,
        onSecondary = AuraColors.OnAccent,
        secondaryContainer = AuraColors.AccentDeeper,
        onSecondaryContainer = AuraColors.AccentLight,
        tertiary = AuraColors.Success,
        onTertiary = AuraColors.OnAccent,
        error = AuraColors.Danger,
        onError = AuraColors.OnAccent,
        background = AuraColors.Background,
        onBackground = AuraColors.TextPrimary,
        surface = AuraColors.SurfaceGlass,
        onSurface = AuraColors.TextPrimary,
        surfaceVariant = AuraColors.DsBackground,
        onSurfaceVariant = AuraColors.TextPrimary.copy(alpha = 0.6f),
        outline = AuraColors.DsDivider,
        outlineVariant = AuraColors.DsDivider,
    )

/**
 * The Nocturne HUD is dark-only by design (readme.md: "a dark ground with a single
 * accent" — no light variant exists for the in-app screens; the Personalization screen's
 * "Light HUD" option is a design-time preview swatch, not a real second theme). This
 * wrapper always applies the dark scheme regardless of system theme, matching the source.
 */
@Composable
fun AuraAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuraDarkColorScheme,
        typography = AuraTypography,
        shapes = AuraShapes,
        content = content,
    )
}
