package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CustomColorScheme = darkColorScheme(
    primary = OrangeAccent,
    onPrimary = DeepPurple,
    primaryContainer = SlateSurfaceLight,
    onPrimaryContainer = OrangeLight,
    secondary = TealSecondary,
    onSecondary = SlateDarkBackground,
    background = SlateDarkBackground,
    onBackground = TextWhite,
    surface = SlateSurface,
    onSurface = TextWhite,
    surfaceVariant = SlateSurfaceLight,
    onSurfaceVariant = TextMuted,
    outline = CardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force our beautiful dark theme
    dynamicColor: Boolean = false, // Use our handcrafted colors exclusively
    content: @Composable () -> Unit,
) {
    val colorScheme = CustomColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
