package com.sonara.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.sonara.music.eq.data.ReverbConfig
import java.nio.ByteBuffer
import kotlin.math.roundToInt

@UnstableApi
class RegaReverbAudioProcessor : BaseAudioProcessor() {
    private var sampleRate = 0
    private var channelCount = 0

    @Volatile
    private var currentConfig: ReverbConfig = ReverbConfig()

    private var combFiltersL: Array<CombFilter> = emptyArray()
    private var combFiltersR: Array<CombFilter> = emptyArray()
    private var allpassFiltersL: Array<AllPassFilter> = emptyArray()
    private var allpassFiltersR: Array<AllPassFilter> = emptyArray()

    private var feedback = 0.8f
    private var damp = 0.2f
    private var wet1 = 0.3f
    private var wet2 = 0.0f
    private var dry = 1.0f
    private var wet = 0.3f

    fun updateConfig(config: ReverbConfig) {
        currentConfig = config
        recalculateParameters(config)
    }

    fun getConfig(): ReverbConfig = currentConfig

    fun isProcessorEnabled(): Boolean = currentConfig.enabled

    private fun recalculateParameters(config: ReverbConfig) {
        val scaledRoom = (config.roomSize * 0.28f + 0.70f).coerceIn(0.0f, 0.985f)
        feedback = (scaledRoom * (0.8f + 0.2f * config.decay.coerceIn(0.0f, 1.0f))).coerceIn(0.1f, 0.985f)
        damp = (config.damping.coerceIn(0.0f, 1.0f) * 0.4f).coerceIn(0.0f, 0.95f)

        wet = config.wetLevel.coerceIn(0.0f, 1.0f)
        dry = config.dryLevel.coerceIn(0.0f, 1.0f)
        val width = config.width.coerceIn(0.0f, 1.0f)
        wet1 = wet * (width * 0.5f + 0.5f)
        wet2 = wet * ((1.0f - width) * 0.5f)
    }

    private fun initFilters(rate: Int) {
        val scale = rate / 44100.0

        val combTunings = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
        val allpassTunings = intArrayOf(556, 441, 341, 225)
        val stereoSpread = 23

        combFiltersL = Array(combTunings.size) { i ->
            val size = (combTunings[i] * scale).roundToInt().coerceAtLeast(16)
            CombFilter(size)
        }
        combFiltersR = Array(combTunings.size) { i ->
            val size = ((combTunings[i] + stereoSpread) * scale).roundToInt().coerceAtLeast(16)
            CombFilter(size)
        }

        allpassFiltersL = Array(allpassTunings.size) { i ->
            val size = (allpassTunings[i] * scale).roundToInt().coerceAtLeast(8)
            AllPassFilter(size)
        }
        allpassFiltersR = Array(allpassTunings.size) { i ->
            val size = ((allpassTunings[i] + stereoSpread) * scale).roundToInt().coerceAtLeast(8)
            AllPassFilter(size)
        }

        recalculateParameters(currentConfig)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount > 2 || inputAudioFormat.channelCount < 1) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        initFilters(sampleRate)

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val output = replaceOutputBuffer(inputBuffer.remaining())
        val config = currentConfig

        if (!config.enabled || combFiltersL.isEmpty()) {
            output.put(inputBuffer)
        } else {
            processAudioBuffer(inputBuffer, output)
        }
        output.flip()
    }

    private fun processAudioBuffer(input: ByteBuffer, output: ByteBuffer) {
        val fb = feedback
        val dm = damp
        val w1 = wet1
        val w2 = wet2
        val d = dry
        val w = wet

        val combsL = combFiltersL
        val combsR = combFiltersR
        val apsL = allpassFiltersL
        val apsR = allpassFiltersR

        val numCombs = combsL.size
        val numAps = apsL.size

        val frames = input.remaining() / 2 / channelCount
        repeat(frames) {
            when (channelCount) {
                1 -> {
                    var inMono = input.getShort().toFloat() / 32768.0f
                    if (inMono.isNaN() || inMono.isInfinite()) inMono = 0.0f

                    var outL = 0.0f
                    var outR = 0.0f
                    var i = 0
                    while (i < numCombs) {
                        outL += combsL[i].process(inMono, fb, dm)
                        outR += combsR[i].process(inMono, fb, dm)
                        i++
                    }
                    var j = 0
                    while (j < numAps) {
                        outL = apsL[j].process(outL)
                        outR = apsR[j].process(outR)
                        j++
                    }

                    var finalMono = d * inMono + w * (outL + outR) * 0.5f
                    if (finalMono.isNaN() || finalMono.isInfinite()) finalMono = 0.0f

                    output.putShort(
                        (finalMono * 32768.0f)
                            .coerceIn(-32768.0f, 32767.0f)
                            .toInt()
                            .toShort()
                    )
                }

                2 -> {
                    var inL = input.getShort().toFloat() / 32768.0f
                    var inR = input.getShort().toFloat() / 32768.0f

                    if (inL.isNaN() || inL.isInfinite()) inL = 0.0f
                    if (inR.isNaN() || inR.isInfinite()) inR = 0.0f

                    val mixedInput = (inL + inR) * 0.5f

                    var outL = 0.0f
                    var outR = 0.0f
                    var i = 0
                    while (i < numCombs) {
                        outL += combsL[i].process(mixedInput, fb, dm)
                        outR += combsR[i].process(mixedInput, fb, dm)
                        i++
                    }
                    var j = 0
                    while (j < numAps) {
                        outL = apsL[j].process(outL)
                        outR = apsR[j].process(outR)
                        j++
                    }

                    var finalL = d * inL + w1 * outL + w2 * outR
                    var finalR = d * inR + w1 * outR + w2 * outL

                    if (finalL.isNaN() || finalL.isInfinite()) finalL = 0.0f
                    if (finalR.isNaN() || finalR.isInfinite()) finalR = 0.0f

                    output.putShort(
                        (finalL * 32768.0f)
                            .coerceIn(-32768.0f, 32767.0f)
                            .toInt()
                            .toShort()
                    )
                    output.putShort(
                        (finalR * 32768.0f)
                            .coerceIn(-32768.0f, 32767.0f)
                            .toInt()
                            .toShort()
                    )
                }
            }
        }
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        resetFilterBuffers()
    }

    @Deprecated("Deprecated in BaseAudioProcessor")
    override fun onFlush() {
        resetFilterBuffers()
    }

    override fun onReset() {
        sampleRate = 0
        channelCount = 0
        resetFilterBuffers()
    }

    private fun resetFilterBuffers() {
        combFiltersL.forEach { it.reset() }
        combFiltersR.forEach { it.reset() }
        allpassFiltersL.forEach { it.reset() }
        allpassFiltersR.forEach { it.reset() }
    }

    private class CombFilter(val bufSize: Int) {
        val buffer = FloatArray(bufSize)
        var bufIdx = 0
        var filterStore = 0.0f

        fun reset() {
            buffer.fill(0.0f)
            bufIdx = 0
            filterStore = 0.0f
        }

        fun process(input: Float, fb: Float, dm: Float): Float {
            val output = buffer[bufIdx]
            filterStore = (output * (1.0f - dm)) + (filterStore * dm)
            if (filterStore > -1e-15f && filterStore < 1e-15f) {
                filterStore = 0.0f
            }
            val newSample = input + (filterStore * fb)
            buffer[bufIdx] = if (newSample.isNaN() || newSample.isInfinite()) 0.0f else newSample
            bufIdx = if (bufIdx + 1 >= bufSize) 0 else bufIdx + 1
            return output
        }
    }

    private class AllPassFilter(val bufSize: Int) {
        val buffer = FloatArray(bufSize)
        var bufIdx = 0

        fun reset() {
            buffer.fill(0.0f)
            bufIdx = 0
        }

        fun process(input: Float): Float {
            val bufOut = buffer[bufIdx]
            val output = -input + bufOut
            val newSample = input + (bufOut * 0.5f)
            buffer[bufIdx] = if (newSample.isNaN() || newSample.isInfinite()) 0.0f else newSample
            bufIdx = if (bufIdx + 1 >= bufSize) 0 else bufIdx + 1
            return output
        }
    }
}
