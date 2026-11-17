package com.metrolist.music.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LyricsPrecisionEngineTest {

    @Test
    fun classifyRawLyricsDistinguishesRichLineAndUnsynced() {
        val richLrc = """
            [00:10.00]Hello world
            <Hello:10.0:10.5|world:10.6:11.2>
            [00:12.00]Second line
            <Second:12.0:12.4|line:12.5:13.0>
        """.trimIndent()
        val lineOnlyLrc = """
            [00:10.00]Hello world
            [00:12.00]Second line
        """.trimIndent()
        val unsynced = """
            Hello world
            Second line
        """.trimIndent()

        assertEquals(LyricsSyncType.RICH_SYNCED, LyricsQualityAnalyzer.classifyLyrics(richLrc))
        assertEquals(LyricsSyncType.LINE_SYNCED, LyricsQualityAnalyzer.classifyLyrics(lineOnlyLrc))
        assertEquals(LyricsSyncType.UNSYNCED, LyricsQualityAnalyzer.classifyLyrics(unsynced))
        assertEquals(LyricsSyncType.UNSYNCED, LyricsQualityAnalyzer.classifyLyrics("LYRICS_NOT_FOUND"))
    }

    @Test
    fun lineSyncedLyricsDoNotProduceSyntheticWords() {
        val lineOnlyLrc = """
            [00:05.00]First line here
            [00:09.00]Second line here
        """.trimIndent()

        val parsed = LyricsUtils.parseLyrics(lineOnlyLrc)
        assertTrue(parsed.isNotEmpty())
        assertTrue(parsed.all { it.words.isNullOrEmpty() })
        assertEquals(LyricsSyncType.LINE_SYNCED, LyricsQualityAnalyzer.classifyLyrics(lineOnlyLrc, parsed))
    }

    @Test
    fun candidateScorerPrefersRichSyncedOverLineAndUnsynced() {
        val rich = """
            [00:10.00]One two
            <One:10.0:10.5|two:10.6:11.0>
            [00:12.00]Three four
            <Three:12.0:12.5|four:12.6:13.0>
            [00:14.00]Five six
            <Five:14.0:14.4|six:14.5:15.0>
        """.trimIndent()
        val line = """
            [00:10.00]One two
            [00:12.00]Three four
            [00:14.00]Five six
        """.trimIndent()
        val plain = """
            One two
            Three four
            Five six
        """.trimIndent()

        val richScore = LyricsQualityAnalyzer.evaluateCandidate(
            rawLyrics = rich,
            providerName = "Paxsenix",
            providerPriorityIndex = 2,
            totalProviders = 6,
            trackDurationSec = 180,
            isExactVideoIdSource = false,
        )
        val lineScore = LyricsQualityAnalyzer.evaluateCandidate(
            rawLyrics = line,
            providerName = "LrcLib",
            providerPriorityIndex = 0,
            totalProviders = 6,
            trackDurationSec = 180,
            isExactVideoIdSource = false,
        )
        val plainScore = LyricsQualityAnalyzer.evaluateCandidate(
            rawLyrics = plain,
            providerName = "YouTube Music",
            providerPriorityIndex = 0,
            totalProviders = 6,
            trackDurationSec = 180,
            isExactVideoIdSource = true,
        )

        assertTrue(richScore.isValid && lineScore.isValid && plainScore.isValid)
        assertTrue(richScore.score > lineScore.score)
        assertTrue(lineScore.score > plainScore.score)
    }

    @Test
    fun candidateScorerRejectsEmptyAndPenalizesWildDurationMismatch() {
        val invalid = LyricsQualityAnalyzer.evaluateCandidate(
            rawLyrics = "Only one line",
            providerName = "LrcLib",
            providerPriorityIndex = 0,
            totalProviders = 4,
            trackDurationSec = 180,
            isExactVideoIdSource = false,
        )
        assertFalse(invalid.isValid)

        val mismatched = LyricsQualityAnalyzer.evaluateCandidate(
            rawLyrics = "[05:40.00]Line one\n[05:50.00]Line two",
            providerName = "LrcLib",
            providerPriorityIndex = 0,
            totalProviders = 4,
            trackDurationSec = 120,
            isExactVideoIdSource = false,
        )
        val matched = LyricsQualityAnalyzer.evaluateCandidate(
            rawLyrics = "[01:40.00]Line one\n[01:50.00]Line two",
            providerName = "LrcLib",
            providerPriorityIndex = 0,
            totalProviders = 4,
            trackDurationSec = 120,
            isExactVideoIdSource = false,
        )
        assertTrue(matched.score > mismatched.score)
    }

    @Test
    fun playheadTrackerRespectsPlaybackSpeedPauseBufferingAndSeekSnap() {
        val tracker = LyricsPlayheadTracker(maxExtrapolationMs = 250L, seekSnapThresholdMs = 450L)

        val t0 = tracker.updateSample(
            playerPositionMs = 10_000L,
            isPlaying = true,
            isBuffering = false,
            playbackSpeed = 1.0f,
            frameTimeMs = 1_000L,
            forceSnap = true,
        )
        assertEquals(10_000L, t0)

        val t15x = tracker.updateSample(
            playerPositionMs = 10_000L,
            isPlaying = true,
            isBuffering = false,
            playbackSpeed = 1.5f,
            frameTimeMs = 1_100L,
        )
        assertEquals(10_150L, t15x)

        val capped = tracker.updateSample(
            playerPositionMs = 10_000L,
            isPlaying = true,
            isBuffering = false,
            playbackSpeed = 1.0f,
            frameTimeMs = 2_000L,
        )
        assertEquals(10_250L, capped)

        val bufferingPos = tracker.updateSample(
            playerPositionMs = 10_240L,
            isPlaying = true,
            isBuffering = true,
            playbackSpeed = 1.0f,
            frameTimeMs = 2_050L,
        )
        assertEquals(10_240L, bufferingPos)

        val pausedPos = tracker.updateSample(
            playerPositionMs = 10_240L,
            isPlaying = false,
            isBuffering = false,
            playbackSpeed = 1.0f,
            frameTimeMs = 2_500L,
        )
        assertEquals(10_240L, pausedPos)

        val backwardSeek = tracker.updateSample(
            playerPositionMs = 4_000L,
            isPlaying = true,
            isBuffering = false,
            playbackSpeed = 1.0f,
            frameTimeMs = 2_516L,
        )
        assertEquals(4_000L, backwardSeek)
    }

    @Test
    fun playheadOffsetAppliesImmediatelyWithoutMutatingRawPosition() {
        val tracker = LyricsPlayheadTracker()
        tracker.updateSample(
            playerPositionMs = 5_000L,
            isPlaying = false,
            isBuffering = false,
            playbackSpeed = 1.0f,
            frameTimeMs = 100L,
            forceSnap = true,
        )

        assertEquals(5_000L, tracker.currentPositionMs())
        assertEquals(5_350L, tracker.effectivePositionMs(350L))
        assertEquals(4_650L, tracker.effectivePositionMs(-350L))
        assertEquals(0L, tracker.effectivePositionMs(-10_000L))
        assertEquals(5_000L, tracker.currentPositionMs())
    }
}
