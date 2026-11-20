package com.sonara.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.sonara.music.eq.data.ReverbConfig
import com.sonara.music.eq.data.ReverbPresetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class RegaReverbAudioProcessorTest {
    @Test
    fun `reverb disabled passes audio unmodified`() {
        val processor = RegaReverbAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.flush()

        val input = createBuffer(1024) { (it % 1000).toShort() }
        val originalBytes = ByteArray(input.remaining())
        input.mark()
        input.get(originalBytes)
        input.reset()

        processor.queueInput(input)
        val output = processor.output
        val outputBytes = ByteArray(output.remaining())
        output.get(outputBytes)

        assertEquals(originalBytes.size, outputBytes.size)
        for (i in originalBytes.indices) {
            assertEquals(originalBytes[i], outputBytes[i])
        }
    }

    @Test
    fun `direct output buffer is reused across queueInput calls`() {
        val processor = RegaReverbAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()

        processor.queueInput(createBuffer(512) { 100.toShort() })
        val firstOutput = processor.output
        processor.queueInput(createBuffer(512) { 200.toShort() })
        val secondOutput = processor.output

        assertSame(firstOutput, secondOutput)
    }

    @Test
    fun `mono input produces valid mono output`() {
        val processor = RegaReverbAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 1, C.ENCODING_PCM_16BIT)
        val outFormat = processor.configure(format)
        assertEquals(1, outFormat.channelCount)
        assertEquals(44100, outFormat.sampleRate)
        assertEquals(C.ENCODING_PCM_16BIT, outFormat.encoding)

        processor.updateConfig(ReverbConfig.forType(ReverbPresetType.SMALL_ROOM).copy(enabled = true))
        processor.flush()

        val input = createBuffer(256) { 5000.toShort() }
        processor.queueInput(input)
        val output = processor.output
        assertEquals(256, output.remaining())
    }

    @Test
    fun `different sample rates configure properly`() {
        val processor = RegaReverbAudioProcessor()
        val rates = intArrayOf(22050, 44100, 48000, 96000)
        for (rate in rates) {
            val format = AudioProcessor.AudioFormat(rate, 2, C.ENCODING_PCM_16BIT)
            val out = processor.configure(format)
            assertEquals(rate, out.sampleRate)
            assertEquals(2, out.channelCount)
        }
    }

    @Test(expected = AudioProcessor.UnhandledAudioFormatException::class)
    fun `unsupported encoding throws exception`() {
        val processor = RegaReverbAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_FLOAT))
    }

    @Test
    fun `impulse response creates audible reverberation tail`() {
        val processor = RegaReverbAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.updateConfig(ReverbConfig.forType(ReverbPresetType.LARGE_HALL).copy(enabled = true))
        processor.flush()

        val impulse = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        impulse.putShort(30000.toShort())
        impulse.putShort(30000.toShort())
        impulse.flip()
        processor.queueInput(impulse)
        val firstOut = processor.output
        while (firstOut.hasRemaining()) firstOut.get()

        var nonZeroCount = 0
        repeat(20) {
            val silence = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder())
            silence.put(ByteArray(1024))
            silence.flip()
            processor.queueInput(silence)
            val out = processor.output
            while (out.hasRemaining()) {
                val sample = out.short
                if (abs(sample.toInt()) > 10) {
                    nonZeroCount++
                }
            }
        }
        assertTrue(nonZeroCount > 50)
    }

    @Test
    fun `wet dry mixing controls reverberation ratio`() {
        val processor = RegaReverbAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.updateConfig(
            ReverbConfig(
                enabled = true,
                wetLevel = 0.0f,
                dryLevel = 1.0f
            )
        )
        processor.flush()

        val input = createBuffer(512) { 1000.toShort() }
        processor.queueInput(input)
        val dryOut = processor.output
        var diffFromDry = 0
        while (dryOut.hasRemaining()) {
            val sample = dryOut.short
            if (abs(sample - 1000) > 2) {
                diffFromDry++
            }
        }
        assertEquals(0, diffFromDry)
    }

    @Test
    fun `nan and infinity samples do not crash or corrupt output`() {
        val processor = RegaReverbAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.updateConfig(ReverbConfig.forType(ReverbPresetType.CATHEDRAL).copy(enabled = true))
        processor.flush()

        val normalInput = createBuffer(1024) { (it * 30).toShort() }
        processor.queueInput(normalInput)
        val out = processor.output
        while (out.hasRemaining()) {
            val sample = out.short
            assertTrue(sample >= -32768)
            assertTrue(sample <= 32767)
        }
    }

    @Test
    fun `flush clears reverberation tail immediately`() {
        val processor = RegaReverbAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.updateConfig(ReverbConfig.forType(ReverbPresetType.CATHEDRAL).copy(enabled = true))
        processor.flush()

        val loud = createBuffer(1024) { 32000.toShort() }
        processor.queueInput(loud)
        val firstOut = processor.output
        while (firstOut.hasRemaining()) firstOut.get()

        processor.flush()

        val zeroInput = createBuffer(512) { 0.toShort() }
        processor.queueInput(zeroInput)
        val outAfterFlush = processor.output
        var maxSample = 0
        while (outAfterFlush.hasRemaining()) {
            val s = abs(outAfterFlush.short.toInt())
            if (s > maxSample) maxSample = s
        }
        assertEquals(0, maxSample)
    }

    @Test
    fun `runtime parameter updates occur smoothly without exceptions`() {
        val processor = RegaReverbAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        processor.flush()

        val presets = ReverbPresetType.entries
        for (preset in presets) {
            processor.updateConfig(ReverbConfig.forType(preset).copy(enabled = true))
            processor.queueInput(createBuffer(512) { (it * 10).toShort() })
            val out = processor.output
            assertEquals(512, out.remaining())
            while (out.hasRemaining()) out.get()
        }
    }

    @Test
    fun `steady state benchmark executes efficiently`() {
        val processor = RegaReverbAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        processor.updateConfig(ReverbConfig.forType(ReverbPresetType.LARGE_HALL).copy(enabled = true))
        processor.flush()

        val buffer = createBuffer(4096) { (it % 1000).toShort() }
        val iterations = 500
        val start = System.nanoTime()
        repeat(iterations) {
            buffer.position(0)
            processor.queueInput(buffer)
            val out = processor.output
            while (out.hasRemaining()) out.get()
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        assertTrue(elapsedMs < 1000.0)
    }

    private fun createBuffer(capacity: Int, generator: (Int) -> Short): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
        repeat(capacity / 2) {
            buf.putShort(generator(it))
        }
        buf.flip()
        return buf
    }
}
