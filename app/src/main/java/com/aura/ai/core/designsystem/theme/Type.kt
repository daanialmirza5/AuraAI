package com.aura.ai.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Nocturne loads Inter for both --font-heading (weight 500) and --font-body (weight 400)
 * from Google Fonts. Binary font assets can't be produced by this tool, so this resolves
 * to the platform sans-serif family (Roboto). To restore pixel-exact Inter: drop
 * inter_regular.ttf / inter_medium.ttf / inter_semibold.ttf / inter_bold.ttf into
 * res/font/ and swap the single line below for
 * `FontFamily(Font(R.font.inter_regular), Font(R.font.inter_medium, FontWeight.Medium), ...)`.
 */
val InterFontFamily: FontFamily = FontFamily.SansSerif

private val HeadingWeight = FontWeight.Medium // var(--font-heading-weight): 500

/** Named text styles lifted directly from the inline styles of each .dc.html screen. */
object AuraTextStyles {
    val splashWordmark =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            letterSpacing = 0.32.em,
        )
    val splashSubtitle =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.28.em,
        )
    val splashStatus =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.1.em,
        )
    val kicker =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            letterSpacing = 0.24.em,
        )
    val headingHero =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = HeadingWeight,
            fontSize = 26.sp,
            lineHeight = 32.5.sp,
            letterSpacing = (-0.01).em,
        )
    val headingXl =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = HeadingWeight,
            fontSize = 24.sp,
        )
    val headingLg =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = HeadingWeight,
            fontSize = 22.sp,
        )
    val headingMd =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = HeadingWeight,
            fontSize = 19.sp,
        )
    val sectionLabel =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 0.1.em,
        )
    val eyebrow =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.5.sp,
            letterSpacing = 0.1.em,
        )
    val cardTitle =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
        )
    val bodySmall =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    val body =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
    val bodyRelaxed =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.4.sp,
        )
    val label =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.02.em,
        )
    val caption =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
        )
    val micro =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
    val navLabel =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp,
            letterSpacing = 0.02.em,
        )
    val buttonLabel =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    val statusBarClock =
        TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.02.em,
        )
    val mono =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 11.5.sp,
            lineHeight = 18.4.sp,
        )
}

val AuraTypography =
    Typography(
        displayLarge = AuraTextStyles.headingHero,
        headlineLarge = AuraTextStyles.headingXl,
        headlineMedium = AuraTextStyles.headingLg,
        headlineSmall = AuraTextStyles.headingMd,
        titleLarge = AuraTextStyles.cardTitle,
        titleMedium = AuraTextStyles.bodySmall,
        titleSmall = AuraTextStyles.label,
        bodyLarge = AuraTextStyles.bodyRelaxed,
        bodyMedium = AuraTextStyles.body,
        bodySmall = AuraTextStyles.caption,
        labelLarge = AuraTextStyles.buttonLabel,
        labelMedium = AuraTextStyles.label,
        labelSmall = AuraTextStyles.navLabel,
    )
