package com.sonara.music.ui.component.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.music.playback.audio.AudioSpectrumBus
import com.sonara.music.ui.theme.LocalRegaColors

@Composable
fun LiquidGlassVisualizer(
    isPlaying: Boolean,
    isVisible: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(isPlaying, isVisible) {
        val active = isPlaying && isVisible
        AudioSpectrumBus.isActive = active
        if (!active) {
            AudioSpectrumBus.clear()
        }
        onDispose {
            AudioSpectrumBus.isActive = false
            AudioSpectrumBus.clear()
        }
    }

    val bars by AudioSpectrumBus.bars.collectAsStateWithLifecycle()
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val regaColors = LocalRegaColors.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(shape)
                .then(
                    if (liquidGlassEnabled) {
                        Modifier
                            .border(
                                width = 1.dp,
                                brush =
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.24f),
                                            barColor.copy(alpha = 0.14f),
                                        ),
                                    ),
                                shape = shape,
                            )
                    } else {
                        Modifier.border(
                            width = 0.75.dp,
                            color = regaColors.borderSubtle,
                            shape = shape,
                        )
                    },
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(),
        ) {
            val count = bars.size
            if (count == 0) return@Canvas
            val gap = 3.dp.toPx()
            val totalGap = gap * (count - 1)
            val barWidth = ((size.width - totalGap) / count).coerceAtLeast(2f)
            val corner = CornerRadius(barWidth * 0.5f, barWidth * 0.5f)
            val minBarHeight = 2.5.dp.toPx()
            val halfCenter = (count - 1) * 0.5f
            val brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            barColor.copy(alpha = 0.92f),
                            barColor.copy(alpha = 0.42f),
                        ),
                )

            for (i in 0 until count) {
                val mirrorIdx = kotlin.math.abs(i - halfCenter).toInt().coerceIn(0, count - 1)
                val level = maxOf(bars[i], bars[mirrorIdx] * 0.45f).coerceIn(0f, 1f)
                val h = minBarHeight + (size.height - minBarHeight) * level
                val x = i * (barWidth + gap)
                val y = (size.height - h) * 0.5f
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, h),
                    cornerRadius = corner,
                )
            }
        }
    }
}
