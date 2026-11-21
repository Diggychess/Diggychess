package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class KnightScopeColors(
    val isDark: Boolean,
    val bg: Color,
    val bgRadialTop: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val accentGreen: Color,
    val accentGreenHover: Color,
    val accentGreenDark: Color,
    val studioCyan: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val lineSubtle: Color
)

// Diggy Chess — Royal Gold & Obsidian Dark Luxury Palette
val DarkStudioPalette = KnightScopeColors(
    isDark = true,
    bg = Color(0xFF080A0F),
    bgRadialTop = Color(0xFF211A0C),
    surface = Color(0xFF10131C),
    card = Color(0xFF151924),
    cardElevated = Color(0xFF1D2231),
    accentGreen = Color(0xFFE2B93B),
    accentGreenHover = Color(0xFFF6D878),
    accentGreenDark = Color(0xFFB6891B),
    studioCyan = Color(0xFF38C6B6),
    onAccent = Color(0xFF140E02),
    textPrimary = Color(0xFFF6F2E9),
    textMuted = Color(0xFF98A2B3),
    borderSubtle = Color(0xFF282E3E),
    lineSubtle = Color(0x2AE2B93B)
)

// Diggy Chess — Alabaster & Antique Gold Light Palette
val LightClassicPalette = KnightScopeColors(
    isDark = false,
    bg = Color(0xFFF6F3EB),
    bgRadialTop = Color(0xFFEDE4CE),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFFAF7F0),
    accentGreen = Color(0xFFB38314),
    accentGreenHover = Color(0xFF8C6306),
    accentGreenDark = Color(0xFF735004),
    studioCyan = Color(0xFF0F766E),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF12141A),
    textMuted = Color(0xFF525866),
    borderSubtle = Color(0xFFDCD3BE),
    lineSubtle = Color(0x28B38314)
)

val LocalKnightScopeColors = staticCompositionLocalOf { DarkStudioPalette }

// Theme-reactive semantic color tokens
val BgDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.bg
val BgRadialTop: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.bgRadialTop
val SurfaceDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.surface
val CardDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.card
val CardSurfaceElevated: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.cardElevated

val AccentGreen: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.accentGreen
val AccentGreenHover: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.accentGreenHover
val AccentGreenDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.accentGreenDark
val StudioCyan: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.studioCyan
val OnAccentDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.onAccent

val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.textPrimary
val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.textMuted
val BorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.borderSubtle
val LineSubtle: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.lineSubtle

// Semantic Positive Feedback Emerald (distinct from Royal Gold UI chrome)
val FeedbackCorrectGreen = Color(0xFF16A34A)

// Move Classification Semantic Colors
val ClassBest = Color(0xFF81B64C)
val ClassGood = Color(0xFF96AF5B)
val ClassInaccuracy = Color(0xFFF0C15A)
val ClassMistake = Color(0xFFE69138)
val ClassBlunder = Color(0xFFDF534C)
val ClassBrilliant = Color(0xFF26C2C9)
val ClassGreat = Color(0xFF5C8BB0)
val ClassBook = Color(0xFFA88865)
val ClassMiss = Color(0xFFD96B66)

// Board Square Highlights & Overlays
val HighlightLastMove = Color(0x8CE2B93B)
val HighlightSelected = Color(0xC4F6D878)
val HighlightHint = Color(0x8838C6B6)
val HighlightCorrect = Color(0x9922C55E)
val HighlightWrong = Color(0x99DF534C)
val HighlightDragTarget = Color(0xAA4ADE80)
val CheckGlow = Color(0xEBDC2828)
val ArrowBestGreen = Color(0xE622C55E)
val ArrowSecondaryCyan = Color(0xBB38C6B6)
val ArrowPlayedRed = Color(0xA6DF534C)

// Studio Board Theme Palettes
enum class BoardTheme(
    val id: String,
    val label: String,
    val lightSquare: Color,
    val darkSquare: Color
) {
    GREEN("green", "Tournament Green", Color(0xFFEEEED2), Color(0xFF769656)),
    WOOD("wood", "Classic Wood", Color(0xFFF0D9B5), Color(0xFFB58863)),
    ROYAL("royal", "Royal Gold", Color(0xFFF2E6CE), Color(0xFF8A6B32)),
    SLATE("slate", "Studio Blue", Color(0xFFDEE4EC), Color(0xFF5C728A)),
    OBSIDIAN("obsidian", "Midnight", Color(0xFFC5CFD9), Color(0xFF3E4959));

    companion object {
        fun fromId(id: String): BoardTheme = entries.find { it.id == id } ?: ROYAL
    }
}
