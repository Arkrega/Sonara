/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.media3.common.Player
import com.metrolist.music.playback.PlayerConnection
import kotlin.math.abs
import kotlin.math.roundToLong

class LyricsPlayheadTracker(
    private val maxExtrapolationMs: Long = DEFAULT_MAX_EXTRAPOLATION_MS,
    private val seekSnapThresholdMs: Long = DEFAULT_SEEK_SNAP_THRESHOLD_MS,
) {
    private var anchorPositionMs: Long = 0L
    private var anchorFrameMs: Long = 0L
    private var currentEstimateMs: Long = 0L
    private var hasInitialized: Boolean = false
    private var lastReportedPlayerPosMs: Long = -1L

    fun currentPositionMs(): Long = currentEstimateMs

    fun reset(
        positionMs: Long = 0L,
        frameTimeMs: Long = 0L,
    ) {
        val safePos = positionMs.coerceAtLeast(0L)
        anchorPositionMs = safePos
        anchorFrameMs = frameTimeMs
        currentEstimateMs = safePos
        lastReportedPlayerPosMs = safePos
        hasInitialized = true
    }

    fun updateSample(
        playerPositionMs: Long,
        isPlaying: Boolean,
        isBuffering: Boolean,
        playbackSpeed: Float,
        frameTimeMs: Long,
        forceSnap: Boolean = false,
    ): Long {
        val safeReported = playerPositionMs.coerceAtLeast(0L)
        val safeSpeed = playbackSpeed.coerceIn(0.25f, 4.0f)

        if (!hasInitialized || forceSnap) {
            reset(safeReported, frameTimeMs)
            return currentEstimateMs
        }

        if (isBuffering || !isPlaying) {
            anchorPositionMs = safeReported
            anchorFrameMs = frameTimeMs
            currentEstimateMs = safeReported
            lastReportedPlayerPosMs = safeReported
            return currentEstimateMs
        }

        val playerAdvanced = safeReported != lastReportedPlayerPosMs
        if (playerAdvanced) {
            val deltaFromEstimate = abs(safeReported - currentEstimateMs)
            if (deltaFromEstimate > seekSnapThresholdMs || safeReported + 40L < lastReportedPlayerPosMs) {
                anchorPositionMs = safeReported
                anchorFrameMs = frameTimeMs
                currentEstimateMs = safeReported
                lastReportedPlayerPosMs = safeReported
                return currentEstimateMs
            }

            anchorPositionMs = safeReported
            anchorFrameMs = frameTimeMs
            lastReportedPlayerPosMs = safeReported
        }

        val rawElapsedMs = (frameTimeMs - anchorFrameMs).coerceAtLeast(0L)
        val boundedElapsedMs = rawElapsedMs.coerceAtMost(maxExtrapolationMs)
        val scaledAdvanceMs = (boundedElapsedMs * safeSpeed).roundToLong()
        val predictedMs = anchorPositionMs + scaledAdvanceMs

        currentEstimateMs = if (abs(predictedMs - currentEstimateMs) > seekSnapThresholdMs) {
            predictedMs
        } else {
            maxOf(currentEstimateMs, predictedMs)
        }
        return currentEstimateMs
    }

    fun effectivePositionMs(offsetMs: Long): Long =
        (currentEstimateMs + offsetMs).coerceAtLeast(0L)

    companion object {
        const val DEFAULT_MAX_EXTRAPOLATION_MS = 250L
        const val DEFAULT_SEEK_SNAP_THRESHOLD_MS = 450L
    }
}

@Stable
class LyricsPlayheadState internal constructor(
    private val positionState: androidx.compose.runtime.MutableLongState,
) : State<Long> {
    override val value: Long
        get() = positionState.longValue
}

@Composable
fun rememberLyricsPlayhead(
    playerConnection: PlayerConnection,
    sliderPositionProvider: () -> Long?,
    lyricsOffsetProvider: () -> Int,
    songId: String?,
    isActive: Boolean = true,
): State<Long> {
    val positionState = remember {
        val initialRaw = sliderPositionProvider() ?: playerConnection.player.currentPosition
        val initialOffset = lyricsOffsetProvider().toLong()
        mutableLongStateOf((initialRaw + initialOffset).coerceAtLeast(0L))
    }
    val tracker = remember { LyricsPlayheadTracker() }
    val latestSliderProvider by rememberUpdatedState(sliderPositionProvider)
    val latestOffsetProvider by rememberUpdatedState(lyricsOffsetProvider)

    LaunchedEffect(playerConnection, songId) {
        val raw = latestSliderProvider() ?: playerConnection.player.currentPosition
        tracker.reset(raw, 0L)
        positionState.longValue = tracker.effectivePositionMs(latestOffsetProvider().toLong())
    }

    LaunchedEffect(playerConnection, isActive, songId) {
        if (!isActive) return@LaunchedEffect

        var previousRawPos = latestSliderProvider() ?: playerConnection.player.currentPosition
        var isFirstFrame = true

        while (true) {
            withFrameNanos { frameNanos ->
                val frameMs = frameNanos / 1_000_000L
                val manualSlider = latestSliderProvider()
                val player = playerConnection.player
                val isSeeking = manualSlider != null
                val rawPos = manualSlider ?: player.currentPosition
                val isBuffering = player.playbackState == Player.STATE_BUFFERING
                val isPlaying = !isSeeking && player.isPlaying && player.playbackState == Player.STATE_READY
                val speed = player.playbackParameters.speed
                val jumped = isFirstFrame ||
                    abs(rawPos - previousRawPos) > LyricsPlayheadTracker.DEFAULT_SEEK_SNAP_THRESHOLD_MS

                tracker.updateSample(
                    playerPositionMs = rawPos,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    playbackSpeed = speed,
                    frameTimeMs = frameMs,
                    forceSnap = isSeeking || jumped,
                )
                isFirstFrame = false
                previousRawPos = rawPos
                positionState.longValue = tracker.effectivePositionMs(latestOffsetProvider().toLong())
            }
        }
    }

    return remember(positionState) { LyricsPlayheadState(positionState) }
}
