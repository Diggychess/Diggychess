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
    val accentGreen: Color,       // Primary Gold (#F5C542 in dark)
    val accentGreenHover: Color,  // Highlighted Gold
    val accentGreenDark: Color,   // Pressed/Container Gold
    val studioCyan: Color,        // Semantic Info Blue (#4C9AFF)
    val onAccent: Color,          // Dark ink on gold buttons
    val textPrimary: Color,
    val textMuted: Color,         // Secondary text (#B8B8B8)
    val textDisabled: Color,      // Disabled text (#6B6B6B)
    val borderSubtle: Color,      // Border (#2E2E30)
    val lineSubtle: Color,
    val success: Color,           // #4CAF50
    val danger: Color             // #E5484D
)

// Diggy Chess — Modern HD Flat Dark Studio Palette
val DarkStudioPalette = KnightScopeColors(
    isDark = true,
    bg = Color(0xFF121212),
    bgRadialTop = Color(0xFF18181A),
    surface = Color(0xFF1C1C1E),
    card = Color(0xFF1C1C1E),
    cardElevated = Color(0xFF242426),
    accentGreen = Color(0xFFF5C542),
    accentGreenHover = Color(0xFFF5C542),
    accentGreenDark = Color(0xFFC99B1D),
    studioCyan = Color(0xFF4C9AFF),
    onAccent = Color(0xFF121212),
    textPrimary = Color(0xFFFFFFFF),
    textMuted = Color(0xFFB8B8B8),
    textDisabled = Color(0xFF6B6B6B),
    borderSubtle = Color(0xFF2E2E30),
    lineSubtle = Color(0xFF2E2E30),
    success = Color(0xFF4CAF50),
    danger = Color(0xFFE5484D)
)

// Diggy Chess — Clean High-Contrast Light Studio Palette (optional Settings toggle)
val LightClassicPalette = KnightScopeColors(
    isDark = false,
    bg = Color(0xFFF5F5F7),
    bgRadialTop = Color(0xFFEEEEF1),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF0F0F3),
    accentGreen = Color(0xFFD4A117),
    accentGreenHover = Color(0xFF9E7406),
    accentGreenDark = Color(0xFF8C6604),
    studioCyan = Color(0xFF1D6FE2),
    onAccent = Color(0xFF121212),
    textPrimary = Color(0xFF121212),
    textMuted = Color(0xFF52525B),
    textDisabled = Color(0xFF8E8E93),
    borderSubtle = Color(0xFFD8D8DD),
    lineSubtle = Color(0xFFE2E2E6),
    success = Color(0xFF2E7D32),
    danger = Color(0xFFD32F2F)
)

val LocalKnightScopeColors = staticCompositionLocalOf { DarkStudioPalette }

// Centralized Theme-Reactive Semantic Color Tokens
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
val TextDisabled: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.textDisabled
val BorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.borderSubtle
val LineSubtle: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.lineSubtle

val FeedbackCorrectGreen: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.success
val FeedbackDangerRed: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.danger

// Move Classification Semantic Colors (Accessible & Distinct)
val ClassBest = Color(0xFF4CAF50)
val ClassGood = Color(0xFF81C784)
val ClassInaccuracy = Color(0xFFF5C542)
val ClassMistake = Color(0xFFFF9800)
val ClassBlunder = Color(0xFFE5484D)
val ClassBrilliant = Color(0xFF26C2C9)
val ClassGreat = Color(0xFF4C9AFF)
val ClassBook = Color(0xFFA88865)
val ClassMiss = Color(0xFFE5484D)

// Board Square Highlights & Overlays (Chess.com / Lichess crisp translucent overlays)
val HighlightLastMove = Color(0x80F6F669)
val HighlightSelected = Color(0xA6F6F669)
val HighlightHint = Color(0x884C9AFF)
val HighlightCorrect = Color(0x994CAF50)
val HighlightWrong = Color(0x99E5484D)
val HighlightDragTarget = Color(0xB8FFFFFF)
val CheckGlow = Color(0xEBE5484D)
val ArrowBestGreen = Color(0xE64CAF50)
val ArrowSecondaryCyan = Color(0xCC4C9AFF)
val ArrowPlayedRed = Color(0xB8E5484D)

// Board Theme Palettes (Default: Gold/Classic #9C7B32 / #F3E7C8 matching Diggy Chess)
enum class BoardTheme(
    val id: String,
    val label: String,
    val lightSquare: Color,
    val darkSquare: Color
) {
    GOLD("gold", "Gold/Classic", Color(0xFFF3E7C8), Color(0xFF9C7B32)),
    GREEN("green", "Green/Cream", Color(0xFFEEEED2), Color(0xFF769656)),
    BLUE("blue", "Blue/Steel", Color(0xFFDEE4EC), Color(0xFF546E8A)),
    BROWN("brown", "Brown/Wood", Color(0xFFF0D9B5), Color(0xFFB58863));

    companion object {
        fun fromId(id: String): BoardTheme = when (id.lowercase()) {
            "green" -> GREEN
            "brown", "wood" -> BROWN
            "blue", "slate", "steel" -> BLUE
            "gold", "royal", "obsidian", "classic" -> GOLD
            else -> GOLD
        }
    }
}

enum class PieceStyle(
    val id: String,
    val label: String,
    val subtitle: String
) {
    CLASSIC_2D("classic_2d", "Classic 2D", "Clean flat crisp vectors"),
    NEO("neo", "Neo", "Soft shadow & subtle gloss"),
    THREED("3d", "3D", "Layered pedestal depth");

    companion object {
        fun fromId(id: String?): PieceStyle = entries.find { it.id == id } ?: NEO
    }
}
