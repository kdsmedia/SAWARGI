package com.altomedia.sawargi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = BrandOnPrimary,
    secondary = BrandDark,
    onSecondary = BrandOnPrimary,
    tertiary = Accent,
    background = BrandBackground,
    surface = BrandSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreenDark,
    onPrimary = BrandOnPrimaryDark,
    secondary = BrandGreen,
    tertiary = Accent,
    background = BrandBackgroundDark,
    surface = BrandSurfaceDark,
)

@Composable
fun SawargiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}