package com.sonara.music.utils

import java.nio.ByteBuffer

object SonaraNativeCore {
    val isAvailable: Boolean =
        runCatching {
            System.loadLibrary("sonara_core")
            true
        }.getOrDefault(false)

    fun generateShazamSignature(samples: ByteArray): String? {
        if (!isAvailable) return null
        return runCatching { nativeGenerateShazamSignature(samples) }.getOrNull()
    }

    fun cleanTitleForSearch(title: String): String? {
        if (!isAvailable) return null
        return runCatching { nativeCleanTitleForSearch(title) }.getOrNull()
    }

    fun filterLyricsCreditLines(lyrics: String): String? {
        if (!isAvailable) return null
        return runCatching { nativeFilterLyricsCreditLines(lyrics) }.getOrNull()
    }

    fun processEqPcm16Direct(
        input: ByteBuffer,
        inputPos: Int,
        output: ByteBuffer,
        outputPos: Int,
        frameCount: Int,
        channels: Int,
        coeffs: DoubleArray,
        states: DoubleArray,
        preampGain: Double,
    ): Boolean {
        if (!isAvailable || !input.isDirect || !output.isDirect) return false
        return nativeProcessEqPcm16Direct(
            input,
            inputPos,
            output,
            outputPos,
            frameCount,
            channels,
            coeffs,
            states,
            preampGain,
        )
    }

    fun processGainDirect(
        input: ByteBuffer,
        inputPos: Int,
        output: ByteBuffer,
        outputPos: Int,
        sampleCount: Int,
        encoding: Int,
        linearGain: Double,
    ): Boolean {
        if (!isAvailable || !input.isDirect || !output.isDirect) return false
        return nativeProcessGainDirect(
            input,
            inputPos,
            output,
            outputPos,
            sampleCount,
            encoding,
            linearGain,
        )
    }

    fun createReverb(sampleRate: Int): Long {
        if (!isAvailable || sampleRate <= 0) return 0L
        return nativeCreateReverb(sampleRate)
    }

    fun resetReverb(handle: Long) {
        if (!isAvailable || handle == 0L) return
        nativeResetReverb(handle)
    }

    fun destroyReverb(handle: Long) {
        if (!isAvailable || handle == 0L) return
        nativeDestroyReverb(handle)
    }

    fun processReverbDirect(
        handle: Long,
        input: ByteBuffer,
        inputPos: Int,
        output: ByteBuffer,
        outputPos: Int,
        frameCount: Int,
        channels: Int,
        fb: Float,
        damp: Float,
        dry: Float,
        wet: Float,
        wet1: Float,
        wet2: Float,
    ): Boolean {
        if (!isAvailable || handle == 0L || !input.isDirect || !output.isDirect) return false
        return nativeProcessReverbDirect(
            handle,
            input,
            inputPos,
            output,
            outputPos,
            frameCount,
            channels,
            fb,
            damp,
            dry,
            wet,
            wet1,
            wet2,
        )
    }

    fun computeSpectrumBarsDirect(
        input: ByteBuffer,
        inputPos: Int,
        byteLen: Int,
        channels: Int,
        sampleRate: Int,
        outBars: FloatArray,
    ): Boolean {
        if (!isAvailable || !input.isDirect) return false
        return nativeComputeSpectrumBarsDirect(input, inputPos, byteLen, channels, sampleRate, outBars)
    }

    @JvmStatic
    private external fun nativeGenerateShazamSignature(samples: ByteArray): String?

    @JvmStatic
    private external fun nativeCleanTitleForSearch(title: String): String?

    @JvmStatic
    private external fun nativeFilterLyricsCreditLines(lyrics: String): String?

    @JvmStatic
    private external fun nativeProcessEqPcm16Direct(
        input: ByteBuffer,
        inputPos: Int,
        output: ByteBuffer,
        outputPos: Int,
        frameCount: Int,
        channels: Int,
        coeffs: DoubleArray,
        states: DoubleArray,
        preampGain: Double,
    ): Boolean

    @JvmStatic
    private external fun nativeProcessGainDirect(
        input: ByteBuffer,
        inputPos: Int,
        output: ByteBuffer,
        outputPos: Int,
        sampleCount: Int,
        encoding: Int,
        linearGain: Double,
    ): Boolean

    @JvmStatic
    private external fun nativeCreateReverb(sampleRate: Int): Long

    @JvmStatic
    private external fun nativeResetReverb(handle: Long)

    @JvmStatic
    private external fun nativeDestroyReverb(handle: Long)

    @JvmStatic
    private external fun nativeProcessReverbDirect(
        handle: Long,
        input: ByteBuffer,
        inputPos: Int,
        output: ByteBuffer,
        outputPos: Int,
        frameCount: Int,
        channels: Int,
        fb: Float,
        damp: Float,
        dry: Float,
        wet: Float,
        wet1: Float,
        wet2: Float,
    ): Boolean

    @JvmStatic
    private external fun nativeComputeSpectrumBarsDirect(
        input: ByteBuffer,
        inputPos: Int,
        byteLen: Int,
        channels: Int,
        sampleRate: Int,
        outBars: FloatArray,
    ): Boolean
}
