/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * MONOCHROME SONIC signature motion: a live waveform that breathes with playback.
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.LocalRegaColors

/**
 * A compact equalizer-style waveform.
 *
 * Each bar runs its own loop so the group never looks mechanical. The infinite
 * animation only exists while playback is running: a paused player draws a
 * static resting silhouette, which keeps the mini player from waking the GPU
 * every frame.
 */
@Composable
fun AnimatedWaveform(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    barCount: Int = 5,
    color: Color = LocalRegaColors.current.textPrimary,
    barWidth: Dp = 2.dp,
    barGap: Dp = 3.dp,
    minBarHeight: Dp = 3.dp,
    maxBarHeight: Dp = 16.dp,
) {
    // Only spin up the infinite transition while something is actually playing.
    val bars = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "waveform")
        List(barCount) { index ->
            transition.animateFloat(
                initialValue = 0.22f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 620 + index * 137,
                        delayMillis = index * 83,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "waveformBar$index",
            )
        }
    } else {
        emptyList()
    }

    val restingFraction = 0.18f
    val width = barWidth + barGap

    Canvas(modifier = modifier) {
        val barWidthPx = barWidth.toPx()
        val totalWidth = (barWidthPx + barGap.toPx()) * barCount - barGap.toPx()
        val startX = (size.width - totalWidth) / 2f
        val minPx = minBarHeight.toPx()
        val maxPx = maxBarHeight.toPx().coerceAtMost(size.height)
        val radius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)

        repeat(barCount) { index ->
            val fraction = if (bars.isEmpty()) restingFraction else {
                restingFraction + bars[index].value * (1f - restingFraction)
            }
            val barHeight = (minPx + (maxPx - minPx) * fraction).coerceAtMost(size.height)
            val left = startX + index * width.toPx()
            drawRoundRect(
                color = color.copy(alpha = if (bars.isEmpty()) 0.35f else 0.9f),
                topLeft = Offset(left, (size.height - barHeight) / 2f),
                size = Size(barWidthPx, barHeight),
                cornerRadius = radius,
            )
        }
    }
}
