package com.sonara.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.sonara.music.eq.data.ParametricEQ
import com.sonara.music.eq.data.ParametricEQBand
import com.sonara.music.utils.SonaraNativeCore
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * Applies AutoEQ parametric profiles with biquad filters.
 */
@UnstableApi
class CustomEqualizerAudioProcessor : BaseAudioProcessor() {
    private var sampleRate = 0
    private var channelCount = 0
    private var equalizerEnabled = false
    private var filters: List<BiquadFilter> = emptyList()
    private var nativeCoeffs = DoubleArray(0)
    private var nativeStates = DoubleArray(0)
    private var preampGain = 1.0
    private var pendingProfile: ParametricEQ? = null

    @Synchronized
    fun applyProfile(parametricEQ: ParametricEQ) {
        if (sampleRate == 0) {
            Timber.tag(TAG)
                .d("Audio processor not configured yet. Storing profile as pending with ${parametricEQ.bands.size} bands")
            pendingProfile = parametricEQ
            return
        }

        preampGain = 10.0.pow(parametricEQ.preamp / 20.0)
        createFilters(parametricEQ.bands)
        equalizerEnabled = true
        filters.forEach { it.reset() }
        nativeStates.fill(0.0)

        Timber.tag(TAG)
            .d("Applied EQ profile with ${filters.size} bands and ${parametricEQ.preamp} dB preamp")
    }

    @Synchronized
    fun disable() {
        equalizerEnabled = false
        filters = emptyList()
        nativeCoeffs = DoubleArray(0)
        nativeStates = DoubleArray(0)
        preampGain = 1.0
        pendingProfile = null
        Timber.tag(TAG).d("Equalizer disabled")
    }

    fun isEnabled(): Boolean = equalizerEnabled

    private fun createFilters(bands: List<ParametricEQBand>) {
        val created =
            bands
                .filter { it.enabled && it.frequency < sampleRate / 2.0 }
                .map { band ->
                    BiquadFilter(
                        sampleRate = sampleRate,
                        frequency = band.frequency,
                        gain = band.gain,
                        q = band.q,
                        filterType = band.filterType,
                    )
                }
        filters = created
        val coeffs = DoubleArray(created.size * 5)
        for (i in created.indices) {
            created[i].writeCoeffsTo(coeffs, i * 5)
        }
        nativeCoeffs = coeffs
        nativeStates = DoubleArray(created.size * 8)

        Timber.tag(TAG)
            .d("Created ${filters.size} biquad filters from ${bands.size} bands (PK/LSC/HSC)")
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        Timber.tag(TAG).d(
            "Configured: sampleRate=$sampleRate, channels=$channelCount, encoding=${inputAudioFormat.encoding}",
        )

        pendingProfile?.let { profile ->
            preampGain = 10.0.pow(profile.preamp / 20.0)
            createFilters(profile.bands)
            equalizerEnabled = true
            pendingProfile = null
            Timber.tag(TAG)
                .d("Applied pending profile with ${filters.size} bands and ${profile.preamp} dB preamp")
        }

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val output = replaceOutputBuffer(inputBuffer.remaining())
        if (!equalizerEnabled || filters.isEmpty()) {
            output.put(inputBuffer)
        } else {
            processAudioBuffer(inputBuffer, output)
        }
        output.flip()
    }

    private fun processAudioBuffer(input: ByteBuffer, output: ByteBuffer) {
        val frameCount = input.remaining() / 2 / channelCount
        if (frameCount <= 0) return

        val byteCount = frameCount * channelCount * 2
        if (input.isDirect && output.isDirect && nativeCoeffs.isNotEmpty() &&
            SonaraNativeCore.processEqPcm16Direct(
                input = input,
                inputPos = input.position(),
                output = output,
                outputPos = output.position(),
                frameCount = frameCount,
                channels = channelCount,
                coeffs = nativeCoeffs,
                states = nativeStates,
                preampGain = preampGain,
            )
        ) {
            input.position(input.position() + byteCount)
            output.position(output.position() + byteCount)
            return
        }

        input.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        val filterList = filters
        repeat(frameCount) {
            when (channelCount) {
                1 -> {
                    var processed = input.getShort().toDouble() / 32768.0
                    for (i in filterList.indices) {
                        processed = filterList[i].processLeft(processed)
                    }
                    processed *= preampGain
                    output.putShort(
                        (processed * 32768.0)
                            .coerceIn(-32768.0, 32767.0)
                            .toInt()
                            .toShort(),
                    )
                }

                2 -> {
                    var left = input.getShort().toDouble() / 32768.0
                    var right = input.getShort().toDouble() / 32768.0
                    for (i in filterList.indices) {
                        val f = filterList[i]
                        left = f.processLeft(left)
                        right = f.processRight(right)
                    }
                    output.putShort(
                        (left * preampGain * 32768.0)
                            .coerceIn(-32768.0, 32767.0)
                            .toInt()
                            .toShort(),
                    )
                    output.putShort(
                        (right * preampGain * 32768.0)
                            .coerceIn(-32768.0, 32767.0)
                            .toInt()
                            .toShort(),
                    )
                }
            }
        }
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        filters.forEach { it.reset() }
        nativeStates.fill(0.0)
    }

    override fun onReset() {
        sampleRate = 0
        channelCount = 0
        filters.forEach { it.reset() }
        nativeStates.fill(0.0)
    }

    private companion object {
        const val TAG = "CustomEqualizerAudioProcessor"
    }
}
