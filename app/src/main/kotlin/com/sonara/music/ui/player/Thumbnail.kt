/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.sonara.music.LocalListenTogetherManager
import com.sonara.music.LocalPlayerConnection
import com.sonara.music.R
import com.sonara.music.constants.CropAlbumArtKey
import com.sonara.music.constants.EnableVideoCacheKey
import com.sonara.music.constants.HidePlayerThumbnailKey
import com.sonara.music.constants.MaxVideoCacheSizeKey
import com.sonara.music.constants.PlayerBackgroundStyle
import com.sonara.music.constants.PlayerBackgroundStyleKey
import com.sonara.music.constants.PlayerHorizontalPadding
import com.sonara.music.constants.SeekExtraSeconds
import com.sonara.music.constants.SwipeThumbnailKey
import com.sonara.music.constants.ThumbnailCornerRadius
import com.sonara.music.constants.VideoQuality
import com.sonara.music.constants.VideoQualityKey
import com.sonara.music.db.entities.SongEntity
import com.sonara.music.listentogether.RoomRole
import com.sonara.music.ui.component.CastButton
import com.sonara.music.ui.component.glass.regaGlassSpecular
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.utils.rememberEnumPreference
import com.sonara.music.utils.rememberPreference
import com.sonara.music.ui.theme.artworkInteraction
import com.sonara.music.ui.theme.blurToFocus
import kotlinx.coroutines.delay

@Immutable
data class ThumbnailDimensions(
    val itemWidth: Dp,
    val containerWidth: Dp,
    val containerHeight: Dp,
    val containerSize: Dp,
    val thumbnailSize: Dp,
    val cornerRadius: Dp
)

@Immutable
data class MediaItemsData(
    val items: List<MediaItem>,
    val currentIndex: Int
)

@Stable
private fun calculateThumbnailDimensions(
    containerWidth: Dp,
    containerHeight: Dp = containerWidth,
    horizontalPadding: Dp = PlayerHorizontalPadding,
    cornerRadius: Dp = ThumbnailCornerRadius,
    isLandscape: Boolean = false
): ThumbnailDimensions {
    val effectiveSize = minOf(
        containerWidth - (horizontalPadding * 2),
        containerHeight - 16.dp
    ).coerceAtLeast(0.dp)
    return ThumbnailDimensions(
        itemWidth = containerWidth,
        containerWidth = containerWidth,
        containerHeight = containerHeight,
        containerSize = containerWidth,
        thumbnailSize = effectiveSize,
        cornerRadius = (effectiveSize * 0.08f).coerceIn(18.dp, 28.dp)
    )
}

@Stable
private fun getMediaItems(
    player: Player,
    swipeThumbnail: Boolean
): MediaItemsData {
    val timeline = player.currentTimeline
    val currentIndex = player.currentMediaItemIndex
    val shuffleModeEnabled = player.shuffleModeEnabled
    
    val currentMediaItem = try {
        player.currentMediaItem
    } catch (e: Exception) { null }
    
    val previousMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val previousIndex = timeline.getPreviousWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (previousIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(previousIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val nextMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val nextIndex = timeline.getNextWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (nextIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(nextIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val items = listOfNotNull(previousMediaItem, currentMediaItem, nextMediaItem)
    val currentMediaIndex = items.indexOf(currentMediaItem)
    
    return MediaItemsData(items, currentMediaIndex)
}

@Stable
@Composable
private fun getTextColor(playerBackground: PlayerBackgroundStyle): Color {
    return when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: () -> Boolean = { true },
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
    isVideoModeEnabled: Boolean = false,
    isVideoHostVisible: Boolean = false,
    onVideoStateChanged: (VideoPlaybackUiState) -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle()
    val error by playerConnection.error.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()

    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val videoQuality by rememberEnumPreference(VideoQualityKey, defaultValue = VideoQuality.AUTO)
    val enableVideoCache by rememberPreference(EnableVideoCacheKey, defaultValue = true)
    val maxVideoCacheSize by rememberPreference(MaxVideoCacheSizeKey, defaultValue = 512)
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    
    val textBackgroundColor = getTextColor(playerBackground)
    
    val thumbnailLazyGridState = rememberLazyGridState()
    
    val mediaItemsData by remember(
        playerConnection.player.currentMediaItemIndex,
        playerConnection.player.shuffleModeEnabled,
        swipeThumbnail,
        mediaMetadata
    ) {
        derivedStateOf {
            getMediaItems(playerConnection.player, swipeThumbnail)
        }
    }
    
    val mediaItems = mediaItemsData.items
    val currentMediaIndex = mediaItemsData.currentIndex

    val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState) {
        ThumbnailSnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize / 2f - itemSize / 2f)
            },
            velocityThreshold = 500f
        )
    }

    val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
    val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

    LaunchedEffect(itemScrollOffset) {
        if (!thumbnailLazyGridState.isScrollInProgress || !swipeThumbnail || itemScrollOffset != 0 || currentMediaIndex < 0) return@LaunchedEffect

        if (currentItem > currentMediaIndex && canSkipNext) {
            playerConnection.player.seekToNext()
        } else if (currentItem < currentMediaIndex && canSkipPrevious) {
            playerConnection.player.seekToPreviousMediaItem()
        }
    }

    LaunchedEffect(mediaMetadata, canSkipPrevious, canSkipNext) {
        val index = maxOf(0, currentMediaIndex)
        if (index >= 0 && index < mediaItems.size) {
            try {
                thumbnailLazyGridState.animateScrollToItem(index)
            } catch (e: Exception) {
                thumbnailLazyGridState.scrollToItem(index)
            }
        }
    }

    LaunchedEffect(playerConnection.player.currentMediaItemIndex) {
        val index = mediaItemsData.currentIndex
        if (index >= 0 && index != currentItem) {
            thumbnailLazyGridState.scrollToItem(index)
        }
    }

    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .graphicsLayer {
                compositingStrategy =
                    if (isVideoModeEnabled && !hidePlayerThumbnail) {
                        CompositingStrategy.Auto
                    } else {
                        CompositingStrategy.Offscreen
                    }
            }
    ) {
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { playbackError ->
                PlaybackError(
                    error = playbackError,
                    retry = playerConnection.player::prepare,
                )
            }
        }

        AnimatedVisibility(
            visible = error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
            ) {
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = if (isLandscape) {
                        Modifier.weight(1f, false)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    val dimensions = remember(maxWidth, maxHeight, isLandscape) {
                        calculateThumbnailDimensions(
                            containerWidth = maxWidth,
                            containerHeight = maxHeight,
                            isLandscape = isLandscape
                        )
                    }

                    val onSeekCallback = remember {
                        { direction: String, showEffect: Boolean ->
                            seekDirection = direction
                            showSeekEffect = showEffect
                        }
                    }
                    
                    val isScrollEnabled by remember(swipeThumbnail) {
                        derivedStateOf { swipeThumbnail && isPlayerExpanded() }
                    }
                    
                    LazyHorizontalGrid(
                        state = thumbnailLazyGridState,
                        rows = GridCells.Fixed(1),
                        flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                        userScrollEnabled = isScrollEnabled,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = mediaItems,
                            key = { item -> 
                                item.mediaId.ifEmpty { "unknown_${item.hashCode()}" }
                            }
                        ) { item ->
                            ThumbnailItem(
                                item = item,
                                dimensions = dimensions,
                                hidePlayerThumbnail = hidePlayerThumbnail,
                                cropAlbumArt = cropAlbumArt,
                                textBackgroundColor = textBackgroundColor,
                                layoutDirection = layoutDirection,
                                onSeek = onSeekCallback,
                                playerConnection = playerConnection,
                                context = context,
                                isLandscape = isLandscape,
                                isListenTogetherGuest = isListenTogetherGuest,
                                currentMediaId = mediaMetadata?.id,
                                currentMediaThumbnail = mediaMetadata?.thumbnailUrl,
                                currentSongEntity = currentSong?.song,
                                isVideoModeEnabled = isVideoModeEnabled,
                                isVideoHostVisible = isVideoHostVisible,
                                videoQuality = videoQuality,
                                enableVideoCache = enableVideoCache,
                                maxVideoCacheSize = maxVideoCacheSize,
                                onVideoStateChanged = onVideoStateChanged,
                            )
                        }
                    }
                }
            }
        }

        LaunchedEffect(showSeekEffect) {
            if (showSeekEffect) {
                delay(1000)
                showSeekEffect = false
            }
        }

        AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SeekEffectOverlay(seekDirection = seekDirection)
        }
    }
}

@Composable
private fun ThumbnailItem(
    item: MediaItem,
    dimensions: ThumbnailDimensions,
    hidePlayerThumbnail: Boolean,
    cropAlbumArt: Boolean,
    textBackgroundColor: Color,
    layoutDirection: LayoutDirection,
    onSeek: (String, Boolean) -> Unit,
    playerConnection: com.sonara.music.playback.PlayerConnection,
    context: android.content.Context,
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
    currentMediaId: String? = null,
    currentMediaThumbnail: String? = null,
    currentSongEntity: SongEntity? = null,
    isVideoModeEnabled: Boolean = false,
    isVideoHostVisible: Boolean = false,
    videoQuality: VideoQuality = VideoQuality.AUTO,
    enableVideoCache: Boolean = true,
    maxVideoCacheSize: Int = 512,
    onVideoStateChanged: (VideoPlaybackUiState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)
    val videoOverlay = LocalVideoOverlayController.current
    var skipMultiplier by remember { mutableIntStateOf(1) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    val isCurrent = currentMediaId == null || item.mediaId == currentMediaId
    val showVideoFrame = isCurrent && isVideoModeEnabled && !hidePlayerThumbnail
    var videoAspectRatio by remember(item.mediaId) { mutableFloatStateOf(16f / 9f) }
    var videoUiState by remember(item.mediaId, showVideoFrame) { mutableStateOf(VideoPlaybackUiState.IDLE) }
    var showVideoQuickActions by remember(item.mediaId, showVideoFrame) { mutableStateOf(false) }

    LaunchedEffect(
        isCurrent,
        showVideoFrame,
        isVideoHostVisible,
        item.mediaId,
        currentSongEntity?.explicit,
        currentSongEntity?.isUploaded,
        videoAspectRatio,
        videoUiState,
    ) {
        val overlay = videoOverlay ?: return@LaunchedEffect
        if (!isCurrent) return@LaunchedEffect
        if (showVideoFrame && isVideoHostVisible) {
            overlay.activeVideoId = item.mediaId
            overlay.activeVideoExplicit = currentSongEntity?.explicit
            overlay.activeVideoUploaded = currentSongEntity?.isUploaded
            overlay.activeVideoAspectRatio = videoAspectRatio
            overlay.activeVideoTitle = item.mediaMetadata.title?.toString().orEmpty()
            overlay.activeVideoSubtitle = item.mediaMetadata.artist?.toString().orEmpty()
            overlay.isVideoPlaybackReady = videoUiState == VideoPlaybackUiState.PLAYING
        } else if (!showVideoFrame && !overlay.isVideoFullscreen && !overlay.isInPipMode) {
            overlay.isVideoPlaybackReady = false
            overlay.activeVideoId = null
        }
    }

    LaunchedEffect(showVideoQuickActions, showVideoFrame) {
        if (showVideoQuickActions && showVideoFrame) {
            delay(4000L)
            showVideoQuickActions = false
        }
    }

    val clampedVideoRatio = videoAspectRatio.coerceIn(0.75f, 2.1f)
    val videoHorizontalPadding = if (isLandscape) 12.dp else 16.dp
    val maxVideoWidth = (dimensions.containerWidth - (videoHorizontalPadding * 2)).coerceAtLeast(120.dp)
    val maxVideoHeight = (dimensions.containerHeight - 12.dp).coerceAtLeast(100.dp)
    val fittedVideoWidth = minOf(maxVideoWidth, maxVideoHeight * clampedVideoRatio)
    val fittedVideoHeight = (fittedVideoWidth / clampedVideoRatio).coerceAtMost(maxVideoHeight)

    val targetCardWidth = if (showVideoFrame) fittedVideoWidth else dimensions.thumbnailSize
    val targetCardHeight = if (showVideoFrame) fittedVideoHeight else dimensions.thumbnailSize
    val targetCornerRadius =
        if (showVideoFrame) {
            (minOf(fittedVideoWidth, fittedVideoHeight) * 0.085f).coerceIn(14.dp, 22.dp)
        } else {
            dimensions.cornerRadius
        }

    val cardWidth by animateDpAsState(
        targetValue = targetCardWidth,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 360f),
        label = "artworkWidth",
    )
    val cardHeight by animateDpAsState(
        targetValue = targetCardHeight,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 360f),
        label = "artworkHeight",
    )
    val cardCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 360f),
        label = "artworkCornerRadius",
    )
    val artworkScale by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.9f,
        animationSpec = tween(durationMillis = 260),
        label = "artworkScale",
    )
    val artworkAlpha by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.5f,
        animationSpec = tween(durationMillis = 260),
        label = "artworkAlpha",
    )
    val thumbnailOverlayAlpha by animateFloatAsState(
        targetValue = if (showVideoFrame && isVideoHostVisible && videoUiState == VideoPlaybackUiState.PLAYING) 0f else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "thumbnailOverlayAlpha",
    )

    Box(
        modifier = modifier
            .width(dimensions.itemWidth)
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy =
                    if (showVideoFrame) {
                        CompositingStrategy.Auto
                    } else {
                        CompositingStrategy.Offscreen
                    }
            }
            .pointerInput(showVideoFrame, videoUiState) {
                detectTapGestures(
                    onTap = {
                        if (showVideoFrame && videoUiState == VideoPlaybackUiState.PLAYING) {
                            showVideoQuickActions = !showVideoQuickActions
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isListenTogetherGuest) return@detectTapGestures

                        val currentPosition = playerConnection.player.currentPosition
                        val duration = playerConnection.player.duration

                        val now = System.currentTimeMillis()
                        if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) {
                            skipMultiplier++
                        } else {
                            skipMultiplier = 1
                        }
                        lastTapTime = now

                        val skipAmount = 5000 * skipMultiplier

                        val isLeftSide = (layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)

                        if (isLeftSide) {
                            playerConnection.player.seekTo((currentPosition - skipAmount).coerceAtLeast(0))
                            onSeek(context.getString(R.string.seek_backward_dynamic, skipAmount / 1000), true)
                        } else {
                            playerConnection.player.seekTo((currentPosition + skipAmount).coerceAtMost(duration))
                            onSeek(context.getString(R.string.seek_forward_dynamic, skipAmount / 1000), true)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val artworkShape = RoundedCornerShape(cardCornerRadius)
        Box(
            modifier = Modifier
                .size(width = cardWidth, height = cardHeight)
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                    alpha = artworkAlpha
                }
                .blurToFocus(key = item.mediaId, enabled = !showVideoFrame)
                .artworkInteraction(enabled = !showVideoFrame)
                .shadow(
                    elevation = 14.dp,
                    shape = artworkShape,
                    ambientColor = Color(0x665E6AD2),
                    spotColor = Color(0x805E6AD2),
                )
                .clip(artworkShape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = artworkShape,
                )
        ) {
            if (hidePlayerThumbnail) {
                HiddenThumbnailPlaceholder(textBackgroundColor = textBackgroundColor)
            } else {
                val artworkUriToUse = if (item.mediaId == currentMediaId && !currentMediaThumbnail.isNullOrBlank()) {
                    currentMediaThumbnail
                } else {
                    item.mediaMetadata.artworkUri?.toString()
                }

                if (showVideoFrame) {
                    SyncedVideoPlayerSurface(
                        videoId = item.mediaId,
                        isExplicit = currentSongEntity?.explicit,
                        isUploaded = currentSongEntity?.isUploaded,
                        mainPlayer = playerConnection.player,
                        videoQuality = videoQuality,
                        isActive = showVideoFrame && isVideoHostVisible && videoOverlay?.isVideoFullscreen != true && videoOverlay?.isInPipMode != true,
                        onVideoAspectRatioChanged = { ratio ->
                            videoAspectRatio = ratio
                        },
                        onVideoStateChanged = { state ->
                            videoUiState = state
                            onVideoStateChanged(state)
                        },
                        videoCache = playerConnection.service.videoCache,
                        enableVideoCache = enableVideoCache,
                        maxVideoCacheSizeMb = maxVideoCacheSize,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (thumbnailOverlayAlpha > 0.01f) {
                    ThumbnailImage(
                        artworkUri = artworkUriToUse,
                        cropArtwork = if (showVideoFrame) true else cropAlbumArt,
                        modifier = Modifier.graphicsLayer {
                            alpha = thumbnailOverlayAlpha
                        },
                    )
                }
            }
            
            CastButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                tintColor = textBackgroundColor
            )

            AnimatedVisibility(
                visible = showVideoFrame && videoUiState == VideoPlaybackUiState.PLAYING && showVideoQuickActions,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .regaGlassSpecular(
                                shape = CircleShape,
                                tintColor = Color.Black.copy(alpha = 0.28f),
                                borderColor = Color.White.copy(alpha = 0.45f),
                            )
                            .clip(CircleShape)
                            .clickable {
                                showVideoQuickActions = false
                                videoOverlay?.onEnterPip?.invoke(videoAspectRatio)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.picture_in_picture),
                            contentDescription = stringResource(R.string.enter_pip),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .regaGlassSpecular(
                                shape = CircleShape,
                                tintColor = Color.Black.copy(alpha = 0.28f),
                                borderColor = Color.White.copy(alpha = 0.45f),
                            )
                            .clip(CircleShape)
                            .clickable {
                                showVideoQuickActions = false
                                videoOverlay?.isVideoFullscreen = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.fullscreen),
                            contentDescription = stringResource(R.string.enter_fullscreen),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenThumbnailPlaceholder(
    textBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalRegaColors.current.glassTint),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = stringResource(R.string.hide_player_thumbnail),
            tint = textBackgroundColor.copy(alpha = 0.7f),
            modifier = Modifier.size(120.dp)
        )
    }
}

@Composable
private fun ThumbnailImage(
    artworkUri: String?,
    cropArtwork: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .background(LocalRegaColors.current.glassTint)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = if (cropArtwork) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SeekEffectOverlay(
    seekDirection: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = seekDirection,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    )
}
