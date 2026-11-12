package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarehouseDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkNavyBase,
    primaryContainer = DarkNavySurfaceHigh,
    onPrimaryContainer = NeonCyan,
    secondary = AmberOrange,
    onSecondary = DarkNavyBase,
    secondaryContainer = DarkNavySurfaceVariant,
    onSecondaryContainer = AmberOrange,
    tertiary = EmeraldGreen,
    onTertiary = DarkNavyBase,
    background = DarkNavyBase,
    onBackground = TextWhite,
    surface = DarkNavyCard,
    onSurface = TextWhite,
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = DarkNavyCardBorder,
    error = CoralRed,
    onError = Color.White
)

private val WarehouseLightColorScheme = lightColorScheme(
    primary = NeonCyanMuted,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceHigh,
    onPrimaryContainer = DarkNavyBase,
    secondary = AmberOrange,
    onSecondary = DarkNavyBase,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = DarkNavyBase,
    tertiary = EmeraldGreen,
    onTertiary = Color.White,
    background = LightSurfaceBase,
    onBackground = TextDark,
    surface = LightSurfaceCard,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextMutedDark,
    outline = LightSurfaceCardBorder,
    error = CrimsonRed,
    onError = Color.White
)

@Composable
fun WarehouseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WarehouseDarkColorScheme else WarehouseLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
