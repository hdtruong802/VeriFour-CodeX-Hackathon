package com.verifour.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorMutedDark = androidx.compose.ui.graphics.Color(0xFF9AB8A8)

private val DarkColorScheme = darkColorScheme(
    primary = EcoPrimaryGlow,
    onPrimary = EcoForeground,
    primaryContainer = EcoPrimaryDark,
    onPrimaryContainer = EcoDarkForeground,
    secondary = EcoDarkCard,
    onSecondary = EcoDarkForeground,
    tertiary = EcoOcean,
    background = EcoDarkBackground,
    onBackground = EcoDarkForeground,
    surface = EcoDarkSurface,
    onSurface = EcoDarkForeground,
    surfaceVariant = EcoDarkCard,
    onSurfaceVariant = ColorMutedDark,
    outline = EcoDarkBorder,
    error = EcoCoral
)

private val LightColorScheme = lightColorScheme(
    primary = EcoPrimary,
    onPrimary = EcoPrimaryForeground,
    primaryContainer = EcoSecondary,
    onPrimaryContainer = EcoForeground,
    secondary = EcoSecondary,
    onSecondary = EcoSecondaryForeground,
    tertiary = EcoOcean,
    background = EcoBackground,
    onBackground = EcoForeground,
    surface = EcoCard,
    onSurface = EcoForeground,
    surfaceVariant = EcoSurface,
    onSurfaceVariant = EcoMutedForeground,
    outline = EcoBorder,
    error = EcoCoral
)

@Composable
fun VeriFourTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
