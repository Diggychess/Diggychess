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

val DarkStudioPalette = KnightScopeColors(
    isDark = true,
    bg = Color(0xFF0C1014),
    bgRadialTop = Color(0xFF152230),
    surface = Color(0xFF131920),
    card = Color(0xFF182028),
    cardElevated = Color(0xFF1E2833),
    accentGreen = Color(0xFF22C55E),
    accentGreenHover = Color(0xFF4ADE80),
    accentGreenDark = Color(0xFF16A34A),
    studioCyan = Color(0xFF2DD4BF),
    onAccent = Color(0xFF071A0E),
    textPrimary = Color(0xFFF3F6FA),
    textMuted = Color(0xFF94A3B8),
    borderSubtle = Color(0xFF273240),
    lineSubtle = Color(0x18FFFFFF)
)

val LightClassicPalette = KnightScopeColors(
    isDark = false,
    bg = Color(0xFFF3F5F8),
    bgRadialTop = Color(0xFFE3ECF5),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF8FAFC),
    accentGreen = Color(0xFF16A34A),
    accentGreenHover = Color(0xFF15803D),
    accentGreenDark = Color(0xFF166534),
    studioCyan = Color(0xFF0D9488),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textMuted = Color(0xFF475569),
    borderSubtle = Color(0xFFD0DAE3),
    lineSubtle = Color(0x140F172A)
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

// Move Classification Semantic Colors (Chess.com + UnCappio Studio)
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
val HighlightLastMove = Color(0x8CF6F669)
val HighlightSelected = Color(0xC4F6F669)
val HighlightHint = Color(0x882DD4BF)
val HighlightCorrect = Color(0x9922C55E)
val HighlightWrong = Color(0x99DF534C)
val HighlightDragTarget = Color(0xAA4ADE80)
val CheckGlow = Color(0xEBDC2828)
val ArrowBestGreen = Color(0xE622C55E)
val ArrowSecondaryCyan = Color(0xBB2DD4BF)
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
    SLATE("slate", "Studio Blue", Color(0xFFDEE4EC), Color(0xFF5C728A)),
    OBSIDIAN("obsidian", "Midnight", Color(0xFFC5CFD9), Color(0xFF404C5C));

    companion object {
        fun fromId(id: String): BoardTheme = entries.find { it.id == id } ?: GREEN
    }
}
