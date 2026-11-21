/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Dedicated Live Lyrics Strip inspired by SimpMusic Apple Music Player
 */

package com.sonara.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.music.R
import com.sonara.music.db.entities.LyricsEntity
import com.sonara.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.sonara.music.db.entities.Song
import com.sonara.music.lyrics.LyricsEntry
import com.sonara.music.lyrics.LyricsQualityAnalyzer
import com.sonara.music.lyrics.LyricsSyncType
import com.sonara.music.lyrics.LyricsUtils
import com.sonara.music.lyrics.WordTimestamp
import com.sonara.music.lyrics.rememberLyricsPlayhead
import com.sonara.music.playback.PlayerConnection
import com.sonara.music.ui.component.glass.LocalLiquidGlassEnabled
import com.sonara.music.ui.component.glass.RegaGlassMaterial
import com.sonara.music.ui.component.glass.RegaGlassStyle
import com.sonara.music.ui.component.glass.RegaLiquidGlass

@Composable
internal fun LiveLyricsStrip(
    lyricsEntity: LyricsEntity?,
    currentSong: Song?,
    songId: String?,
    playerConnection: PlayerConnection,
    sliderPositionProvider: () -> Long?,
    textColor: Color,
    isExpanded: Boolean,
    isLoading: Boolean = false,
    compactMode: Boolean = false,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rawLyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val parsedBundle = remember(rawLyrics) {
        if (rawLyrics.isNullOrBlank() || rawLyrics == LYRICS_NOT_FOUND) {
            emptyList<LyricsEntry>() to LyricsSyncType.UNSYNCED
        } else {
            val entries = runCatching { LyricsUtils.parseLyrics(rawLyrics) }.getOrDefault(emptyList())
                .filter { it != LyricsEntry.HEAD_LYRICS_ENTRY && it.text.isNotBlank() && !it.isBackground }
            val syncType = LyricsQualityAnalyzer.classifyLyrics(rawLyrics, entries)
            entries to syncType
        }
    }

    val entries = parsedBundle.first
    val syncType = parsedBundle.second
    val isSynced = syncType != LyricsSyncType.UNSYNCED && entries.isNotEmpty()

    val playheadState = rememberLyricsPlayhead(
        playerConnection = playerConnection,
        sliderPositionProvider = sliderPositionProvider,
        lyricsOffsetProvider = { currentSong?.song?.lyricsOffset ?: 0 },
        songId = songId,
        isActive = isExpanded && isSynced,
    )

    var activeIndex by remember(rawLyrics) { mutableIntStateOf(-1) }

    LaunchedEffect(entries, isSynced, isExpanded) {
        if (!isSynced || !isExpanded) {
            activeIndex = -1
            return@LaunchedEffect
        }
        snapshotFlow { playheadState.value }.collect { effectivePos ->
            val idx = LyricsUtils.findCurrentLineIndex(entries, effectivePos)
            if (idx != activeIndex) {
                activeIndex = idx
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val cardRadius = if (compactMode) 16.dp else 20.dp
    val cardShape = RoundedCornerShape(cardRadius)

    val stripContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = textColor.copy(alpha = 0.12f)),
                    onClick = onOpenLyrics,
                )
                .padding(horizontal = if (compactMode) 14.dp else 16.dp, vertical = if (compactMode) 12.dp else 14.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sonara_lyrics_preview),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.translate),
                contentDescription = null,
                tint = textColor.copy(alpha = 0.50f),
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
                when {
                    rawLyrics == LYRICS_NOT_FOUND -> {
                        Text(
                            text = stringResource(R.string.lyrics_unavailable_tap),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.56f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    rawLyrics.isNullOrBlank() -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WaitingLyricsDots(color = textColor.copy(alpha = 0.72f))
                            if (isLoading) {
                                Text(
                                    text = stringResource(R.string.loading_lyrics),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    syncType == LyricsSyncType.UNSYNCED -> {
                        val previewLines = rawLyrics.lineSequence()
                            .map { it.trim() }
                            .filter { it.isNotBlank() && !it.startsWith("[") }
                            .take(if (compactMode) 1 else 2)
                            .toList()
                        val previewText = previewLines.joinToString("\n").ifBlank {
                            stringResource(R.string.lyrics_unavailable_tap)
                        }
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = textColor.copy(alpha = 0.82f),
                            maxLines = if (compactMode) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    activeIndex < 0 -> {
                        val firstLineText = entries.firstOrNull()?.text.orEmpty()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            WaitingLyricsDots(color = textColor.copy(alpha = 0.80f))
                            if (firstLineText.isNotBlank()) {
                                Text(
                                    text = firstLineText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = textColor.copy(alpha = 0.56f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    else -> {
                        val currentEntry = entries.getOrNull(activeIndex)
                        if (currentEntry != null) {
                            AnimatedContent(
                                targetState = activeIndex,
                                transitionSpec = {
                                    (slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(320)))
                                        .togetherWith(slideOutVertically(tween(220)) { -it / 4 } + fadeOut(tween(180)))
                                },
                                label = "liveLyricsStripLine",
                            ) { idx ->
                                val item = entries.getOrNull(idx)
                                val nextItem = if (!compactMode) entries.getOrNull(idx + 1) else null
                                if (item != null) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        val realWords = item.words?.takeIf { words ->
                                            words.isNotEmpty() && words.any { it.endTime > it.startTime }
                                        }
                                        if (syncType == LyricsSyncType.RICH_SYNCED && realWords != null) {
                                            StripWordSweepLine(
                                                lineText = item.text,
                                                words = realWords,
                                                playheadState = playheadState,
                                                activeColor = textColor,
                                                inactiveColor = textColor.copy(alpha = 0.45f),
                                            )
                                        } else {
                                            Text(
                                                text = item.text,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = textColor,
                                                maxLines = if (compactMode) 1 else 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        if (nextItem != null && nextItem.text.isNotBlank()) {
                                            Text(
                                                text = nextItem.text,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Normal,
                                                ),
                                                color = textColor.copy(alpha = 0.50f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }
        }
    }

    if (liquidGlassEnabled) {
        RegaLiquidGlass(
            modifier = modifier.fillMaxWidth(),
            shape = cardShape,
            borderColor = textColor.copy(alpha = 0.18f),
            style = RegaGlassStyle.preset(
                material = RegaGlassMaterial.Clear,
                tint = textColor.copy(alpha = 0.06f),
                borderRadius = cardRadius,
                elevation = 0.dp,
            ),
            contentAlignment = Alignment.TopStart,
        ) {
            stripContent()
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(textColor.copy(alpha = 0.055f))
                .border(1.dp, textColor.copy(alpha = 0.075f), cardShape),
        ) {
            stripContent()
        }
    }
}

@Composable
private fun StripWordSweepLine(
    lineText: String,
    words: List<WordTimestamp>,
    playheadState: State<Long>,
    activeColor: Color,
    inactiveColor: Color,
) {
    var layoutResult by remember(lineText) { mutableStateOf<TextLayoutResult?>(null) }

    val lineStartMs = remember(words) { (words.first().startTime * 1000.0).toLong() }
    val lineEndMs = remember(words) { (words.last().endTime * 1000.0).toLong().coerceAtLeast(lineStartMs + 100L) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = lineText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = inactiveColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult = it },
        )

        val sweepStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = lineText,
            style = sweepStyle,
            color = activeColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    val posMs = playheadState.value
                    val progress = computeWordWeightedLineProgress(words, lineStartMs, lineEndMs, posMs)
                    if (progress <= 0f) return@drawWithContent
                    val measuredW = layoutResult?.size?.width?.toFloat()?.coerceAtMost(size.width) ?: size.width
                    val revealX = (measuredW * progress).coerceIn(0f, size.width)
                    val featherPx = 20.dp.toPx().coerceAtMost(revealX.coerceAtLeast(1f))
                    clipRect(left = 0f, top = 0f, right = revealX, bottom = size.height) {
                        this@drawWithContent.drawContent()
                    }
                    if (progress in 0.01f..0.99f && revealX > 2f) {
                        val fadeStart = ((revealX - featherPx) / size.width).coerceIn(0f, 1f)
                        val fadeEnd = (revealX / size.width).coerceIn(fadeStart, 1f)
                        if (fadeEnd > fadeStart) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0f to Color.Black,
                                    fadeStart to Color.Black,
                                    fadeEnd to Color.Transparent,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                    }
                },
        )
    }
}

private fun computeWordWeightedLineProgress(
    words: List<WordTimestamp>,
    lineStartMs: Long,
    lineEndMs: Long,
    posMs: Long,
): Float {
    if (words.isEmpty()) return 0f
    if (posMs <= lineStartMs) return 0f
    if (posMs >= lineEndMs) return 1f

    val totalChars = words.sumOf { it.text.length.coerceAtLeast(1) }.toFloat().coerceAtLeast(1f)
    var accumulatedChars = 0f
    for (word in words) {
        val wordLen = word.text.length.coerceAtLeast(1).toFloat()
        val sMs = (word.startTime * 1000.0).toLong()
        val eMs = (word.endTime * 1000.0).toLong().coerceAtLeast(sMs + 1L)
        when {
            posMs >= eMs -> {
                accumulatedChars += wordLen
            }
            posMs in sMs until eMs -> {
                val frac = (posMs - sMs).toFloat() / (eMs - sMs).toFloat()
                accumulatedChars += wordLen * frac.coerceIn(0f, 1f)
                return (accumulatedChars / totalChars).coerceIn(0f, 1f)
            }
            else -> {
                return (accumulatedChars / totalChars).coerceIn(0f, 1f)
            }
        }
    }
    return (accumulatedChars / totalChars).coerceIn(0f, 1f)
}

@Composable
private fun WaitingLyricsDots(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "waitingDots")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waitingDotsPhase",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until 3) {
            val active = ((phase - i + 3f) % 3f)
            val dotAlpha = if (active < 1f) 0.9f else 0.35f
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(
                    color = color.copy(alpha = dotAlpha),
                    radius = size.minDimension * 0.5f,
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                )
            }
        }
    }
}
