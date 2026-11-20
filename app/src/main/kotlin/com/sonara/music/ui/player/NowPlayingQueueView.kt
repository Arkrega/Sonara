/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.sonara.music.LocalPlayerConnection
import com.sonara.music.R
import com.sonara.music.constants.QueueEditLockKey
import com.sonara.music.extensions.metadata
import com.sonara.music.extensions.toggleRepeatMode
import com.sonara.music.models.MediaMetadata
import com.sonara.music.ui.component.BottomSheetState
import com.sonara.music.ui.component.glass.RegaGlassMaterial
import com.sonara.music.ui.component.glass.RegaGlassStyle
import com.sonara.music.ui.component.glass.RegaIconButton
import com.sonara.music.ui.component.glass.RegaLiquidGlass
import com.sonara.music.ui.component.glass.RegaPlayButton
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.utils.makeTimeString
import com.sonara.music.utils.rememberPreference

@Composable
internal fun NowPlayingQueueView(
    playerBottomSheetState: BottomSheetState,
    mediaMetadata: MediaMetadata?,
    pureBlack: Boolean,
    isPlaying: Boolean,
    isListenTogetherGuest: Boolean,
    textColor: Color,
    primaryButtonColor: Color,
    primaryIconColor: Color,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    onBackToMain: () -> Unit,
    onOpenLyrics: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val regaColors = LocalRegaColors.current
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { mutableStateListOf() }
    val snackbarHostState = remember { SnackbarHostState() }

    val queueLength = remember(queueWindows) {
        queueWindows.sumOf { it.mediaItem.metadata?.duration ?: 0 }
    }

    if (inSelectMode) {
        BackHandler {
            inSelectMode = false
            selection.clear()
        }
    }

    CompositionLocalProvider(LocalContentColor provides textColor) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                                text = queueTitle?.takeIf { it.isNotBlank() } ?: stringResource(R.string.queue),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 2000),
                            )
                            Text(
                                text = "${pluralStringResource(R.plurals.n_song, queueWindows.size, queueWindows.size)} • ${makeTimeString(queueLength * 1000L)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    RegaIconButton(
                        onClick = { locked = !locked },
                        size = 40.dp,
                        containerColor = regaColors.glassTint,
                        borderColor = regaColors.glassBorder,
                        contentColor = textColor,
                    ) {
                        Icon(
                            painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                QueueInnerContent(
                    playerBottomSheetState = playerBottomSheetState,
                    sheetState = null,
                    background = Color.Transparent,
                    pureBlack = pureBlack,
                    locked = locked,
                    onToggleLock = { locked = !locked },
                    inSelectMode = inSelectMode,
                    selection = selection,
                    onEnterSelectionMode = { mediaId ->
                        inSelectMode = true
                        if (mediaId !in selection) selection.add(mediaId)
                    },
                    onExitSelectionMode = {
                        inSelectMode = false
                        selection.clear()
                    },
                    snackbarHostState = snackbarHostState,
                    showTopBar = false,
                    showBottomBar = false,
                    contentTopPadding = 8.dp,
                    contentBottomPadding = 88.dp,
                    modifier = Modifier.weight(1f),
                )
            }

            RegaLiquidGlass(
                shape = CircleShape,
                borderColor = regaColors.glassBorder,
                style = RegaGlassStyle.preset(
                    material = RegaGlassMaterial.Regular,
                    tint = regaColors.glassTint,
                    borderRadius = 32.dp,
                    elevation = 10.dp,
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    RegaIconButton(
                        onClick = {
                            if (!isListenTogetherGuest) {
                                playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                            }
                        },
                        enabled = !isListenTogetherGuest,
                        size = 38.dp,
                        containerColor = if (shuffleModeEnabled) textColor.copy(alpha = 0.16f) else Color.Transparent,
                        borderColor = Color.Transparent,
                        contentColor = textColor,
                        contentDescription = stringResource(R.string.shuffle),
                    ) {
                        Icon(
                            painter = painterResource(if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    RegaIconButton(
                        onClick = onPrev,
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
                        onClick = onPlayPause,
                        size = 46.dp,
                        containerColor = primaryButtonColor,
                        iconColor = primaryIconColor,
                        contentDescription = stringResource(R.string.play_pause),
                    )

                    RegaIconButton(
                        onClick = onNext,
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
                            if (!isListenTogetherGuest) {
                                playerConnection.player.toggleRepeatMode()
                            }
                        },
                        enabled = !isListenTogetherGuest,
                        size = 38.dp,
                        containerColor = if (repeatMode != Player.REPEAT_MODE_OFF) textColor.copy(alpha = 0.16f) else Color.Transparent,
                        borderColor = Color.Transparent,
                        contentColor = textColor,
                        contentDescription = stringResource(R.string.repeat_mode_off),
                    ) {
                        Icon(
                            painter = painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    RegaIconButton(
                        onClick = onOpenLyrics,
                        size = 38.dp,
                        containerColor = Color.Transparent,
                        borderColor = Color.Transparent,
                        contentColor = textColor,
                        contentDescription = stringResource(R.string.lyrics),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lyrics),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
    }
}
