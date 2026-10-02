package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Secondary Brain 2.0 - Exclusive 4-Color Anime Sky Palette
// #6499E9, #9EDDFF, #A6F6FF, #BEFFF7
// =========================================================================
val PaletteCornflower = Color(0xFF6499E9) // #6499E9 - Vivid Cornflower Azure
val PaletteSoftSky    = Color(0xFF9EDDFF) // #9EDDFF - Soft Light Horizon Blue
val PaletteIceCyan    = Color(0xFFA6F6FF) // #A6F6FF - Bright Aqua Ice
val PaletteMintFrost  = Color(0xFFBEFFF7) // #BEFFF7 - Frosted Seafoam Mint

// Semantic tokens built strictly from the 4 colors
val KamakuraDeepCobalt = PaletteCornflower
val KamakuraSkyBlue = PaletteCornflower
val KamakuraSoftHorizon = PaletteSoftSky
val KamakuraCloudWhite = Color(0xFFFFFFFF)
val KamakuraCloudShadow = PaletteSoftSky.copy(alpha = 0.35f)

// Glass container fills
val LiquidGlassFill = Color(0xD8FFFFFF)
val KamakuraGlassContainer = PaletteIceCyan.copy(alpha = 0.50f)
val KamakuraGlassHigh = Color(0xF2FFFFFF)

// Primary accents and typography
val KamakuraSignBlue = PaletteCornflower
val KamakuraTextPrimary = Color(0xFF1E3A5F)   // High-contrast deep navy ink based on #6499E9
val KamakuraTextSecondary = Color(0xFF4A729A) // Soft muted slate blue based on #6499E9

// Accents strictly mapped to the 4 palette colors
val KamakuraSunWarmth = PaletteCornflower
val KamakuraEmerald = PaletteCornflower
val KamakuraCoralWarning = PaletteCornflower
val KamakuraSeaFoam = PaletteMintFrost

// Backwards-compatible aliases
val CyberBg = PaletteIceCyan
val CyberSurface = Color.White
val CyberSurfaceVariant = PaletteSoftSky.copy(alpha = 0.30f)
val CyberBorder = PaletteSoftSky
val CyberCyan = PaletteCornflower
val CyberCyanMuted = PaletteSoftSky
val CyberGreen = PaletteCornflower
val CyberGreenMuted = PaletteMintFrost
val CyberAmber = PaletteCornflower
val CyberRed = PaletteCornflower
val CyberTextPrimary = KamakuraTextPrimary
val CyberTextSecondary = KamakuraTextSecondary
val CyberTextMuted = KamakuraTextSecondary
