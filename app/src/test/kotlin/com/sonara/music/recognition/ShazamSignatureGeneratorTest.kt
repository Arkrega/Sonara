package com.sonara.music.recognition

import com.sonara.music.lyrics.LyricsUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
class ShazamSignatureGeneratorTest {

    @Test
    fun rustAndKotlinProduceIdenticalShazamSignatures() {
        val sampleCount = 16_000 * 3
        val steadyPcm = ByteArray(sampleCount * 2)
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / 16_000.0
            val amp =
                0.25 * sin(2.0 * PI * 440.0 * t) +
                    0.25 * sin(2.0 * PI * 1000.0 * t) +
                    0.25 * sin(2.0 * PI * 2500.0 * t) +
                    0.25 * sin(2.0 * PI * 4200.0 * t)
            val s = (amp * 20_000.0).roundToInt().toShort().toInt()
            steadyPcm[i * 2] = (s and 0xFF).toByte()
            steadyPcm[i * 2 + 1] = ((s ushr 8) and 0xFF).toByte()
        }
        assertEquals(
            "data:audio/vnd.shazam.sig;base64,gCX+yhPgOVIIAAAAAJwRlAAAAAAAAAAAAAAAAAAAABgAAAAAAAAAAIDKAAAAAHwAAAAAQAgAAAA=",
            VibraSignature.fromI16(steadyPcm),
        )

        val pulsedCount = 16_000 * 4
        val pulsedPcm = ByteArray(pulsedCount * 2)
        for (i in 0 until pulsedCount) {
            val t = i.toDouble() / 16_000.0
            val phase = (i % 4800).toDouble() / 16_000.0
            val env = if (phase < 0.08) sin(PI * phase / 0.08) else 0.0
            val amp =
                env * (
                    0.25 * sin(2.0 * PI * 380.0 * t) +
                        0.25 * sin(2.0 * PI * 950.0 * t) +
                        0.25 * sin(2.0 * PI * 2400.0 * t) +
                        0.25 * sin(2.0 * PI * 4400.0 * t)
                    )
            val s = (amp * 24_000.0).roundToInt().toShort().toInt()
            pulsedPcm[i * 2] = (s and 0xFF).toByte()
            pulsedPcm[i * 2 + 1] = ((s ushr 8) and 0xFF).toByte()
        }
        assertEquals(
            "data:audio/vnd.shazam.sig;base64,gCX+yvJ1zTEAAQAAAJwRlAAAAAAAAAAAAAAAAAAAABgAAAAAAAAAAAAJAQAAAHwAAAAAQAABAABAAANgHgAAAAxTeygMS1N7KAxLU3soDEtTeygMS1N7KAxLU3soDAAAQQADYB4AAAAMOXtmHks5e2YeSzl7Zh5LOXtmHks5e2YeSzl7Zh4AAEIAA2B4AAAABxkKFG4FoHvNTAVcCo5pHGQKd2olGQoUbgWge81MBVwKjmkcZAp3aiUZChRuBaB7zUwFXAqOaRxkCndqJRkKFG4FoHvNTAVcCo5pHGQKd2olGQoUbgWge81MBVwKjmkcZAp3aiUZChRuBaB7zUwFXAqOaRxkCndqQwADYB4AAAAMoHvNjEuge82MS6B7zYxLoHvNjEuge82MS6B7zYwAAA==",
            VibraSignature.fromI16(pulsedPcm),
        )
    }

    @Test
    fun lyricsTitleCleanAndCreditFilterParity() {
        assertEquals(
            "Bohemian Rhapsody",
            LyricsUtils.cleanTitleForSearch("Bohemian Rhapsody (Remastered 2011) [Official Audio]"),
        )
        assertEquals(
            "Song Title - Live",
            LyricsUtils.cleanTitleForSearch("  Song Title (feat. Someone) - Live  "),
        )
        assertEquals(
            "Unclosed (Title",
            LyricsUtils.cleanTitleForSearch("Unclosed (Title"),
        )
        assertEquals(
            "Mixed Normal",
            LyricsUtils.cleanTitleForSearch("Mixed [a) Normal"),
        )

        val input =
            "[00:00.00]Synced by John\n[00:01.00]{agent:v1}v1: Lyrics by Someone\n[00:02.50]{bg}[bg: synced by someone]\n[00:05.00]First real lyric line\n[00:09.123]v2: Second real lyric line\n[Synced by Tester]"
        val expected =
            "[00:02.50]{bg}[bg: synced by someone]\n[00:05.00]First real lyric line\n[00:09.123]v2: Second real lyric line"
        assertEquals(expected, LyricsUtils.filterLyricsCreditLines(input))
    }
}
