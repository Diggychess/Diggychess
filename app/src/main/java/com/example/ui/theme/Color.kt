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
    val accentGreen: Color,       // Chess.com-inspired Emerald Green (#7FA650)
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
    val success: Color,           // #7FA650
    val danger: Color,            // Error / Blunder red
    val matteDark: Color = Color(0xFF18181B),
    val matteDarkSecondary: Color = Color(0xFF1F2937),
    val onMatteDark: Color = Color(0xFFFFFFFF),
    val onMatteDarkMuted: Color = Color(0xFFA1A1AA),
    val matteDarkBorder: Color = Color(0xFF27272A),
    val inputBg: Color = Color(0xFFFFFFFF),
    val inputText: Color = Color(0xFF111827),
    val inputHint: Color = Color(0xFF6B7280),
    val inputBorder: Color = Color(0xFFD1D5DB)
)

// Optional Dark Mode Palette (Settings Dark Theme toggle) — aligned to Matte #18181B / #1F2937 / #7FA650
val DarkStudioPalette = KnightScopeColors(
    isDark = true,
    bg = Color(0xFF111114),
    bgRadialTop = Color(0xFF16161A),
    surface = Color(0xFF18181B),
    card = Color(0xFF18181B),
    cardElevated = Color(0xFF1F2937),
    accentGreen = Color(0xFF7FA650),
    accentGreenHover = Color(0xFF93BA64),
    accentGreenDark = Color(0xFF5D7D37),
    studioCyan = Color(0xFF60A5FA),
    trophyGold = Color(0xFFF5C542),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFF9FAFB),
    textMuted = Color(0xFFA1A1AA),
    textDisabled = Color(0xFF71717A),
    borderSubtle = Color(0xFF27272A),
    lineSubtle = Color(0xFF27272A),
    success = Color(0xFF7FA650),
    danger = Color(0xFFEF4444),
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

// Default Premium Studio Palette (#F4F5F7 Canvas, #FFFFFF Cards, #18181B Matte Dark Cards, #7FA650 Green)
val LightClassicPalette = KnightScopeColors(
    isDark = false,
    bg = Color(0xFFF4F5F7),
    bgRadialTop = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFEEF0F3),
    accentGreen = Color(0xFF7FA650),
    accentGreenHover = Color(0xFF90B860),
    accentGreenDark = Color(0xFF5F8038),
    studioCyan = Color(0xFF2563EB),
    trophyGold = Color(0xFFD97706),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF111827),
    textMuted = Color(0xFF4B5563),
    textDisabled = Color(0xFF9CA3AF),
    borderSubtle = Color(0xFFE5E7EB),
    lineSubtle = Color(0xFFE5E7EB),
    success = Color(0xFF7FA650),
    danger = Color(0xFFDC2626),
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

val LocalKnightScopeColors = staticCompositionLocalOf { LightClassicPalette }

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
