package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// UnCappio Dark Studio Palette
val BgDark = Color(0xFF0C1014)
val BgRadialTop = Color(0xFF152230)
val SurfaceDark = Color(0xFF131920)
val CardDark = Color(0xFF182028)
val CardSurfaceElevated = Color(0xFF1E2833)

val AccentGreen = Color(0xFF22C55E)
val AccentGreenHover = Color(0xFF4ADE80)
val AccentGreenDark = Color(0xFF16A34A)
val StudioCyan = Color(0xFF2DD4BF)
val OnAccentDark = Color(0xFF071A0E)

val TextPrimary = Color(0xFFF3F6FA)
val TextMuted = Color(0xFF94A3B8)
val BorderSubtle = Color(0xFF273240)
val LineSubtle = Color(0x18FFFFFF)

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
val HighlightWrong = Color(0x99DF534C)
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
