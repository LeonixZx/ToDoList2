package com.example.todolist.ui.theme

import com.example.todolist.ui.theme.MacOSColors
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MacOSColors.Primary,
    secondary = MacOSColors.Secondary,
    background = MacOSColors.Background,
    surface = MacOSColors.Surface,
    onPrimary = MacOSColors.OnPrimary,
    onSecondary = MacOSColors.OnSecondary,
    onBackground = MacOSColors.OnBackground,
    onSurface = MacOSColors.OnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = MacOSColors.Primary,
    secondary = MacOSColors.Secondary,
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF2D2D2D),
    onPrimary = MacOSColors.OnPrimary,
    onSecondary = MacOSColors.OnSecondary,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MacOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}