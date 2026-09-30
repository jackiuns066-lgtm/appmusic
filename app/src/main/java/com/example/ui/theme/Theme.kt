package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGold,
    secondary = PrimaryPurple,
    tertiary = AccentCyan,
    background = DeepIndigoBackground,
    surface = DeepIndigoSurface,
    surfaceVariant = DeepIndigoSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF0F0F5),
    onSurface = Color(0xFFF0F0F5),
    error = AccentCrimson
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5E35B1),
    secondary = Color(0xFFFFB300),
    tertiary = Color(0xFF00ACC1),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1E1E26),
    onSurface = Color(0xFF1E1E26),
    error = AccentCrimson
)

@Composable
fun AvaMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentTheme: String = "gold",
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val finalScheme = if (darkTheme && isAmoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color(0xFF0C0C0E),
            surfaceVariant = Color(0xFF1A1A1E)
        )
    } else {
        baseScheme
    }

    val themedScheme = when (accentTheme) {
        "emerald" -> finalScheme.copy(primary = AccentEmerald)
        "cyan" -> finalScheme.copy(primary = AccentCyan)
        "crimson" -> finalScheme.copy(primary = AccentCrimson)
        "purple" -> finalScheme.copy(primary = PrimaryPurple)
        else -> finalScheme
    }

    MaterialTheme(
        colorScheme = themedScheme,
        typography = Typography,
        content = content
    )
}
