/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sonara.music.LocalDatabase
import com.sonara.music.LocalPlayerConnection
import com.sonara.music.R
import com.sonara.music.constants.ExperimentalLyricsKey
import com.sonara.music.db.entities.LyricsEntity
import com.sonara.music.models.MediaMetadata
import com.sonara.music.ui.component.LocalMenuState
import com.sonara.music.ui.component.glass.RegaGlassMaterial
import com.sonara.music.ui.component.glass.RegaGlassStyle
import com.sonara.music.ui.component.glass.RegaIconButton
import com.sonara.music.ui.component.glass.RegaLiquidGlass
import com.sonara.music.ui.component.glass.RegaPlayButton
import com.sonara.music.ui.menu.LyricsMenu
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.ui.theme.LocalTabularTimeStyle
import com.sonara.music.utils.rememberPreference
import kotlinx.coroutines.delay

@Composable
internal fun NowPlayingLyricsView(
    mediaMetadata: MediaMetadata?,
    lyricsEntity: LyricsEntity?,
    sliderPositionProvider: () -> Long?,
    isPlaying: Boolean,
    isListenTogetherGuest: Boolean,
    textColor: Color,
    primaryButtonColor: Color,
    primaryIconColor: Color,
    onBackToMain: () -> Unit,
    onOpenQueue: () -> Unit,
    onShowOffsetDialog: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val regaColors = LocalRegaColors.current

    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
    val (experimentalLyrics) = rememberPreference(ExperimentalLyricsKey, defaultValue = true)

    var showOffsetBar by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionSignal by remember { mutableIntStateOf(0) }

    val wakeControls = remember {
        {
            controlsVisible = true
            interactionSignal += 1
        }
    }

    LaunchedEffect(interactionSignal, isPlaying, showOffsetBar) {
        if (isPlaying && !showOffsetBar) {
            delay(3600L)
            controlsVisible = false
        } else {
            controlsVisible = true
        }
    }

    val adjustOffset: (Int) -> Unit = remember(currentSong, lyricsOffset) {
        { deltaMs ->
            wakeControls()
            currentSong?.song?.let { song ->
                val nextOffset = (song.lyricsOffset + deltaMs).coerceIn(-5000, 5000)
                database.query {
                    upsert(song.copy(lyricsOffset = nextOffset))
                }
            }
        }
    }

    val resetOffset: () -> Unit = remember(currentSong) {
        {
            wakeControls()
            currentSong?.song?.let { song ->
                database.query {
                    upsert(song.copy(lyricsOffset = 0))
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { wakeControls() })
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        RegaIconButton(
                            onClick = onBackToMain,
                            size = 40.dp,
                            containerColor = regaColors.glassTint,
                            borderColor = regaColors.glassBorder,
                            contentColor = textColor,
                            contentDescription = stringResource(R.string.back_to_player),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onBackToMain() }
                                .padding(vertical = 2.dp),
                        ) {
                            if (!mediaMetadata?.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = mediaMetadata.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mediaMetadata?.title.orEmpty(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 2000),
                                )
                                Text(
                                    text = mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        RegaLiquidGlass(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    wakeControls()
                                    showOffsetBar = !showOffsetBar
                                },
                            shape = CircleShape,
                            borderColor = if (showOffsetBar) textColor.copy(alpha = 0.55f) else regaColors.glassBorder,
                            style = RegaGlassStyle.preset(
                                material = RegaGlassMaterial.Thin,
                                tint = regaColors.glassTint,
                                borderRadius = 18.dp,
                                elevation = 4.dp,
                            ),
                        ) {
                            Text(
                                text = "${if (lyricsOffset >= 0) "+" else ""}${lyricsOffset}ms",
                                style = LocalTabularTimeStyle.current.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 10.dp),
                            )
                        }

                        RegaIconButton(
                            onClick = {
                                wakeControls()
                                mediaMetadata?.let { meta ->
                                    menuState.show {
                                        LyricsMenu(
                                            lyricsProvider = { lyricsEntity },
                                            songProvider = { currentSong?.song },
                                            mediaMetadataProvider = { meta },
                                            onDismiss = menuState::dismiss,
                                            onShowOffsetDialog = onShowOffsetDialog,
                                        )
                                    }
                                }
                            },
                            size = 40.dp,
                            containerColor = regaColors.glassTint,
                            borderColor = regaColors.glassBorder,
                            contentColor = textColor,
                            contentDescription = stringResource(R.string.more_options),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }

                    AnimatedVisibility(visible = showOffsetBar) {
                        RegaLiquidGlass(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            borderColor = regaColors.glassBorder,
                            style = RegaGlassStyle.preset(
                                material = RegaGlassMaterial.Thin,
                                tint = regaColors.glassTint,
                                borderRadius = 18.dp,
                                elevation = 6.dp,
                            ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                OffsetAdjustPill(label = "-100", textColor = textColor) { adjustOffset(-100) }
                                OffsetAdjustPill(label = "-50", textColor = textColor) { adjustOffset(-50) }
                                OffsetAdjustPill(
                                    label = stringResource(R.string.lyrics_offset_reset),
                                    textColor = textColor,
                                    onClick = resetOffset,
                                )
                                OffsetAdjustPill(label = "+50", textColor = textColor) { adjustOffset(50) }
                                OffsetAdjustPill(label = "+100", textColor = textColor) { adjustOffset(100) }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                InlineLyricsView(
                    mediaMetadata = mediaMetadata,
                    showLyrics = true,
                    sliderPositionProvider = sliderPositionProvider,
                    useExperimentalLyrics = experimentalLyrics,
                    onUserInteraction = wakeControls,
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            RegaLiquidGlass(
                shape = CircleShape,
                borderColor = regaColors.glassBorder,
                style = RegaGlassStyle.preset(
                    material = RegaGlassMaterial.Regular,
                    tint = regaColors.glassTint,
                    borderRadius = 32.dp,
                    elevation = 10.dp,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    RegaIconButton(
                        onClick = {
                            wakeControls()
                            onPrev()
                        },
                        enabled = canSkipPrev && !isListenTogetherGuest,
                        size = 40.dp,
                        containerColor = Color.Transparent,
                        borderColor = Color.Transparent,
                        contentColor = textColor,
                        contentDescription = stringResource(R.string.previous),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    RegaPlayButton(
                        isPlaying = isPlaying,
                        onClick = {
                            wakeControls()
                            onPlayPause()
                        },
                        size = 46.dp,
                        containerColor = primaryButtonColor,
                        iconColor = primaryIconColor,
                        contentDescription = stringResource(R.string.play_pause),
                    )

                    RegaIconButton(
                        onClick = {
                            wakeControls()
                            onNext()
                        },
                        enabled = canSkipNext && !isListenTogetherGuest,
                        size = 40.dp,
                        containerColor = Color.Transparent,
                        borderColor = Color.Transparent,
                        contentColor = textColor,
                        contentDescription = stringResource(R.string.next),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    RegaIconButton(
                        onClick = {
                            wakeControls()
                            onOpenQueue()
                        },
                        size = 40.dp,
                        containerColor = Color.Transparent,
                        borderColor = Color.Transparent,
                        contentColor = textColor,
                        contentDescription = stringResource(R.string.queue),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OffsetAdjustPill(
    label: String,
    textColor: Color,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = textColor,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
