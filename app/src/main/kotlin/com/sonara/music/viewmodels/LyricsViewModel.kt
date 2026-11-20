/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.sonara.music.lyrics.LyricsEntry
import com.sonara.music.lyrics.LyricsQualityAnalyzer
import com.sonara.music.lyrics.LyricsSyncType
import com.sonara.music.lyrics.LyricsUtils
import com.sonara.music.ui.component.LyricsListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class LyricsViewModel @Inject constructor() : ViewModel() {
    companion object {
        private val timestampRegex = Regex("\\[\\d{1,2}:\\d{2}")
    }

    private var processJob: Job? = null

    private val _lines = MutableStateFlow<List<LyricsEntry>>(emptyList())
    val lines: StateFlow<List<LyricsEntry>> = _lines.asStateFlow()

    private val _mergedLyricsList = MutableStateFlow<List<LyricsListItem>>(emptyList())
    val mergedLyricsList: StateFlow<List<LyricsListItem>> = _mergedLyricsList.asStateFlow()

    private val _syncType = MutableStateFlow(LyricsSyncType.UNSYNCED)
    val syncType: StateFlow<LyricsSyncType> = _syncType.asStateFlow()

    fun processLyrics(
        lyrics: String?,
        enabledLanguages: List<String>,
        romanizeCyrillicByLine: Boolean,
        showIntervalIndicator: Boolean
    ) {
        processJob?.cancel()
        processJob = viewModelScope.launch {
            val (processedLines, resolvedSyncType) = withContext(Dispatchers.Default) {
                if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                    emptyList<LyricsEntry>() to LyricsSyncType.UNSYNCED
                } else {
                    val isLrc = timestampRegex.containsMatchIn(lyrics)
                    val parsedLines = if (isLrc) LyricsUtils.parseLyrics(lyrics) else emptyList()

                    if (parsedLines.isNotEmpty()) {
                        val classified = LyricsQualityAnalyzer.classifyLyrics(lyrics, parsedLines)
                        (listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parsedLines) to classified
                    } else {
                        val unsyncedLines = lyrics.lines()
                            .filter { it.isNotBlank() && !timestampRegex.containsMatchIn(it) }
                            .mapIndexed { index, line ->
                                LyricsEntry(-1L - index, line.trim())
                            }
                        unsyncedLines to LyricsSyncType.UNSYNCED
                    }
                }
            }

            _syncType.value = resolvedSyncType
            _lines.value = processedLines
            updateMergedList(
                lines = processedLines,
                showIntervalIndicator = showIntervalIndicator && resolvedSyncType != LyricsSyncType.UNSYNCED,
            )

            if (lyrics != null && lyrics != LYRICS_NOT_FOUND && enabledLanguages.isNotEmpty()) {
                launch(Dispatchers.Default) {
                    processedLines.forEach { entry ->
                        if (entry == LyricsEntry.HEAD_LYRICS_ENTRY) return@forEach
                        entry.romanizedTextFlow.value = LyricsUtils.romanize(
                            text = lyrics,
                            line = entry.text,
                            enabledLanguages = enabledLanguages,
                            romanizeCyrillicByLine = romanizeCyrillicByLine
                        )
                    }
                }
            }
        }
    }

    private fun updateMergedList(lines: List<LyricsEntry>, showIntervalIndicator: Boolean) {
        val result = mutableListOf<LyricsListItem>()
        if (lines.isEmpty()) {
            _mergedLyricsList.value = result
            return
        }
        lines.forEachIndexed { i, entry ->
            if (entry.text.isNotBlank()) {
                result.add(LyricsListItem.Line(i, entry))
            }
            if (showIntervalIndicator && i < lines.size - 1) {
                val nextStart = lines[i + 1].time
                val currentEnd = if (!entry.words.isNullOrEmpty()) {
                    (entry.words.last().endTime * 1000).toLong()
                } else if (entry.text.isBlank()) {
                    entry.time
                } else {
                    null
                }

                if (currentEnd != null && currentEnd < nextStart) {
                    val gap = nextStart - currentEnd
                    if (gap > 4000L) {
                        result.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart, lines[i + 1].agent))
                    }
                }
            }
        }
        _mergedLyricsList.value = result
    }
}
