package com.sonara.music.playback.audio

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.sonara.music.utils.SonaraNativeCore
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

@UnstableApi
class AudioSpectrumProcessor : BaseAudioProcessor() {
    private var sampleRate = 0
    private var channelCount = 0
    private var lastEmitElapsedMs = 0L
    private val scratchBars = FloatArray(AudioSpectrumBus.BAR_COUNT)
    private val fallbackRe = FloatArray(FFT_SIZE)
    private val fallbackIm = FloatArray(FFT_SIZE)
    private val fallbackMag = FloatArray(FFT_SIZE / 2)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            (inputAudioFormat.channelCount != 1 && inputAudioFormat.channelCount != 2)
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val remaining = inputBuffer.remaining()
        val output = replaceOutputBuffer(remaining)

        if (AudioSpectrumBus.isActive && sampleRate > 0) {
            val nowMs = runCatching { SystemClock.elapsedRealtime() }.getOrElse { System.currentTimeMillis() }
            if (nowMs - lastEmitElapsedMs >= 33L) {
                lastEmitElapsedMs = nowMs
                val startPos = inputBuffer.position()
                val computed =
                    inputBuffer.isDirect &&
                        SonaraNativeCore.computeSpectrumBarsDirect(
                            input = inputBuffer,
                            inputPos = startPos,
                            byteLen = remaining,
                            channels = channelCount,
                            sampleRate = sampleRate,
                            outBars = scratchBars,
                        )
                if (!computed) {
                    computeSpectrumFallback(inputBuffer, startPos, remaining)
                }
                AudioSpectrumBus.publish(scratchBars)
            }
        }

        output.put(inputBuffer)
        output.flip()
    }

    private fun computeSpectrumFallback(
        input: ByteBuffer,
        startPos: Int,
        byteLen: Int,
    ) {
        val frameBytes = channelCount * 2
        val availableFrames = byteLen / frameBytes
        if (availableFrames <= 0) {
            scratchBars.fill(0f)
            return
        }
        val count = minOf(availableFrames, FFT_SIZE)
        fallbackRe.fill(0f)
        fallbackIm.fill(0f)

        for (i in 0 until count) {
            val off = startPos + i * frameBytes
            val mono =
                if (channelCount == 1) {
                    readShortLe(input, off) / 32768f
                } else {
                    val l = readShortLe(input, off).toFloat()
                    val r = readShortLe(input, off + 2).toFloat()
                    (l + r) * (0.5f / 32768f)
                }
            val win = 0.5f * (1f - cos((2.0 * PI * i / (FFT_SIZE - 1))).toFloat())
            fallbackRe[i] = mono * win
        }

        var j = 0
        for (i in 1 until FFT_SIZE) {
            var bit = FFT_SIZE shr 1
            while ((j and bit) != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = fallbackRe[i]
                fallbackRe[i] = fallbackRe[j]
                fallbackRe[j] = tr
                val ti = fallbackIm[i]
                fallbackIm[i] = fallbackIm[j]
                fallbackIm[j] = ti
            }
        }

        var len = 2
        while (len <= FFT_SIZE) {
            val half = len shr 1
            val ang = -PI / half
            val wBaseRe = cos(ang).toFloat()
            val wBaseIm = kotlin.math.sin(ang).toFloat()
            var i = 0
            while (i < FFT_SIZE) {
                var wRe = 1f
                var wIm = 0f
                for (k in 0 until half) {
                    val u = i + k
                    val v = u + half
                    val evenRe = fallbackRe[u]
                    val evenIm = fallbackIm[u]
                    val oddRe = fallbackRe[v] * wRe - fallbackIm[v] * wIm
                    val oddIm = fallbackRe[v] * wIm + fallbackIm[v] * wRe
                    fallbackRe[u] = evenRe + oddRe
                    fallbackIm[u] = evenIm + oddIm
                    fallbackRe[v] = evenRe - oddRe
                    fallbackIm[v] = evenIm - oddIm
                    val nextWRe = wRe * wBaseRe - wIm * wBaseIm
                    wIm = wRe * wBaseIm + wIm * wBaseRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }

        val halfSize = FFT_SIZE / 2
        for (bin in 1 until halfSize) {
            val r = fallbackRe[bin]
            val im = fallbackIm[bin]
            fallbackMag[bin] = sqrt(r * r + im * im) / 64f
        }

        val sr = sampleRate.toFloat()
        val hiLimit = minOf(sr * 0.48f, 14000f).coerceAtLeast(120f)
        val loLimit = minOf(60f, hiLimit * 0.5f)
        val ratio = (hiLimit / loLimit).toDouble()
        val barCount = scratchBars.size.toDouble()
        val normDenom = ln(13f)

        for (idx in scratchBars.indices) {
            val fLo = loLimit * Math.pow(ratio, idx / barCount).toFloat()
            val fHi = loLimit * Math.pow(ratio, (idx + 1) / barCount).toFloat()
            val bLo = (fLo * FFT_SIZE / sr).toInt().coerceIn(1, halfSize - 1)
            val bHi = kotlin.math.ceil((fHi * FFT_SIZE / sr).toDouble()).toInt().coerceIn(bLo, halfSize - 1)
            var peak = 0f
            for (bin in bLo..bHi) {
                if (fallbackMag[bin] > peak) {
                    peak = fallbackMag[bin]
                }
            }
            scratchBars[idx] = (ln(1f + peak * 12f) / normDenom).coerceIn(0f, 1f)
        }
    }

    private fun readShortLe(
        buf: ByteBuffer,
        offset: Int,
    ): Int {
        val lo = buf.get(offset).toInt() and 0xFF
        val hi = buf.get(offset + 1).toInt()
        return (hi shl 8) or lo
    }

    override fun onReset() {
        sampleRate = 0
        channelCount = 0
        lastEmitElapsedMs = 0L
        scratchBars.fill(0f)
    }

    private companion object {
        const val FFT_SIZE = 256
    }
}
