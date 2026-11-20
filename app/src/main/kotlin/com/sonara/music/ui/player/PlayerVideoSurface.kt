package com.sonara.music.ui.player

import android.content.Context
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.sonara.innertube.YouTube
import com.sonara.music.R
import com.sonara.music.constants.VideoQuality
import com.sonara.music.db.entities.SongEntity
import com.sonara.music.models.MediaMetadata
import com.sonara.music.playback.CacheCleanupManager
import com.sonara.music.playback.ChunkedRangeDataSource
import com.sonara.music.ui.component.glass.RegaPillShape
import com.sonara.music.ui.component.glass.regaGlassSpecular
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.utils.InnerTubeXPlayer
import com.sonara.music.utils.InnerTubeXPlayer.resolveMaxHeight
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
    val activeBg = textColor.copy(alpha = 0.18f)
    val activeBorder = textColor.copy(alpha = 0.30f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .height(34.dp)
            .regaGlassSpecular(
                shape = RegaPillShape,
                tintColor = regaColors.glassTint,
                borderColor = regaColors.glassBorder,
            )
            .clip(RegaPillShape)
            .padding(3.dp),
    ) {
        VideoModePillSegment(
            selected = !isVideoMode,
            label = stringResource(R.string.player_mode_audio),
            iconRes = R.drawable.music_note,
            showLoading = false,
            textColor = textColor,
            activeBg = activeBg,
            activeBorder = activeBorder,
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
            activeBg = activeBg,
            activeBorder = activeBorder,
            onClick = {
                if (!isVideoMode) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectVideoMode(true)
                }
            },
        )
    }
}

@Composable
private fun VideoModePillSegment(
    selected: Boolean,
    label: String,
    iconRes: Int,
    showLoading: Boolean,
    textColor: Color,
    activeBg: Color,
    activeBorder: Color,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) activeBg else Color.Transparent,
        animationSpec = tween(180),
        label = "videoPillBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) activeBorder else Color.Transparent,
        animationSpec = tween(180),
        label = "videoPillBorder",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) textColor else textColor.copy(alpha = 0.62f),
        animationSpec = tween(180),
        label = "videoPillContent",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RegaPillShape)
            .background(bgColor)
            .border(0.75.dp, borderColor, RegaPillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
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

private object VideoHttpClientHolder {
    @Volatile
    private var cachedProxy: java.net.Proxy? = null

    @Volatile
    private var cachedProxyAuth: String? = null

    @Volatile
    private var cachedClient: OkHttpClient? = null

    @Synchronized
    fun get(): OkHttpClient {
        val currentProxy = YouTube.proxy
        val currentAuth = YouTube.proxyAuth
        val existing = cachedClient
        if (existing != null && cachedProxy == currentProxy && cachedProxyAuth == currentAuth) {
            return existing
        }
        val created =
            OkHttpClient
                .Builder()
                .proxy(currentProxy)
                .proxyAuthenticator { _, response ->
                    YouTube.proxyAuth?.let { auth ->
                        response.request
                            .newBuilder()
                            .header("Proxy-Authorization", auth)
                            .build()
                    } ?: response.request
                }
                .build()
        cachedProxy = currentProxy
        cachedProxyAuth = currentAuth
        cachedClient = created
        return created
    }
}

private fun buildVideoOkHttpClient(): OkHttpClient = VideoHttpClientHolder.get()

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
