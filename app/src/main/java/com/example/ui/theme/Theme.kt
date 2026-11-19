package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalPieceAnimations = staticCompositionLocalOf { true }

private val KnightScopeDarkColorScheme = darkColorScheme(
    primary = DarkStudioPalette.accentGreen,
    onPrimary = DarkStudioPalette.onAccent,
    primaryContainer = DarkStudioPalette.accentGreenDark,
    onPrimaryContainer = DarkStudioPalette.textPrimary,
    secondary = DarkStudioPalette.studioCyan,
    onSecondary = DarkStudioPalette.onAccent,
    secondaryContainer = DarkStudioPalette.cardElevated,
    onSecondaryContainer = DarkStudioPalette.textPrimary,
    tertiary = ClassBrilliant,
    onTertiary = DarkStudioPalette.onAccent,
    background = DarkStudioPalette.bg,
    onBackground = DarkStudioPalette.textPrimary,
    surface = DarkStudioPalette.surface,
    onSurface = DarkStudioPalette.textPrimary,
    surfaceVariant = DarkStudioPalette.card,
    onSurfaceVariant = DarkStudioPalette.textMuted,
    outline = DarkStudioPalette.borderSubtle,
    outlineVariant = DarkStudioPalette.lineSubtle,
    error = ClassBlunder,
    onError = DarkStudioPalette.textPrimary
)

private val KnightScopeLightColorScheme = lightColorScheme(
    primary = LightClassicPalette.accentGreen,
    onPrimary = LightClassicPalette.onAccent,
    primaryContainer = LightClassicPalette.accentGreenDark,
    onPrimaryContainer = LightClassicPalette.onAccent,
    secondary = LightClassicPalette.studioCyan,
    onSecondary = LightClassicPalette.onAccent,
    secondaryContainer = LightClassicPalette.cardElevated,
    onSecondaryContainer = LightClassicPalette.textPrimary,
    tertiary = ClassBrilliant,
    onTertiary = LightClassicPalette.onAccent,
    background = LightClassicPalette.bg,
    onBackground = LightClassicPalette.textPrimary,
    surface = LightClassicPalette.surface,
    onSurface = LightClassicPalette.textPrimary,
    surfaceVariant = LightClassicPalette.card,
    onSurfaceVariant = LightClassicPalette.textMuted,
    outline = LightClassicPalette.borderSubtle,
    outlineVariant = LightClassicPalette.lineSubtle,
    error = ClassBlunder,
    onError = LightClassicPalette.onAccent
)

private val KnightScopeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun KnightScopeTheme(
    darkTheme: Boolean = true,
    pieceAnimations: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkStudioPalette else LightClassicPalette
    val colorScheme = if (darkTheme) KnightScopeDarkColorScheme else KnightScopeLightColorScheme

    CompositionLocalProvider(
        LocalKnightScopeColors provides palette,
        LocalPieceAnimations provides pieceAnimations
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KnightScopeTypography,
            shapes = KnightScopeShapes,
            content = content
        )
    }
}
