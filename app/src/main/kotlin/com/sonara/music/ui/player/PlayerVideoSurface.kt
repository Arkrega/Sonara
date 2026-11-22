package com.sonara.music.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import com.metrolist.innertubex.extraction.ContentHints
import com.sonara.music.R
import com.sonara.music.constants.VideoQuality
import com.sonara.music.db.entities.SongEntity
import com.sonara.music.models.MediaMetadata
import com.sonara.music.playback.CacheCleanupManager
import com.sonara.music.playback.ChunkedRangeDataSource
import com.sonara.music.ui.component.glass.LocalLiquidGlassEnabled
import com.sonara.music.ui.component.glass.LocalRegaBackdrop
import com.sonara.music.ui.component.glass.RegaGlassMaterial
import com.sonara.music.ui.component.glass.RegaGlassStyle
import com.sonara.music.ui.component.glass.RegaIconButton
import com.sonara.music.ui.component.glass.RegaLiquidGlass
import com.sonara.music.ui.component.glass.RegaPillShape
import com.sonara.music.ui.component.glass.RegaPlayButton
import com.sonara.music.ui.component.glass.regaBackdropSource
import com.sonara.music.ui.component.glass.regaGlassSpecular
import com.sonara.music.ui.component.glass.rememberRegaBackdropState
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.utils.InnerTubeXPlayer
import com.sonara.music.utils.InnerTubeXPlayer.resolveMaxHeight
import com.sonara.music.utils.SharedHttpPool
import com.sonara.music.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import kotlin.math.abs

enum class VideoPlaybackUiState {
    IDLE,
    LOADING,
    PLAYING,
    FAILED,
}

fun isVideoTrack(
    mediaMetadata: MediaMetadata?,
    songEntity: SongEntity?,
): Boolean {
    if (mediaMetadata == null) return false
    if (mediaMetadata.musicVideoType != null) {
        return mediaMetadata.isVideoSong
    }
    if (songEntity?.isVideo == true) return true
    if (!mediaMetadata.isEpisode && mediaMetadata.album == null) {
        val thumb = mediaMetadata.thumbnailUrl.orEmpty()
        if (thumb.contains("i.ytimg.com", ignoreCase = true) ||
            thumb.contains("img.youtube.com", ignoreCase = true)
        ) {
            return true
        }
    }
    return false
}

@Composable
fun PlayerVideoModeToggle(
    isVideoMode: Boolean,
    isLoading: Boolean,
    textColor: Color,
    onSelectVideoMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val regaColors = LocalRegaColors.current
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val pillShape = if (liquidGlassEnabled) CircleShape else RegaPillShape
    val activeBg = textColor.copy(alpha = 0.18f)
    val activeBorder = textColor.copy(alpha = 0.32f)

    BoxWithConstraints(
        modifier = modifier
            .width(184.dp)
            .height(34.dp)
            .regaGlassSpecular(
                shape = pillShape,
                tintColor = regaColors.glassTint,
                borderColor = regaColors.glassBorder,
            )
            .clip(pillShape)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / 2f
        val capsuleOffset by animateDpAsState(
            targetValue = if (isVideoMode) segmentWidth else 0.dp,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "videoToggleCapsuleOffset",
        )

        if (liquidGlassEnabled) {
            RegaLiquidGlass(
                modifier = Modifier
                    .offset(x = capsuleOffset)
                    .width(segmentWidth)
                    .fillMaxHeight(),
                shape = pillShape,
                borderColor = activeBorder,
                style = RegaGlassStyle.preset(
                    material = RegaGlassMaterial.Clear,
                    tint = activeBg,
                    borderRadius = 15.dp,
                    elevation = 2.dp,
                ),
            ) {}
        } else {
            Box(
                modifier = Modifier
                    .offset(x = capsuleOffset)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(pillShape)
                    .background(activeBg)
                    .border(0.75.dp, activeBorder, pillShape),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            VideoModePillSegment(
                selected = !isVideoMode,
                label = stringResource(R.string.player_mode_audio),
                iconRes = R.drawable.music_note,
                showLoading = false,
                textColor = textColor,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = {
                    if (isVideoMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectVideoMode(false)
                    }
                },
            )
            VideoModePillSegment(
                selected = isVideoMode,
                label = stringResource(R.string.player_mode_video),
                iconRes = R.drawable.smart_display,
                showLoading = isVideoMode && isLoading,
                textColor = textColor,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = {
                    if (!isVideoMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectVideoMode(true)
                    }
                },
            )
        }
    }
}

@Composable
private fun VideoModePillSegment(
    selected: Boolean,
    label: String,
    iconRes: Int,
    showLoading: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) textColor else textColor.copy(alpha = 0.62f),
        animationSpec = tween(180),
        label = "videoPillContent",
    )

    val segmentShape = if (LocalLiquidGlassEnabled.current) CircleShape else RegaPillShape
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(segmentShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (showLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 1.6.dp,
                modifier = Modifier.size(13.dp),
            )
        } else {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Stable
class VideoOverlayController(
    val onEnterPip: (Float) -> Unit,
) {
    var isVideoFullscreen by mutableStateOf(false)
    var isInPipMode by mutableStateOf(false)
    var activeVideoId by mutableStateOf<String?>(null)
    var activeVideoExplicit by mutableStateOf<Boolean?>(null)
    var activeVideoUploaded by mutableStateOf<Boolean?>(null)
    var activeVideoAspectRatio by mutableFloatStateOf(16f / 9f)
    var activeVideoTitle by mutableStateOf("")
    var activeVideoSubtitle by mutableStateOf("")
    var isVideoPlaybackReady by mutableStateOf(false)

    fun canAutoEnterPip(
        isPlaying: Boolean,
        isPlayerExpanded: Boolean,
    ): Boolean = isVideoPlaybackReady && activeVideoId != null && isPlaying && (isPlayerExpanded || isVideoFullscreen)
}

val LocalVideoOverlayController = staticCompositionLocalOf<VideoOverlayController?> { null }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenVideoPlayerOverlay(
    videoId: String,
    isExplicit: Boolean?,
    isUploaded: Boolean?,
    title: String,
    subtitle: String,
    mainPlayer: ExoPlayer,
    videoQuality: VideoQuality,
    videoCache: Cache?,
    enableVideoCache: Boolean,
    maxVideoCacheSizeMb: Int,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onEnterPip: (Float) -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val regaColors = LocalRegaColors.current
    val videoHudBackdrop = rememberRegaBackdropState()
    var showHud by remember { mutableStateOf(true) }
    var currentAspectRatio by remember(videoId) { mutableFloatStateOf(16f / 9f) }
    var seekBadgeText by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(mainPlayer.isPlaying) }
    var positionMs by remember { mutableLongStateOf(mainPlayer.currentPosition.coerceAtLeast(0L)) }
    var durationMs by remember { mutableLongStateOf(mainPlayer.duration.takeIf { it > 0L } ?: 0L) }
    var sliderDragMs by remember { mutableStateOf<Long?>(null) }

    BackHandler(onBack = onExitFullscreen)

    DisposableEffect(context) {
        val activity = context.findActivity()
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller?.let {
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(mainPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        mainPlayer.addListener(listener)
        onDispose {
            mainPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(mainPlayer, showHud) {
        while (true) {
            positionMs = mainPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = mainPlayer.duration.takeIf { it > 0L } ?: 0L
            delay(300L)
        }
    }

    LaunchedEffect(showHud, isPlaying) {
        if (showHud && isPlaying) {
            delay(3500L)
            showHud = false
        }
    }

    LaunchedEffect(seekBadgeText) {
        if (seekBadgeText != null) {
            delay(900L)
            seekBadgeText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .regaBackdropSource(videoHudBackdrop),
        ) {
            SyncedVideoPlayerSurface(
                videoId = videoId,
                isExplicit = isExplicit,
                isUploaded = isUploaded,
                mainPlayer = mainPlayer,
                videoQuality = videoQuality,
                isActive = true,
                onVideoAspectRatioChanged = { ratio ->
                    currentAspectRatio = ratio
                },
                onVideoStateChanged = {},
                videoCache = videoCache,
                enableVideoCache = enableVideoCache,
                maxVideoCacheSizeMb = maxVideoCacheSizeMb,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showHud = !showHud
                        },
                        onDoubleTap = { offset ->
                            val isLeftSide = (layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2f) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2f)
                            val delta = if (isLeftSide) -10_000L else 10_000L
                            val dur = mainPlayer.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                            val target = (mainPlayer.currentPosition + delta).coerceIn(0L, dur)
                            mainPlayer.seekTo(target)
                            positionMs = target
                            seekBadgeText = if (isLeftSide) "-10s" else "+10s"
                        },
                    )
                },
        )

        CompositionLocalProvider(LocalRegaBackdrop provides videoHudBackdrop) {
            AnimatedVisibility(
                visible = seekBadgeText != null,
                enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.88f, animationSpec = tween(140)),
                exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180)),
                modifier = Modifier.align(Alignment.Center),
            ) {
                RegaLiquidGlass(
                    shape = CircleShape,
                    borderColor = Color.White.copy(alpha = 0.45f),
                    style = RegaGlassStyle.preset(
                        material = RegaGlassMaterial.Thin,
                        tint = Color.Black.copy(alpha = 0.35f),
                        borderRadius = 24.dp,
                    ),
                ) {
                    Text(
                        text = seekBadgeText.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = showHud,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.56f),
                                    Color.Black.copy(alpha = 0.16f),
                                    Color.Black.copy(alpha = 0.64f),
                                ),
                            ),
                        )
                        .systemBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            RegaIconButton(
                                onClick = onExitFullscreen,
                                size = 40.dp,
                                shape = CircleShape,
                                containerColor = regaColors.glassTint,
                                borderColor = Color.White.copy(alpha = 0.35f),
                                contentColor = Color.White,
                                contentDescription = stringResource(R.string.exit_fullscreen),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.fullscreen_exit),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(modifier = Modifier.widthIn(max = 420.dp)) {
                                if (title.isNotBlank()) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        color = Color.White.copy(alpha = 0.78f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        RegaIconButton(
                            onClick = { onEnterPip(currentAspectRatio) },
                            size = 40.dp,
                            shape = CircleShape,
                            containerColor = regaColors.glassTint,
                            borderColor = Color.White.copy(alpha = 0.35f),
                            contentColor = Color.White,
                            contentDescription = stringResource(R.string.enter_pip),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.picture_in_picture),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        RegaIconButton(
                            onClick = onPrevious,
                            size = 48.dp,
                            shape = CircleShape,
                            containerColor = regaColors.glassTint,
                            borderColor = Color.White.copy(alpha = 0.35f),
                            contentColor = Color.White,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_previous),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        RegaPlayButton(
                            isPlaying = isPlaying,
                            onClick = onTogglePlayPause,
                            size = 64.dp,
                            containerColor = Color.White,
                            iconColor = Color.Black,
                        )
                        RegaIconButton(
                            onClick = onNext,
                            size = 48.dp,
                            shape = CircleShape,
                            containerColor = regaColors.glassTint,
                            borderColor = Color.White.copy(alpha = 0.35f),
                            contentColor = Color.White,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_next),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    ) {
                        val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
                        val displayPosition = sliderDragMs ?: positionMs
                        val clamped = displayPosition.coerceIn(0L, durationMs.coerceAtLeast(0L)).toFloat()
                        val sliderRange = 0f..safeDuration
                        val sliderState = remember(sliderRange) {
                            SliderState(value = clamped, trackRange = sliderRange)
                        }
                        sliderState.value = clamped
                        val interactionSource = remember { MutableInteractionSource() }
                        val isDragged by interactionSource.collectIsDraggedAsState()
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val isInteracting = isDragged || isPressed

                        Text(
                            text = makeTimeString(displayPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )

                        Slider(
                            state = sliderState,
                            onValueChange = { sliderDragMs = it.toLong() },
                            onValueChangeFinished = {
                                sliderDragMs?.let { mainPlayer.seekTo(it) }
                                sliderDragMs = null
                            },
                            interactionSource = interactionSource,
                            thumb = {
                                Box(
                                    modifier = Modifier.size(width = 32.dp, height = 22.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    RegaLiquidGlass(
                                        modifier = Modifier.size(
                                            width = if (isInteracting) 32.dp else 26.dp,
                                            height = if (isInteracting) 20.dp else 16.dp,
                                        ),
                                        shape = CircleShape,
                                        borderColor = Color.White.copy(alpha = 0.65f),
                                        style = RegaGlassStyle.preset(
                                            material = RegaGlassMaterial.Clear,
                                            tint = Color.White.copy(alpha = 0.22f),
                                            borderRadius = 10.dp,
                                        ),
                                    ) {}
                                }
                            },
                            track = { sState ->
                                val frac =
                                    if (sState.trackRange.endInclusive <= sState.trackRange.start) {
                                        0f
                                    } else {
                                        ((sState.value - sState.trackRange.start) / (sState.trackRange.endInclusive - sState.trackRange.start))
                                            .coerceIn(0f, 1f)
                                    }
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(22.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Canvas(
                                        modifier = Modifier.fillMaxWidth().height(if (isInteracting) 7.dp else 5.dp),
                                    ) {
                                        val h = size.height
                                        val r = CornerRadius(h / 2f, h / 2f)
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.26f),
                                            topLeft = Offset.Zero,
                                            size = size,
                                            cornerRadius = r,
                                        )
                                        val w = (size.width * frac).coerceAtLeast(if (frac > 0f) h else 0f)
                                        if (w > 0f) {
                                            drawRoundRect(
                                                color = Color.White,
                                                topLeft = Offset.Zero,
                                                size = Size(w, h),
                                                cornerRadius = r,
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )

                        Text(
                            text = if (durationMs > 0L) makeTimeString(durationMs) else "",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SyncedVideoPlayerSurface(
    videoId: String,
    isExplicit: Boolean?,
    isUploaded: Boolean?,
    mainPlayer: ExoPlayer,
    videoQuality: VideoQuality,
    isActive: Boolean,
    onVideoAspectRatioChanged: (Float) -> Unit,
    onVideoStateChanged: (VideoPlaybackUiState) -> Unit,
    modifier: Modifier = Modifier,
    videoCache: Cache? = null,
    enableVideoCache: Boolean = true,
    maxVideoCacheSizeMb: Int = 512,
) {
    val context = LocalContext.current
    val latestOnAspectRatio by rememberUpdatedState(onVideoAspectRatioChanged)
    val latestOnStateChanged by rememberUpdatedState(onVideoStateChanged)

    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var videoAspectRatio by remember(videoId) { mutableFloatStateOf(16f / 9f) }
    var isFirstFrameRendered by remember(videoId, videoQuality, isActive) { mutableStateOf(false) }
    var retryGeneration by remember(videoId, videoQuality) { mutableIntStateOf(0) }
    var activeStreamClient by remember(videoId, videoQuality) { mutableStateOf<String?>(null) }
    var activeDiskCacheKey by remember(videoId, videoQuality) { mutableStateOf<String?>(null) }
    var playedFromDiskCache by remember(videoId, videoQuality, retryGeneration) { mutableStateOf(false) }

    val videoPlayer = remember(isActive) {
        if (!isActive) {
            null
        } else {
            createCompanionVideoPlayer(context)
        }
    }

    DisposableEffect(videoPlayer) {
        onDispose {
            if (videoPlayer != null) {
                textureViewRef?.let { tv ->
                    tv.keepScreenOn = false
                    videoPlayer.clearVideoTextureView(tv)
                }
                videoPlayer.stop()
                videoPlayer.clearMediaItems()
                videoPlayer.release()
            }
        }
    }

    DisposableEffect(videoPlayer, textureViewRef) {
        val player = videoPlayer
        val view = textureViewRef
        if (player != null && view != null) {
            player.setVideoTextureView(view)
            applyAspectFitTransform(view, videoAspectRatio)
        }
        onDispose {
            if (player != null && view != null) {
                view.keepScreenOn = false
                player.clearVideoTextureView(view)
            }
        }
    }

    DisposableEffect(videoPlayer, mainPlayer, videoId, isActive) {
        val vp = videoPlayer
        if (!isActive || vp == null) {
            onDispose {}
        } else {
            val videoListener = object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        val ratio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
                        if (ratio.isFinite() && ratio > 0f) {
                            videoAspectRatio = ratio
                            latestOnAspectRatio(ratio)
                            textureViewRef?.let { applyAspectFitTransform(it, ratio) }
                        }
                    }
                }

                override fun onRenderedFirstFrame() {
                    isFirstFrameRendered = true
                    latestOnStateChanged(VideoPlaybackUiState.PLAYING)
                    textureViewRef?.keepScreenOn = mainPlayer.isPlaying
                    if (mainPlayer.currentMediaItem?.mediaId == videoId) {
                        val mainPos = mainPlayer.currentPosition.coerceAtLeast(0L)
                        val videoPos = vp.currentPosition.coerceAtLeast(0L)
                        if (abs(mainPos - videoPos) > 350L) {
                            vp.seekTo(mainPos)
                        }
                        vp.setPlaybackSpeed(mainPlayer.playbackParameters.speed)
                        vp.playWhenReady = mainPlayer.isPlaying
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Timber.tag("PlayerVideoSurface").w(error, "Video playback failed for %s", videoId)
                    isFirstFrameRendered = false
                    textureViewRef?.keepScreenOn = false
                    if (playedFromDiskCache && activeDiskCacheKey != null && videoCache != null) {
                        val staleKey = activeDiskCacheKey!!
                        runCatching { videoCache.removeResource(staleKey) }
                    } else {
                        activeStreamClient?.let { client ->
                            InnerTubeXPlayer.markVideoStreamClientFailed(videoId, client)
                        } ?: InnerTubeXPlayer.invalidateVideoStreamCache(videoId)
                    }
                    if (retryGeneration < 1) {
                        retryGeneration++
                    } else {
                        latestOnStateChanged(VideoPlaybackUiState.FAILED)
                    }
                }
            }

            val mainListener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (mainPlayer.currentMediaItem?.mediaId != videoId) return
                    vp.playWhenReady = isPlaying
                    textureViewRef?.keepScreenOn = isPlaying && isFirstFrameRendered
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (mainPlayer.currentMediaItem?.mediaId != videoId) return
                    when (playbackState) {
                        Player.STATE_READY -> {
                            vp.playWhenReady = mainPlayer.isPlaying
                            textureViewRef?.keepScreenOn = mainPlayer.isPlaying && isFirstFrameRendered
                        }

                        Player.STATE_BUFFERING,
                        Player.STATE_ENDED,
                        Player.STATE_IDLE,
                        -> {
                            vp.playWhenReady = false
                            textureViewRef?.keepScreenOn = false
                        }
                    }
                }

                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                    if (mainPlayer.currentMediaItem?.mediaId != videoId) return
                    vp.setPlaybackSpeed(playbackParameters.speed)
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    if (mainPlayer.currentMediaItem?.mediaId != videoId) return
                    if (vp.playbackState == Player.STATE_READY || vp.playbackState == Player.STATE_BUFFERING || vp.playbackState == Player.STATE_ENDED) {
                        val target = newPosition.positionMs.coerceAtLeast(0L)
                        val duration = vp.duration
                        vp.seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
                        vp.playWhenReady = mainPlayer.isPlaying
                    }
                }
            }

            vp.addListener(videoListener)
            mainPlayer.addListener(mainListener)
            onDispose {
                vp.removeListener(videoListener)
                mainPlayer.removeListener(mainListener)
            }
        }
    }

    LaunchedEffect(videoPlayer, videoId, videoQuality, isActive, retryGeneration, enableVideoCache, maxVideoCacheSizeMb) {
        val vp = videoPlayer
        if (!isActive || vp == null) {
            isFirstFrameRendered = false
            latestOnStateChanged(VideoPlaybackUiState.IDLE)
            return@LaunchedEffect
        }

        isFirstFrameRendered = false
        latestOnStateChanged(VideoPlaybackUiState.LOADING)
        vp.stop()
        vp.clearMediaItems()

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val preferredMaxHeight = videoQuality.resolveMaxHeight(connectivityManager.isActiveNetworkMetered)
        val useDiskCache = enableVideoCache && maxVideoCacheSizeMb != 0 && videoCache != null

        val completeCachedKey =
            if (useDiskCache && retryGeneration == 0) {
                withContext(Dispatchers.IO) {
                    CacheCleanupManager.findCompleteCachedVideoKey(
                        cache = videoCache!!,
                        videoId = videoId,
                        preferredMaxHeight = preferredMaxHeight,
                    )
                }
            } else {
                null
            }

        val dataSourceFactory: DataSource.Factory
        val mediaItem: MediaItem

        if (completeCachedKey != null && videoCache != null) {
            activeDiskCacheKey = completeCachedKey
            playedFromDiskCache = true
            activeStreamClient = null
            dataSourceFactory =
                CacheDataSource
                    .Factory()
                    .setCache(videoCache)
                    .setCacheWriteDataSinkFactory(null)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            mediaItem =
                MediaItem
                    .Builder()
                    .setUri("https://sonara.local/video/$completeCachedKey")
                    .setCustomCacheKey(completeCachedKey)
                    .build()
        } else {
            playedFromDiskCache = false
            val streamResult = withContext(Dispatchers.IO) {
                InnerTubeXPlayer.videoStreamForPlayback(
                    videoId = videoId,
                    videoQuality = videoQuality,
                    connectivityManager = connectivityManager,
                    contentHints = ContentHints(
                        isExplicit = isExplicit,
                        isUploaded = isUploaded,
                    ),
                    bypassCache = retryGeneration > 0,
                )
            }

            val playbackData = streamResult.getOrNull()
            if (playbackData == null) {
                Timber.tag("PlayerVideoSurface").w(streamResult.exceptionOrNull(), "Could not resolve video stream for %s", videoId)
                latestOnStateChanged(VideoPlaybackUiState.FAILED)
                return@LaunchedEffect
            }

            activeStreamClient = playbackData.streamClient
            val width = playbackData.width
            val height = playbackData.height
            if (width != null && height != null && width > 0 && height > 0) {
                val resolvedRatio = width.toFloat() / height.toFloat()
                if (resolvedRatio.isFinite() && resolvedRatio > 0f) {
                    videoAspectRatio = resolvedRatio
                    latestOnAspectRatio(resolvedRatio)
                    textureViewRef?.let { applyAspectFitTransform(it, resolvedRatio) }
                }
            }

            val okHttpClient = buildVideoOkHttpClient()
            val upstreamFactory =
                ChunkedRangeDataSource.Factory(
                    upstreamFactory = OkHttpDataSource.Factory(okHttpClient),
                    playbackData = playbackData,
                )
            if (useDiskCache && videoCache != null) {
                val cacheKey =
                    CacheCleanupManager.videoCacheKey(
                        videoId = videoId,
                        height = playbackData.height ?: preferredMaxHeight,
                        itag = playbackData.itag ?: 0,
                    )
                activeDiskCacheKey = cacheKey
                withContext(Dispatchers.IO) {
                    CacheCleanupManager.clearStalePartialVideoEntry(
                        cache = videoCache,
                        cacheKey = cacheKey,
                        expectedContentLength = playbackData.contentLengthBytes,
                    )
                }
                dataSourceFactory =
                    CacheDataSource
                        .Factory()
                        .setCache(videoCache)
                        .setUpstreamDataSourceFactory(upstreamFactory)
                        .setCacheWriteDataSinkFactory(
                            CacheDataSink
                                .Factory()
                                .setCache(videoCache)
                                .setFragmentSize(2 * 1024 * 1024L),
                        )
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                mediaItem =
                    MediaItem
                        .Builder()
                        .setUri(playbackData.streamUrl)
                        .setCustomCacheKey(cacheKey)
                        .build()
            } else {
                activeDiskCacheKey = null
                dataSourceFactory = upstreamFactory
                mediaItem = MediaItem.fromUri(playbackData.streamUrl)
            }
        }

        val mediaSource =
            DefaultMediaSourceFactory(
                dataSourceFactory,
                ExtractorsFactory {
                    arrayOf(MatroskaExtractor(), FragmentedMp4Extractor(), Mp4Extractor())
                },
            ).createMediaSource(mediaItem)

        vp.setMediaSource(mediaSource)
        vp.setPlaybackSpeed(mainPlayer.playbackParameters.speed)
        val initialPosition =
            if (mainPlayer.currentMediaItem?.mediaId == videoId) {
                mainPlayer.currentPosition.coerceAtLeast(0L)
            } else {
                0L
            }
        vp.seekTo(initialPosition)
        vp.playWhenReady = mainPlayer.isPlaying && mainPlayer.currentMediaItem?.mediaId == videoId
        vp.prepare()

        while (coroutineContext.isActive) {
            delay(450L)
            if (mainPlayer.currentMediaItem?.mediaId != videoId) continue
            val shouldPlay = mainPlayer.isPlaying && mainPlayer.playbackState == Player.STATE_READY
            if (vp.playWhenReady != shouldPlay) {
                vp.playWhenReady = shouldPlay
            }
            val mainSpeed = mainPlayer.playbackParameters.speed
            if (vp.playbackParameters.speed != mainSpeed) {
                vp.setPlaybackSpeed(mainSpeed)
            }
            textureViewRef?.keepScreenOn = shouldPlay && isFirstFrameRendered
            if (vp.playbackState == Player.STATE_READY || vp.playbackState == Player.STATE_ENDED) {
                val mainPos = mainPlayer.currentPosition.coerceAtLeast(0L)
                val videoPos = vp.currentPosition.coerceAtLeast(0L)
                val videoDuration = vp.duration
                val targetPos = if (videoDuration > 0L) mainPos.coerceAtMost(videoDuration) else mainPos
                if (abs(targetPos - videoPos) > 450L) {
                    vp.seekTo(targetPos)
                }
            }
        }
    }

    if (isActive) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        isClickable = false
                        isFocusable = false
                        addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                            applyAspectFitTransform(view as TextureView, videoAspectRatio)
                        }
                        textureViewRef = this
                    }
                },
                update = { view ->
                    if (textureViewRef !== view) {
                        textureViewRef = view
                    }
                    applyAspectFitTransform(view, videoAspectRatio)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun createCompanionVideoPlayer(context: Context): ExoPlayer {
    val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(
            buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true),
        )
    }
    return ExoPlayer
        .Builder(context)
        .setTrackSelector(trackSelector)
        .setLoadControl(
            DefaultLoadControl
                .Builder()
                .setBufferDurationsMs(15_000, 30_000, 500, 1_500)
                .build(),
        )
        .setAudioAttributes(AudioAttributes.DEFAULT, false)
        .setHandleAudioBecomingNoisy(false)
        .build()
        .apply {
            volume = 0f
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
}

private fun buildVideoOkHttpClient(): OkHttpClient = SharedHttpPool.proxiedClient()

internal fun applyAspectFitTransform(
    textureView: TextureView,
    videoAspectRatio: Float,
) {
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f || !videoAspectRatio.isFinite() || videoAspectRatio <= 0f) return
    val viewAspectRatio = viewWidth / viewHeight
    val scaleX: Float
    val scaleY: Float
    if (videoAspectRatio > viewAspectRatio) {
        scaleX = 1f
        scaleY = viewAspectRatio / videoAspectRatio
    } else {
        scaleX = videoAspectRatio / viewAspectRatio
        scaleY = 1f
    }
    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}
