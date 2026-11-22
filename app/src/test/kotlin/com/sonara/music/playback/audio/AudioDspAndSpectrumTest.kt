package com.sonara.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.sonara.music.eq.audio.CustomEqualizerAudioProcessor
import com.sonara.music.eq.data.FilterType
import com.sonara.music.eq.data.ParametricEQ
import com.sonara.music.eq.data.ParametricEQBand
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class AudioDspAndSpectrumTest {

    @Test
    @Suppress("DEPRECATION")
    fun `custom equalizer with active bands processes stereo and reuses buffer`() {
        val eq = CustomEqualizerAudioProcessor()
        eq.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        eq.flush()
        eq.applyProfile(
            ParametricEQ(
                preamp = -2.0,
                bands =
                    listOf(
                        ParametricEQBand(100.0, 4.0, 1.0, FilterType.LSC, true),
                        ParametricEQBand(1000.0, 3.0, 1.41, FilterType.PK, true),
                        ParametricEQBand(8000.0, -3.0, 1.0, FilterType.HSC, true),
                    ),
            ),
        )

        val input1 = createPcm16StereoSineBuffer(frames = 1024, freqHz = 1000.0)
        eq.queueInput(input1)
        val out1 = eq.output
        assertEquals(1024 * 4, out1.remaining())
        var nonZero = false
        while (out1.hasRemaining()) {
            if (out1.get() != 0.toByte()) {
                nonZero = true
            }
        }
        assertTrue(nonZero)

        val input2 = createPcm16StereoSineBuffer(frames = 1024, freqHz = 1000.0)
        eq.queueInput(input2)
        val out2 = eq.output
        assertSame(out1, out2)
        assertEquals(1024 * 4, out2.remaining())
    }

    @Test
    @Suppress("DEPRECATION")
    fun `volume normalization passthrough and all encodings gain scaling`() {
        val processor = VolumeNormalizationAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.enabled = false

        val passIn = createPcm16StereoSineBuffer(frames = 256, freqHz = 440.0)
        val expectedPass = ByteArray(passIn.remaining()).also {
            passIn.mark()
            passIn.get(it)
            passIn.reset()
        }
        processor.queueInput(passIn)
        val passOut = processor.output
        val actualPass = ByteArray(passOut.remaining()).also(passOut::get)
        assertArrayEquals(expectedPass, actualPass)

        processor.enabled = true
        processor.setTargetGain(602)

        val pcm16Buf =
            ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN).apply {
                putShort(10000)
                putShort(-30000)
                flip()
            }
        processor.queueInput(pcm16Buf)
        val out16 = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        assertTrue(out16.getShort().toInt() > 19000)
        assertEquals((-32768).toShort(), out16.getShort())

        processor.configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_FLOAT))
        processor.flush()
        val floatBuf =
            ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN).apply {
                putFloat(0.25f)
                putFloat(-0.8f)
                flip()
            }
        processor.queueInput(floatBuf)
        val outFloat = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        assertTrue(outFloat.getFloat() > 0.45f)
        assertEquals(-1.0f, outFloat.getFloat(), 0.001f)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `spectrum processor passes audio unchanged and publishes bars when active`() {
        val processor = AudioSpectrumProcessor()
        processor.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()

        AudioSpectrumBus.clear()
        AudioSpectrumBus.isActive = true
        try {
            val in1 = createPcm16StereoSineBuffer(frames = 512, freqHz = 1000.0)
            val expected = ByteArray(in1.remaining()).also {
                in1.mark()
                in1.get(it)
                in1.reset()
            }

            processor.queueInput(in1)
            val out1 = processor.output
            val actual = ByteArray(out1.remaining()).also(out1::get)
            assertArrayEquals(expected, actual)

            val bars = AudioSpectrumBus.bars.value
            assertEquals(AudioSpectrumBus.BAR_COUNT, bars.size)
            assertTrue(bars.any { it > 0.05f })
            assertTrue(bars.all { it in 0f..1f })

            processor.queueInput(createPcm16StereoSineBuffer(frames = 512, freqHz = 1000.0))
            assertSame(out1, processor.output)
        } finally {
            AudioSpectrumBus.isActive = false
            AudioSpectrumBus.clear()
        }
    }

    private fun createPcm16StereoSineBuffer(
        frames: Int,
        freqHz: Double,
    ): ByteBuffer =
        ByteBuffer
            .allocateDirect(frames * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                for (i in 0 until frames) {
                    val sample = (sin(2.0 * PI * freqHz * i / 48_000.0) * 16_000.0).toInt().toShort()
                    putShort(sample)
                    putShort(sample)
                }
                flip()
            }
}
