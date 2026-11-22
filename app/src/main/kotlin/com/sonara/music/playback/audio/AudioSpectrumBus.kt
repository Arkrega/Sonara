package com.sonara.music.playback.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioSpectrumBus {
    const val BAR_COUNT = 24

    @Volatile
    var isActive: Boolean = false

    private val smoothedBars = FloatArray(BAR_COUNT)
    private val _bars = MutableStateFlow(FloatArray(BAR_COUNT))
    val bars: StateFlow<FloatArray> = _bars.asStateFlow()

    fun publish(rawBars: FloatArray) {
        val count = minOf(rawBars.size, BAR_COUNT)
        val snapshot = FloatArray(BAR_COUNT)
        for (i in 0 until count) {
            val target = rawBars[i].coerceIn(0f, 1f)
            val prev = smoothedBars[i]
            val coeff = if (target > prev) 0.55f else 0.18f
            val next = (prev + (target - prev) * coeff).coerceIn(0f, 1f)
            smoothedBars[i] = next
            snapshot[i] = next
        }
        _bars.value = snapshot
    }

    fun clear() {
        smoothedBars.fill(0f)
        _bars.value = FloatArray(BAR_COUNT)
    }
}
