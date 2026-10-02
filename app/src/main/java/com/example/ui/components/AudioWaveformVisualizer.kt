package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import com.example.ui.theme.PaletteSoftSky

@Composable
fun LiveAudioWaveform(
    isRecordingOrPlaying: Boolean,
    currentAmplitude: Int = 0,
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    maxHeight: Dp = 48.dp,
    barWidth: Dp = 3.5.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWaveform")

    val animFactors = (0 until barCount).map { index ->
        val duration = 300 + (index * 47) % 400
        val target = 0.25f + ((index * 13) % 75) / 100f
        val animatedValue by infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
        animatedValue
    }

    val normalizedAmp = (currentAmplitude / 32767f).coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (0 until barCount).forEach { index ->
            val factor = animFactors[index]
            val heightFrac = if (isRecordingOrPlaying) {
                (factor * 0.5f + normalizedAmp * 0.5f).coerceIn(0.12f, 1f)
            } else {
                0.08f
            }

            val barHeight = maxHeight * heightFrac
            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    PaletteCornflower,
                    PaletteSoftSky,
                    if (index % 2 == 0) PaletteMintFrost else PaletteIceCyan
                )
            )

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isRecordingOrPlaying) barBrush else Brush.verticalGradient(listOf(Color.LightGray.copy(alpha = 0.5f), Color.LightGray.copy(alpha = 0.3f))))
            )
        }
    }
}
