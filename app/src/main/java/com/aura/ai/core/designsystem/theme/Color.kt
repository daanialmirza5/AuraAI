package com.aura.ai.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Every value here is transcribed verbatim from the Nocturne design source
 * (AURA AI.dc.html and its screen imports) — literal hex/rgba, not reinterpreted.
 * Alpha variants are applied at call sites via `.copy(alpha = ...)` using the
 * exact alpha the source used for that instance.
 */
object AuraColors {
    // ── Grounds ──────────────────────────────────────────────────────────
    val Background = Color(0xFF05060B)
    val BackgroundGlowTop = Color(0xFF171B33)
    val BackgroundGlowTopAlt = Color(0xFF141A33)
    val BackgroundDeep = Color(0xFF020204)
    val FrameEdgeTop = Color(0xFF1A1D2B)
    val FrameEdgeBottom = Color(0xFF08090F)
    val SurfaceGlass = Color(0xFF232532)
    val CodeSurface = Color(0xFF0A0B12)
    val SideButton = Color(0xFF2A2D3D)

    // ── Text ─────────────────────────────────────────────────────────────
    val TextPrimary = Color(0xFFE9E9ED)
    val OnAccent = Color(0xFF0C0D16)

    // ── Accent (brand blurple) ──────────────────────────────────────────
    val Accent = Color(0xFF9184D9)
    val AccentLight = Color(0xFFD2CEFD)
    val AccentLighter = Color(0xFFE3E0FF)
    val AccentDeep = Color(0xFF403876)
    val AccentDeeper = Color(0xFF362F5E)
    val AccentButtonEnd = Color(0xFF6F63B8)
    val Accent2 = Color(0xFFA7A1DB)

    // ── Status ───────────────────────────────────────────────────────────
    val Success = Color(0xFF8EE6C9)
    val Warning = Color(0xFFE0A15C)
    val Danger = Color(0xFFE08A8A)

    // ── Nocturne token ramp (design-system source of truth, styles.css) ──
    val DsBackground = Color(0xFF161826)
    val DsSurface = Color(0xFF232532)
    val DsDivider = Color(0x28E9E9ED) // color-mix(#e9e9ed 16%, transparent)
}
