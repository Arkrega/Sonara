/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

enum class LyricsSyncType {
    RICH_SYNCED,
    LINE_SYNCED,
    UNSYNCED,
}

data class LyricsCandidateEvaluation(
    val syncType: LyricsSyncType,
    val score: Int,
    val lineCount: Int,
    val wordSyncedLineCount: Int,
    val isValid: Boolean,
)

object LyricsQualityAnalyzer {
    private val lineTimestampRegex = Regex("""\[\d{1,2}:\d{2}(?:\.\d{2,3})?]""")
    private val wordAngleBracketRegex = Regex("""<\d{1,2}:\d{2}(?:\.\d{2,3})?>""")
    private val wordBlockRegex = Regex("""<[^<>\n]+?:\d+(?:\.\d+)?:\d+(?:\.\d+)?(?:\|[^<>\n]+)*>""")
    private val bgTaggedLineRegex = Regex("""(?m)^\[bg:.*]$""")

    fun classifyLyrics(
        rawLyrics: String?,
        parsedEntries: List<LyricsEntry>? = null,
    ): LyricsSyncType {
        if (rawLyrics.isNullOrBlank() || rawLyrics.trim() == "LYRICS_NOT_FOUND") {
            return LyricsSyncType.UNSYNCED
        }

        val entries = parsedEntries ?: runCatching { LyricsUtils.parseLyrics(rawLyrics) }.getOrDefault(emptyList())
        val realEntries = entries.filter { it != LyricsEntry.HEAD_LYRICS_ENTRY && it.text.isNotBlank() }

        val hasRealWordTiming = realEntries.any { entry ->
            val words = entry.words
            !words.isNullOrEmpty() && words.any { it.endTime > it.startTime }
        }
        if (hasRealWordTiming) {
            return LyricsSyncType.RICH_SYNCED
        }

        if (wordBlockRegex.containsMatchIn(rawLyrics) || wordAngleBracketRegex.containsMatchIn(rawLyrics)) {
            return LyricsSyncType.RICH_SYNCED
        }

        val hasLineTiming = realEntries.any { it.time >= 0L } ||
            lineTimestampRegex.containsMatchIn(rawLyrics) ||
            bgTaggedLineRegex.containsMatchIn(rawLyrics)
        if (hasLineTiming) {
            return LyricsSyncType.LINE_SYNCED
        }

        return LyricsSyncType.UNSYNCED
    }

    fun evaluateCandidate(
        rawLyrics: String?,
        providerName: String,
        providerPriorityIndex: Int,
        totalProviders: Int,
        trackDurationSec: Int,
        isExactVideoIdSource: Boolean,
    ): LyricsCandidateEvaluation {
        if (rawLyrics.isNullOrBlank() || rawLyrics.trim() == "LYRICS_NOT_FOUND") {
            return LyricsCandidateEvaluation(
                syncType = LyricsSyncType.UNSYNCED,
                score = Int.MIN_VALUE,
                lineCount = 0,
                wordSyncedLineCount = 0,
                isValid = false,
            )
        }

        val entries = runCatching { LyricsUtils.parseLyrics(rawLyrics) }.getOrDefault(emptyList())
        val contentLines = if (entries.isNotEmpty()) {
            entries.filter { it != LyricsEntry.HEAD_LYRICS_ENTRY && it.text.isNotBlank() }
        } else {
            rawLyrics.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { LyricsEntry(-1L, it) }
        }

        val lineCount = contentLines.size
        if (lineCount < 2) {
            return LyricsCandidateEvaluation(
                syncType = LyricsSyncType.UNSYNCED,
                score = Int.MIN_VALUE,
                lineCount = lineCount,
                wordSyncedLineCount = 0,
                isValid = false,
            )
        }

        val wordSyncedCount = contentLines.count { entry ->
            val words = entry.words
            !words.isNullOrEmpty() && words.any { it.endTime > it.startTime }
        }
        val syncType = classifyLyrics(rawLyrics, entries)

        var score = when (syncType) {
            LyricsSyncType.RICH_SYNCED -> 1000
            LyricsSyncType.LINE_SYNCED -> 450
            LyricsSyncType.UNSYNCED -> 100
        }

        if (wordSyncedCount > 0 && contentLines.isNotEmpty()) {
            val wordCoverage = (wordSyncedCount.toFloat() / contentLines.size.toFloat()).coerceIn(0f, 1f)
            score += (wordCoverage * 200f).toInt()
        }

        if (isExactVideoIdSource) {
            score += 80
        }

        val priorityWeight = ((totalProviders - providerPriorityIndex).coerceAtLeast(0)) * 12
        score += priorityWeight

        if (trackDurationSec > 0 && entries.isNotEmpty()) {
            val maxEntryMs = entries.maxOfOrNull { it.time } ?: 0L
            val maxAllowedMs = (trackDurationSec + 25) * 1000L
            if (maxEntryMs > maxAllowedMs) {
                score -= 250
            }
        }

        if (providerName.equals("YouTube Music", ignoreCase = true) && syncType == LyricsSyncType.UNSYNCED) {
            score -= 20
        }

        return LyricsCandidateEvaluation(
            syncType = syncType,
            score = score,
            lineCount = lineCount,
            wordSyncedLineCount = wordSyncedCount,
            isValid = true,
        )
    }
}
