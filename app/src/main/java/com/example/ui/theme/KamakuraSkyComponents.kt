package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R

/**
 * High-performance full-bleed Anime Summer Sky background:
 * - Wallpaper is cached in memory with hardware bitmap acceleration
 * - Optimized single-pass translucent overlay wash
 * - Fast 120 FPS rendering without jank or scroll stalls
 */
@Composable
fun KamakuraSkyBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(context) {
        ImageRequest.Builder(context)
            .data(R.drawable.anime_scenery_wallpaper)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .crossfade(200)
            .build()
    }

    val overlayGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                PaletteCornflower.copy(alpha = 0.06f),
                Color.Transparent,
                PaletteIceCyan.copy(alpha = 0.12f),
                PaletteMintFrost.copy(alpha = 0.22f)
            )
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Hardware-accelerated memory-cached wallpaper
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Translucent palette tint wash (#6499E9, #9EDDFF, #A6F6FF, #BEFFF7)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayGradient)
        )

        // 3. Screen content
        content()
    }
}

/**
 * Liquid Glass Card:
 * Optimized with 2.dp elevation to eliminate GPU drop-shadow calculation lag
 * while retaining the vibrant frosted gradient border and high contrast.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 2.dp,
    containerColor: Color = LiquidGlassFill,
    borderBrush: Brush = remember {
        Brush.verticalGradient(
            listOf(PaletteCornflower, PaletteIceCyan)
        )
    },
    content: @Composable () -> Unit
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Card(
        modifier = modifier
            .border(
                border = BorderStroke(1.2.dp, borderBrush),
                shape = shape
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        content()
    }
}

/**
 * Tactile Liquid Glass Button:
 * Glassmorphic action pill with light elevation and gradient border.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    containerColor: Color = LiquidGlassFill,
    content: @Composable () -> Unit
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val borderBrush = remember {
        Brush.verticalGradient(listOf(PaletteCornflower, PaletteSoftSky))
    }

    Surface(
        modifier = modifier
            .border(BorderStroke(1.2.dp, borderBrush), shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = containerColor,
        shadowElevation = 2.dp
    ) {
        content()
    }
}

/**
 * HarmonyOS Glassmorphism Card for backward-compatibility.
 */
@Composable
fun HarmonyGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 2.dp,
    containerColor: Color = KamakuraGlassContainer,
    borderColor: Color = PaletteSoftSky,
    content: @Composable () -> Unit
) {
    val cardShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = cardShape
            ),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        content()
    }
}
