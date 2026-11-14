package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core KnightScope Dark Palette (from index-3.html CSS variables)
val BgDark = Color(0xFF111512)
val BgRadialTop = Color(0xFF1C2B20)
val SurfaceDark = Color(0xFF191F1B)
val CardDark = Color(0xFF202923)
val CardSurfaceElevated = Color(0xFF253029)

val AccentGreen = Color(0xFF81B64C)
val AccentGreenHover = Color(0xFF96C95F)
val AccentGreenDark = Color(0xFF5F9A35)
val OnAccentDark = Color(0xFF10200A)

val TextPrimary = Color(0xFFF4F6F3)
val TextMuted = Color(0xFFA5AEA7)
val BorderSubtle = Color(0xFF303A33)
val LineSubtle = Color(0x14FFFFFF)

// Move Classification Semantic Colors
val ClassBest = Color(0xFF81B64C)
val ClassGood = Color(0xFFA3C25A)
val ClassInaccuracy = Color(0xFFE6B94A)
val ClassMistake = Color(0xFFE8883A)
val ClassBlunder = Color(0xFFD9534F)
val ClassBrilliant = Color(0xFF26C2C9)

// Board Square Highlights & Overlays
val HighlightLastMove = Color(0x99F5D646)
val HighlightSelected = Color(0xFFE5D34B)
val HighlightHint = Color(0x8050A0FF)
val HighlightWrong = Color(0x99DC3232)
val CheckGlow = Color(0xE6DC2828)
val ArrowBestGreen = Color(0xE62E9D57)
val ArrowPlayedRed = Color(0x99D6453D)

// Board Theme Palettes
enum class BoardTheme(
    val id: String,
    val label: String,
    val lightSquare: Color,
    val darkSquare: Color
) {
    WOOD("wood", "Classic wood", Color(0xFFF0D9B5), Color(0xFFB58863)),
    GREEN("green", "Tournament green", Color(0xFFEEEED2), Color(0xFF769656)),
    BLUE("blue", "Slate blue", Color(0xFFDEE3E6), Color(0xFF8CA2AD));

    companion object {
        fun fromId(id: String): BoardTheme = entries.find { it.id == id } ?: WOOD
    }
}
