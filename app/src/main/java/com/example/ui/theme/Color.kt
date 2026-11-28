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
    val accentGreen: Color,       // Primary green accent (#81B64C)
    val accentGreenHover: Color,  // Bright Emerald highlight
    val accentGreenDark: Color,   // Tactile 3D button lip & container shade
    val studioCyan: Color,        // Semantic Info Blue
    val trophyGold: Color,        // Warm Gold for trophies, best streaks & rewards
    val onAccent: Color,          // High-contrast ink on green buttons
    val textPrimary: Color,
    val textMuted: Color,         // Secondary text
    val textDisabled: Color,      // Disabled text
    val borderSubtle: Color,      // Hairline border
    val lineSubtle: Color,
    val success: Color,           // #81B64C
    val danger: Color,            // Error / Blunder red
    val lightGreen: Color = Color(0xFFE8F0DF),
    val softGrey: Color = Color(0xFFF0F2EF),
    val lightBlue: Color = Color(0xFFEAF1F7),
    val matteDark: Color = Color(0xFF18181B),
    val matteDarkSecondary: Color = Color(0xFF1F2937),
    val onMatteDark: Color = Color(0xFFFFFFFF),
    val onMatteDarkMuted: Color = Color(0xFFA1A1AA),
    val matteDarkBorder: Color = Color(0xFF27272A),
    val inputBg: Color = Color(0xFFFFFFFF),
    val inputText: Color = Color(0xFF25332B),
    val inputHint: Color = Color(0xFF76847B),
    val inputBorder: Color = Color(0xFFD2D8CF)
)

// Optional Dark Mode Palette (Settings Dark Theme toggle) — aligned to Matte #18181B / #1F2937 / #81B64C
val DarkStudioPalette = KnightScopeColors(
    isDark = true,
    bg = Color(0xFF111114),
    bgRadialTop = Color(0xFF16161A),
    surface = Color(0xFF18181B),
    card = Color(0xFF18181B),
    cardElevated = Color(0xFF1F2937),
    accentGreen = Color(0xFF81B64C),
    accentGreenHover = Color(0xFF93C45E),
    accentGreenDark = Color(0xFF659635),
    studioCyan = Color(0xFF60A5FA),
    trophyGold = Color(0xFFF5C542),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFF9FAFB),
    textMuted = Color(0xFFA1A1AA),
    textDisabled = Color(0xFF71717A),
    borderSubtle = Color(0xFF27272A),
    lineSubtle = Color(0xFF27272A),
    success = Color(0xFF81B64C),
    danger = Color(0xFFEF4444),
    lightGreen = Color(0xFF233527),
    softGrey = Color(0xFF1F2937),
    lightBlue = Color(0xFF1E293B),
    matteDark = Color(0xFF18181B),
    matteDarkSecondary = Color(0xFF1F2937),
    onMatteDark = Color(0xFFFFFFFF),
    onMatteDarkMuted = Color(0xFFA1A1AA),
    matteDarkBorder = Color(0xFF27272A),
    inputBg = Color(0xFFFFFFFF),
    inputText = Color(0xFF111827),
    inputHint = Color(0xFF6B7280),
    inputBorder = Color(0xFFD1D5DB)
)

// Default Premium Light Palette (#F5F7F5 Canvas, #FFFFFF Cards, #E8F0DF Light Green, #F0F2EF Soft Grey, #EAF1F7 Light Blue, #81B64C Green, #25332B Text)
val LightClassicPalette = KnightScopeColors(
    isDark = false,
    bg = Color(0xFFF5F7F5),
    bgRadialTop = Color(0xFFF5F7F5),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF0F2EF),
    accentGreen = Color(0xFF81B64C),
    accentGreenHover = Color(0xFF93C45E),
    accentGreenDark = Color(0xFF659635),
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

val LocalKnightScopeColors = staticCompositionLocalOf { LightClassicPalette }

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
val HighlightLastMove = Color(0x66FFFF00) // rgba(255,255,0,0.4)
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
