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
    val accentGreen: Color,       // Signature Chess.com Green (#81B64C)
    val accentGreenHover: Color,  // Bright Emerald highlight (#96CB5F)
    val accentGreenDark: Color,   // Tactile 3D button bottom lip (#537A30)
    val studioCyan: Color,        // Coach Blue (#4E9AF1)
    val trophyGold: Color,        // Crown / Diamond Gold (#F5C542)
    val onAccent: Color,          // High-contrast ink on green buttons
    val textPrimary: Color,
    val textMuted: Color,         // Secondary text
    val textDisabled: Color,      // Disabled text
    val borderSubtle: Color,      // Hairline border
    val lineSubtle: Color,
    val success: Color,           // #81B64C
    val danger: Color,            // Error / Blunder red
    val lightGreen: Color = Color(0xFF283520),
    val softGrey: Color = Color(0xFF383531),
    val lightBlue: Color = Color(0xFF233040),
    val matteDark: Color = Color(0xFF211F1C),
    val matteDarkSecondary: Color = Color(0xFF2E2B28),
    val onMatteDark: Color = Color(0xFFFFFFFF),
    val onMatteDarkMuted: Color = Color(0xFFB3B0AA),
    val matteDarkBorder: Color = Color(0xFF43403B),
    val inputBg: Color = Color(0xFF1E1C19),
    val inputText: Color = Color(0xFFFFFFFF),
    val inputHint: Color = Color(0xFF9A968F),
    val inputBorder: Color = Color(0xFF48443F)
)

// Signature Chess.com Warm Charcoal Dark Palette (Default)
val DarkStudioPalette = KnightScopeColors(
    isDark = true,
    bg = Color(0xFF302E2B),
    bgRadialTop = Color(0xFF383531),
    surface = Color(0xFF262421),
    card = Color(0xFF262421),
    cardElevated = Color(0xFF36332E),
    accentGreen = Color(0xFF81B64C),
    accentGreenHover = Color(0xFF96CB5F),
    accentGreenDark = Color(0xFF537A30),
    studioCyan = Color(0xFF4E9AF1),
    trophyGold = Color(0xFFF5C542),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textMuted = Color(0xFFB3B0AA),
    textDisabled = Color(0xFF7A7771),
    borderSubtle = Color(0xFF43403B),
    lineSubtle = Color(0xFF3B3833),
    success = Color(0xFF81B64C),
    danger = Color(0xFFEA4D44),
    lightGreen = Color(0xFF283621),
    softGrey = Color(0xFF36332E),
    lightBlue = Color(0xFF233245),
    matteDark = Color(0xFF211F1C),
    matteDarkSecondary = Color(0xFF2E2B28),
    onMatteDark = Color(0xFFFFFFFF),
    onMatteDarkMuted = Color(0xFFB3B0AA),
    matteDarkBorder = Color(0xFF43403B),
    inputBg = Color(0xFF1E1C19),
    inputText = Color(0xFFFFFFFF),
    inputHint = Color(0xFF9A968F),
    inputBorder = Color(0xFF48443F)
)

// Optional Light Classic Palette (Settings Theme Toggle)
val LightClassicPalette = KnightScopeColors(
    isDark = false,
    bg = Color(0xFFF5F7F5),
    bgRadialTop = Color(0xFFF5F7F5),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF0F2EF),
    accentGreen = Color(0xFF81B64C),
    accentGreenHover = Color(0xFF93C45E),
    accentGreenDark = Color(0xFF537A30),
    studioCyan = Color(0xFF2563EB),
    trophyGold = Color(0xFFD97706),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF25332B),
    textMuted = Color(0xFF526157),
    textDisabled = Color(0xFF8A998F),
    borderSubtle = Color(0xFFE2E6E1),
    lineSubtle = Color(0xFFE2E6E1),
    success = Color(0xFF81B64C),
    danger = Color(0xFFDC2626),
    lightGreen = Color(0xFFE8F0DF),
    softGrey = Color(0xFFF0F2EF),
    lightBlue = Color(0xFFEAF1F7),
    matteDark = Color(0xFFFFFFFF),
    matteDarkSecondary = Color(0xFFE8F0DF),
    onMatteDark = Color(0xFF25332B),
    onMatteDarkMuted = Color(0xFF526157),
    matteDarkBorder = Color(0xFFD6E4C8),
    inputBg = Color(0xFFFFFFFF),
    inputText = Color(0xFF25332B),
    inputHint = Color(0xFF76847B),
    inputBorder = Color(0xFFD2D8CF)
)

val LocalKnightScopeColors = staticCompositionLocalOf { DarkStudioPalette }

// Centralized Theme-Reactive Semantic Color Tokens
val BgDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.bg
val BgRadialTop: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.bgRadialTop
val SurfaceDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.surface
val CardDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.card
val CardSurfaceElevated: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.cardElevated
val LightGreenSurface: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.lightGreen
val SoftGreySurface: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.softGrey
val LightBlueSurface: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.lightBlue

val AccentGreen: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.accentGreen
val AccentGreenHover: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.accentGreenHover
val AccentGreenDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.accentGreenDark
val StudioCyan: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.studioCyan
val TrophyGold: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.trophyGold
val OnAccentDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.onAccent

val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.textPrimary
val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.textMuted
val TextDisabled: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.textDisabled
val BorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.borderSubtle
val LineSubtle: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.lineSubtle

val FeedbackCorrectGreen: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.success
val FeedbackDangerRed: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.danger

val MatteDarkCard: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.matteDark
val MatteDarkSecondary: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.matteDarkSecondary
val OnMatteDark: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.onMatteDark
val OnMatteDarkMuted: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.onMatteDarkMuted
val MatteDarkBorder: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.matteDarkBorder

val InputBg: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.inputBg
val InputText: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.inputText
val InputHint: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.inputHint
val InputBorder: Color @Composable @ReadOnlyComposable get() = LocalKnightScopeColors.current.inputBorder

// Move Classification Semantic Colors (Chess.com-inspired high-contrast tokens)
val ClassBest = Color(0xFF81B64C)
val ClassExcellent = Color(0xFF74B05A)
val ClassGood = Color(0xFF96BC4B)
val ClassInaccuracy = Color(0xFFF7C631)
val ClassMistake = Color(0xFFE6912C)
val ClassBlunder = Color(0xFFCA3431)
val ClassBrilliant = Color(0xFF26C2C9)
val ClassGreat = Color(0xFF5C8BB0)
val ClassBook = Color(0xFFA88865)
val ClassMiss = Color(0xFFFF7769)

// Board Square Highlights & Overlays
val HighlightLastMove = Color(0x66FFFF00)
val HighlightSelected = Color(0xB3BACB2B)
val HighlightHint = Color(0x884C9AFF)
val HighlightCorrect = Color(0x9981B64C)
val HighlightWrong = Color(0x99FA412D)
val HighlightDragTarget = Color(0xCCFFFFFF)
val HighlightUserMarkRed = Color(0x88FA412D)
val CheckGlow = Color(0xEBFA412D)
val ArrowBestGreen = Color(0xE681B64C)
val ArrowSecondaryCyan = Color(0xCC4C9AFF)
val ArrowPlayedRed = Color(0xCCCA3431)
val ArrowUserOrange = Color(0xD9FFA91F)

// Board Theme Palettes (Default: Green/Cream #EEEED2 / #769656)
enum class BoardTheme(
    val id: String,
    val label: String,
    val lightSquare: Color,
    val darkSquare: Color
) {
    GREEN("green", "Green/Cream", Color(0xFFEEEED2), Color(0xFF769656)),
    GOLD("gold", "Gold/Classic", Color(0xFFF3E7C8), Color(0xFF9C7B32)),
    BLUE("blue", "Blue/Steel", Color(0xFFDEE4EC), Color(0xFF546E8A)),
    BROWN("brown", "Brown/Wood", Color(0xFFF0D9B5), Color(0xFFB58863));

    companion object {
        fun fromId(id: String): BoardTheme = when (id.lowercase()) {
            "green" -> GREEN
            "brown", "wood" -> BROWN
            "blue", "slate", "steel" -> BLUE
            "gold", "royal", "obsidian", "classic" -> GOLD
            else -> GREEN
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
