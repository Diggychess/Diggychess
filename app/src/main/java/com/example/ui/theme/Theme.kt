package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KnightScopeColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = OnAccentDark,
    primaryContainer = AccentGreenDark,
    onPrimaryContainer = TextPrimary,
    secondary = StudioCyan,
    onSecondary = OnAccentDark,
    secondaryContainer = CardSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = ClassBrilliant,
    onTertiary = OnAccentDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextMuted,
    outline = BorderSubtle,
    outlineVariant = LineSubtle,
    error = ClassBlunder,
    onError = TextPrimary
)

private val KnightScopeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun KnightScopeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KnightScopeColorScheme,
        typography = KnightScopeTypography,
        shapes = KnightScopeShapes,
        content = content
    )
}
