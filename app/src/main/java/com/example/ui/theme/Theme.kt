package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SecondaryBrainColorScheme = lightColorScheme(
    primary = PaletteCornflower,             // #6499E9
    onPrimary = Color.White,
    primaryContainer = PaletteSoftSky,       // #9EDDFF
    onPrimaryContainer = PaletteCornflower,
    secondary = PaletteSoftSky,              // #9EDDFF
    onSecondary = PaletteCornflower,
    secondaryContainer = PaletteIceCyan,     // #A6F6FF
    onSecondaryContainer = PaletteCornflower,
    tertiary = PaletteMintFrost,             // #BEFFF7
    onTertiary = PaletteCornflower,
    background = Color.White,
    onBackground = KamakuraTextPrimary,
    surface = Color.White,
    onSurface = KamakuraTextPrimary,
    surfaceVariant = PaletteIceCyan,         // #A6F6FF
    onSurfaceVariant = KamakuraTextSecondary,
    outline = PaletteSoftSky,                // #9EDDFF
    error = PaletteCornflower,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SecondaryBrainColorScheme,
        typography = Typography,
        content = content
    )
}
