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
val LocalPieceStyle = staticCompositionLocalOf { PieceStyle.NEO }
val LocalBoard3dEnabled = staticCompositionLocalOf { false }
val LocalSoundEnabled = staticCompositionLocalOf { true }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

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
    surfaceVariant = DarkStudioPalette.cardElevated,
    onSurfaceVariant = DarkStudioPalette.textMuted,
    outline = DarkStudioPalette.borderSubtle,
    outlineVariant = DarkStudioPalette.lineSubtle,
    error = DarkStudioPalette.danger,
    onError = DarkStudioPalette.textPrimary
)

private val KnightScopeLightColorScheme = lightColorScheme(
    primary = LightClassicPalette.accentGreen,
    onPrimary = LightClassicPalette.onAccent,
    primaryContainer = LightClassicPalette.accentGreenDark,
    onPrimaryContainer = LightClassicPalette.textPrimary,
    secondary = LightClassicPalette.studioCyan,
    onSecondary = LightClassicPalette.surface,
    secondaryContainer = LightClassicPalette.cardElevated,
    onSecondaryContainer = LightClassicPalette.textPrimary,
    tertiary = ClassBrilliant,
    onTertiary = LightClassicPalette.surface,
    background = LightClassicPalette.bg,
    onBackground = LightClassicPalette.textPrimary,
    surface = LightClassicPalette.surface,
    onSurface = LightClassicPalette.textPrimary,
    surfaceVariant = LightClassicPalette.cardElevated,
    onSurfaceVariant = LightClassicPalette.textMuted,
    outline = LightClassicPalette.borderSubtle,
    outlineVariant = LightClassicPalette.lineSubtle,
    error = LightClassicPalette.danger,
    onError = LightClassicPalette.surface
)

private val KnightScopeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun KnightScopeTheme(
    darkTheme: Boolean = true,
    pieceAnimations: Boolean = true,
    pieceStyle: PieceStyle = PieceStyle.NEO,
    board3dEnabled: Boolean = false,
    soundEnabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkStudioPalette else LightClassicPalette
    val colorScheme = if (darkTheme) KnightScopeDarkColorScheme else KnightScopeLightColorScheme

    CompositionLocalProvider(
        LocalKnightScopeColors provides palette,
        LocalPieceAnimations provides pieceAnimations,
        LocalPieceStyle provides pieceStyle,
        LocalBoard3dEnabled provides board3dEnabled,
        LocalSoundEnabled provides soundEnabled,
        LocalHapticsEnabled provides hapticsEnabled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KnightScopeTypography,
            shapes = KnightScopeShapes,
            content = content
        )
    }
}
