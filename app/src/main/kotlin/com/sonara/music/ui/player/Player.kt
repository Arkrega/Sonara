/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.player

import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import com.sonara.music.LocalNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.sonara.music.LocalDatabase
import com.sonara.music.LocalDownloadUtil
import com.sonara.music.LocalListenTogetherManager
import com.sonara.music.LocalPlayerConnection
import com.sonara.music.R
import com.sonara.music.constants.CropAlbumArtKey
import com.sonara.music.constants.DarkModeKey
import com.sonara.music.constants.HidePlayerThumbnailKey
import com.sonara.music.constants.HideStatusBarOnFullscreenKey
import com.sonara.music.constants.KeepScreenOn
import com.sonara.music.constants.PlayerBackgroundStyle
import com.sonara.music.constants.PlayerBackgroundStyleKey
import com.sonara.music.constants.PlayerButtonsStyle
import com.sonara.music.constants.PlayerButtonsStyleKey
import com.sonara.music.constants.PlayerHorizontalPadding
import com.sonara.music.constants.QueuePeekHeight
import com.sonara.music.constants.SleepTimerDefaultKey
import com.sonara.music.constants.SleepTimerFadeOutKey
import com.sonara.music.constants.SleepTimerStopAfterCurrentSongKey
import com.sonara.music.constants.SliderStyle
import com.sonara.music.constants.SliderStyleKey
import com.sonara.music.constants.SquigglySliderKey
import com.sonara.music.constants.ThumbnailCornerRadius
import com.sonara.music.constants.UseNewPlayerDesignKey
import com.sonara.music.constants.VideoPlaybackEnabledKey
import com.sonara.music.db.entities.LyricsEntity
import com.sonara.music.extensions.metadata
import com.sonara.music.extensions.togglePlayPause
import com.sonara.music.extensions.toggleRepeatMode
import com.sonara.music.listentogether.RoomRole
import com.sonara.music.models.MediaMetadata
import com.sonara.music.ui.component.BottomSheet
import com.sonara.music.ui.component.BottomSheetState
import com.sonara.music.ui.component.DefaultDialog
import com.sonara.music.ui.component.LocalBottomSheetPageState
import com.sonara.music.ui.component.LocalMenuState
import com.sonara.music.ui.component.glass.LocalRegaBackdrop
import com.sonara.music.ui.component.glass.RegaGlassMaterial
import com.sonara.music.ui.component.glass.RegaGlassStyle
import com.sonara.music.ui.component.glass.RegaGlassSurface
import com.sonara.music.ui.component.glass.RegaIconButton
import com.sonara.music.ui.component.glass.RegaLiquidGlass
import com.sonara.music.ui.component.glass.RegaPillShape
import com.sonara.music.ui.component.glass.RegaPlayButton
import com.sonara.music.ui.component.glass.RegaSquircleShape
import com.sonara.music.ui.component.glass.regaGlassSpecular
import com.sonara.music.ui.component.glass.rememberRegaBackdropState
import com.sonara.music.ui.component.ExperimentalLyrics
import com.sonara.music.ui.component.OriginalLyrics
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.ui.component.Lyrics
import com.sonara.music.ui.component.AnimatedWaveform
import com.sonara.music.ui.component.PlayerSliderTrack
import com.sonara.music.ui.component.SquigglySlider
import com.sonara.music.ui.component.WavySlider
import com.sonara.music.ui.component.rememberBottomSheetState
import com.sonara.music.ui.menu.PlayerMenu
import com.sonara.music.ui.screens.settings.DarkMode
import com.sonara.music.ui.theme.LocalTabularTimeStyle
import com.sonara.music.ui.theme.PlayerColorExtractor
import com.sonara.music.ui.theme.PlayerSliderColors
import com.sonara.music.ui.utils.ShowMediaInfo
import com.sonara.music.ui.utils.ShowOffsetDialog
import com.sonara.music.utils.dataStore
import com.sonara.music.utils.makeTimeString
import com.sonara.music.utils.rememberEnumPreference
import com.sonara.music.utils.rememberPreference
import com.sonara.music.utils.safeDataStoreEdit
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import com.sonara.music.ui.component.Icon as MIcon


internal enum class NowPlayingViewMode {
    MAIN,
    LYRICS,
    QUEUE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val copiedTitleStr = stringResource(R.string.copied_title)
    val copiedArtistStr = stringResource(R.string.copied_artist)
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(
            UseNewPlayerDesignKey,
            defaultValue = true,
        )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (hideStatusBarOnFullscreen) = rememberPreference(HideStatusBarOnFullscreenKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val videoPlaybackEnabled by rememberPreference(VideoPlaybackEnabledKey, defaultValue = true)
    val lifecycleOwner = LocalLifecycleOwner.current
    var isPlayerForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isPlayerForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var viewMode by rememberSaveable {
        mutableStateOf(NowPlayingViewMode.MAIN)
    }

    var isFullScreen by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(state.isCollapsed) {
        if (state.isCollapsed && viewMode != NowPlayingViewMode.MAIN) {
            viewMode = NowPlayingViewMode.MAIN
        }
    }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT,
    )
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT,
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    val shouldUseDarkButtonColors =
        remember(playerBackground, useDarkTheme) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> true
                PlayerBackgroundStyle.DEFAULT -> useDarkTheme
            }
        }

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    DisposableEffect(playerBackground, state.isExpanded, useDarkTheme, keepScreenOn, isFullScreen, hideStatusBarOnFullscreen) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            when (playerBackground) {
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> {
                    insetsController.isAppearanceLightStatusBars = false
                }

                PlayerBackgroundStyle.DEFAULT -> {
                    insetsController.isAppearanceLightStatusBars = !useDarkTheme
                }
            }

            if (isFullScreen && hideStatusBarOnFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }

            if (keepScreenOn && state.isExpanded) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    BackHandler(enabled = state.isExpanded) {
        state.collapseSoft()
    }

    val onBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurface
        }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val squigglySlider by rememberPreference(SquigglySliderKey, defaultValue = false)

    // Listen Together state (reactive)
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if focus request fails
            }
        }
    }

    // Use Cast state when casting, otherwise local player
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    // Use State objects for position/duration to pass to MiniPlayer without causing recomposition
    // These states persist across playback state changes to ensure continuous progress updates.
    // Seed from the player's current values so re-entering composition on resume shows the real
    // time immediately instead of flashing 0:00 until the first poll fires. runCatching guards the
    // player-not-ready race; the poll loop corrects duration if it isn't known yet.
    val positionState = remember { mutableLongStateOf(runCatching { playerConnection.player.currentPosition }.getOrDefault(0L)) }
    val durationState = remember {
        mutableLongStateOf(
            (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: runCatching { playerConnection.player.duration }.getOrDefault(0L).coerceAtLeast(0L),
        )
    }

    // Convenience accessors for local use
    var position by positionState
    var duration by durationState

    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) {
                castPosition
            } else {
                position
            }
        }
    }

    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    // Track when we last manually set position to avoid Cast overwriting it
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id) {
        val currentMetadata = mediaMetadata
        val thumbUrl = currentMetadata?.thumbnailUrl
        if (currentMetadata == null || thumbUrl.isNullOrBlank()) {
            gradientColors = emptyList()
            return@LaunchedEffect
        }
        val cachedColors = gradientColorsCache[currentMetadata.id]
        if (cachedColors != null) {
            gradientColors = cachedColors
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(thumbUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .memoryCacheKey("gradient_${currentMetadata.id}")
                    .build()

            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            val bitmap = result?.image?.toBitmap()
            if (bitmap != null) {
                val palette =
                    withContext(Dispatchers.Default) {
                        Palette
                            .from(bitmap)
                            .maximumColorCount(8)
                            .resizeBitmapArea(100 * 100)
                            .generate()
                    }
                val extractedColors =
                    PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor,
                    )
                gradientColorsCache[currentMetadata.id] = extractedColors
                withContext(Dispatchers.Main) { gradientColors = extractedColors }
            }
        }
    }

    val TextBackgroundColor by animateColorAsState(
        targetValue =
            when (playerBackground) {
                PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
                PlayerBackgroundStyle.BLUR -> Color.White
                PlayerBackgroundStyle.GRADIENT -> Color.White
            },
        label = "TextBackgroundColor",
    )

    val icBackgroundColor by animateColorAsState(
        targetValue =
            when (playerBackground) {
                PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
                PlayerBackgroundStyle.BLUR -> Color.Black
                PlayerBackgroundStyle.GRADIENT -> Color.Black
            },
        label = "icBackgroundColor",
    )

    val (textButtonColor, iconButtonColor) =
        when {
            playerBackground == PlayerBackgroundStyle.BLUR ||
                playerBackground == PlayerBackgroundStyle.GRADIENT -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        Pair(Color.White, Color.Black)
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            }

            else -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        if (useDarkTheme) {
                            Pair(Color.White, Color.Black)
                        } else {
                            Pair(Color.Black, Color.White)
                        }
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            }
        }

    // Separate colors for Previous/Next buttons in PRIMARY/TERTIARY modes
    val (sideButtonContainerColor, sideButtonContentColor) =
        when {
            playerBackground == PlayerBackgroundStyle.BLUR ||
                playerBackground == PlayerBackgroundStyle.GRADIENT -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        Pair(
                            Color.White.copy(alpha = 0.2f),
                            Color.White,
                        )
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            else -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        Pair(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }

    val download by LocalDownloadUtil.current
        .getDownload(mediaMetadata?.id ?: "")
        .collectAsStateWithLifecycle(initialValue = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer?.triggerTime,
            playerConnection.service.sleepTimer?.pauseWhenSongEnd,
        ) {
            playerConnection.service.sleepTimer?.isActive ?: false
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer?.pauseWhenSongEnd == true) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        (playerConnection.service.sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    val scope = rememberCoroutineScope()
    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember {
        derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() }
    }
    LaunchedEffect(sleepTimerDefault) { sleepTimerValue = sleepTimerDefault }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)


    if (showSleepTimerDialog) {
        DefaultDialog(
            onDismiss = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            buttons = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer?.start(
                            minute = sleepTimerValue.roundToInt(),
                            stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                            fadeOut = sleepTimerFadeOut,
                        )
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt(),
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Slider(
                    value = sleepTimerValue,
                    onValueChange = { sleepTimerValue = it },
                    valueRange = 5f..120f,
                    steps = (120 - 5) / 5 - 1,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    com.sonara.music.ui.component.glass.RegaSecondaryButton(
                        onClick = {
                            scope.launch {
                                context.safeDataStoreEdit { settings ->
                                    settings[SleepTimerDefaultKey] = sleepTimerValue
                                }
                            }
                            Toast.makeText(
                                context,
                                String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        containerColor = if (isAtDefault) MaterialTheme.colorScheme.primary else LocalRegaColors.current.glassTint,
                        contentColor = if (isAtDefault) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text(stringResource(R.string.set_as_default))
                    }

                    com.sonara.music.ui.component.glass.RegaSecondaryButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer?.start(minute = -1)
                        },
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            }
        }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // Position update - only for local playback
    // When casting, we use castPosition directly to avoid sync issues
    // Use isPlaying instead of playbackState to ensure continuous updates during playback
    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100) // Update more frequently for smoother progress bar
                if (sliderPosition == null) { // Only update if user isn't dragging
                    position = playerConnection.player.currentPosition
                    // Don't clobber a valid (metadata-derived) duration with 0/UNSET mid-resolve.
                    playerConnection.player.duration.takeIf { it > 0 }?.let { duration = it }
                }
            }
        }
    }

    // Also update position when playback state changes (e.g., song change, seek)
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            // Prefer the song's known duration (from metadata, available instantly from the restored
            // queue) so the slider range is right even when restored paused / before the stream
            // resolves; fall back to the player's duration once it is known.
            duration = (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: playerConnection.player.duration
        }
    }

    // Auto-switch from repeat one to repeat all when song ends naturally
    var previousMediaId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playbackState, mediaMetadata?.id) {
        val currentId = mediaMetadata?.id

        // Only switch from REPEAT_ONE to REPEAT_ALL when playback naturally ended
        // (i.e., the player transitioned to ENDED state and then moved to next track).
        // Do NOT switch on manual skips.
        if (currentId != null &&
            currentId != previousMediaId &&
            previousMediaId != null &&
            playbackState == Player.STATE_ENDED &&
            repeatMode == Player.REPEAT_MODE_ONE &&
            !isListenTogetherGuest) {
            playerConnection.player.setRepeatMode(Player.REPEAT_MODE_ALL)
        }

        previousMediaId = currentId
    }

    // When casting, use Cast position/duration directly
    // But wait a bit after manual seeks to let Cast catch up
    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                // Only update from Cast if we haven't manually seeked recently
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }

    val dismissedBound =
        if (useNewPlayerDesign) {
            0.dp
        } else {
            QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        }

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = dismissedBound,
            expandedBound = state.expandedBound,
            collapsedBound = if (useNewPlayerDesign) 0.dp else dismissedBound + 1.dp,
            initialAnchor = if (useNewPlayerDesign) 0 else 1,
        )

    val topBarSubtitle =
        when (listenTogetherRoleState?.value) {
            RoomRole.HOST -> stringResource(R.string.listen_together_you_are_host)
            RoomRole.GUEST -> stringResource(R.string.listen_together_you_are_guest)
            else ->
                queueTitle
                    ?: mediaMetadata?.album?.title
                    ?: currentSong?.album?.title
                    ?: currentSong?.song?.albumName
        }

    val baseBackgroundColor =
        when {
            playerBackground == PlayerBackgroundStyle.BLUR ||
                playerBackground == PlayerBackgroundStyle.GRADIENT -> Color.Black
            useBlackBackground -> Color.Black
            useDarkTheme -> Color(0xFF070709)
            else -> Color(0xFFF6F3F0)
        }

    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val playerBackdropState = rememberRegaBackdropState()

    val database = LocalDatabase.current
    var isFetchingLyrics by remember(mediaMetadata?.id) { mutableStateOf(false) }

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        val currentMeta = mediaMetadata
        if (currentMeta != null && currentLyrics == null) {
            isFetchingLyrics = true
            try {
                withContext(Dispatchers.IO) {
                    val existing = database.lyrics(currentMeta.id).first()
                    if (existing != null) return@withContext
                    val entryPoint =
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.sonara.music.di.LyricsHelperEntryPoint::class.java,
                        )
                    val fetched = entryPoint.lyricsHelper().getLyrics(currentMeta)
                    database.query {
                        upsert(
                            LyricsEntity(
                                id = currentMeta.id,
                                lyrics = fetched.lyrics,
                                provider = fetched.provider,
                            ),
                        )
                    }
                }
            } catch (_: Exception) {
            } finally {
                isFetchingLyrics = false
            }
        } else {
            isFetchingLyrics = false
        }
    }

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            PlayerDynamicBackdrop(
                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                mediaId = mediaMetadata?.id,
                gradientColors = gradientColors,
                playerBackground = playerBackground,
                useDarkTheme = useDarkTheme,
                pureBlack = useBlackBackground,
                backgroundAlpha = backgroundAlpha,
                backdropState = playerBackdropState,
            )
        },
        onDismiss =
            if (!isListenTogetherGuest) {
                {
                    playerConnection.service.clearAutomix()
                    playerConnection.player.stop()
                    playerConnection.player.clearMediaItems()
                }
            } else {
                null
            },
        collapsedContent = {
            MiniPlayer(
                positionState = positionState,
                durationState = durationState,
                onClick = { state.expandSoft() },
            )
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata, Boolean) -> Unit = { mediaMetadata, compactMode ->
            val isEpisode = currentSong?.song?.isEpisode == true
            val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
            val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
            val unknownArtistStr = stringResource(R.string.widget_recognizer_unknown_artist)
            val artistName = mediaMetadata.artists.joinToString(", ") { it.name }.ifBlank { unknownArtistStr }
            val albumNavigationId = mediaMetadata.album?.id ?: currentSong?.album?.id ?: currentSong?.song?.albumId
            val controlsHorizontalPadding = if (compactMode) 16.dp else PlayerHorizontalPadding

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = controlsHorizontalPadding),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = mediaMetadata.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "trackTitle",
                    ) { title ->
                        Text(
                            text = title,
                            style = (if (compactMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
                                .copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextBackgroundColor,
                            modifier = Modifier
                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                .combinedClickable(
                                    enabled = true,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        if (albumNavigationId != null) {
                                            navController.navigate("album/$albumNavigationId")
                                            state.collapseSoft()
                                        }
                                    },
                                    onLongClick = {
                                        val clip = ClipData.newPlainText(copiedTitleStr, title)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast.makeText(context, copiedTitleStr, Toast.LENGTH_SHORT).show()
                                    },
                                ),
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (mediaMetadata.explicit) {
                            MIcon.Explicit()
                            Spacer(Modifier.width(5.dp))
                        }

                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.Medium,
                            color = TextBackgroundColor.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                .combinedClickable(
                                    enabled = true,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        val primaryArtistId = mediaMetadata.artists.firstOrNull()?.id
                                        if (!primaryArtistId.isNullOrBlank()) {
                                            navController.navigate("artist/$primaryArtistId")
                                            state.collapseSoft()
                                        } else if (albumNavigationId != null) {
                                            navController.navigate("album/$albumNavigationId")
                                            state.collapseSoft()
                                        }
                                    },
                                    onLongClick = {
                                        val clip = ClipData.newPlainText(copiedArtistStr, artistName)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast.makeText(context, copiedArtistStr, Toast.LENGTH_SHORT).show()
                                    },
                                ),
                        )
                    }
                }

                PlayerDockButton(
                    painter = painterResource(if (isFavorite) R.drawable.check else R.drawable.add),
                    contentDescription = stringResource(
                        if (isFavorite) R.string.action_remove_like else R.string.like,
                    ),
                    active = isFavorite,
                    onClick = playerConnection::toggleLike,
                    contentColor = TextBackgroundColor,
                    activeColor = textButtonColor,
                    size = if (compactMode) 36.dp else 42.dp,
                )
            }

            Spacer(Modifier.height(if (compactMode) 8.dp else 12.dp))

            PlayerPlaybackProgress(
                positionProvider = { effectivePosition },
                durationProvider = { duration },
                sliderPositionProvider = { sliderPosition },
                onSliderPositionChange = { sliderPosition = it },
                sliderStyle = sliderStyle,
                squigglySlider = squigglySlider,
                isListenTogetherGuest = isListenTogetherGuest,
                textButtonColor = textButtonColor,
                textBackgroundColor = TextBackgroundColor,
                playerBackground = playerBackground,
                useDarkTheme = useDarkTheme,
                effectiveIsPlaying = effectiveIsPlaying,
                horizontalPadding = controlsHorizontalPadding,
                onSeek = { seekPos ->
                    if (isCasting) {
                        castHandler?.seekTo(seekPos)
                        lastManualSeekTime = System.currentTimeMillis()
                    } else {
                        playerConnection.player.seekTo(seekPos)
                    }
                    position = seekPos
                },
            )

            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(if (compactMode) 6.dp else 12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = controlsHorizontalPadding + 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RegaIconButton(
                            onClick = {
                                if (!isListenTogetherGuest) {
                                    playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                                }
                            },
                            enabled = !isListenTogetherGuest,
                            size = if (compactMode) 36.dp else 42.dp,
                            shape = CircleShape,
                            containerColor = if (shuffleModeEnabled) {
                                TextBackgroundColor.copy(alpha = 0.14f)
                            } else {
                                Color.Transparent
                            },
                            borderColor = if (shuffleModeEnabled) {
                                TextBackgroundColor.copy(alpha = 0.35f)
                            } else {
                                Color.Transparent
                            },
                            contentColor = TextBackgroundColor,
                            contentDescription = stringResource(R.string.shuffle),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle,
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(if (compactMode) 19.dp else 21.dp),
                            )
                        }


                        RegaIconButton(
                            onClick = playerConnection::seekToPrevious,
                            enabled = canSkipPrevious && !isListenTogetherGuest,
                            size = if (compactMode) 44.dp else 56.dp,
                            shape = RegaSquircleShape,
                            containerColor = LocalRegaColors.current.glassTint,
                            borderColor = LocalRegaColors.current.glassBorder,
                            contentColor = TextBackgroundColor,
                            contentDescription = stringResource(R.string.previous),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_previous),
                                contentDescription = null,
                                modifier = Modifier.size(if (compactMode) 24.dp else 28.dp),
                            )
                        }


                        RegaPlayButton(
                            isPlaying = effectiveIsPlaying,
                            onClick = {
                                if (isListenTogetherGuest) {
                                    playerConnection.toggleMute()
                                    return@RegaPlayButton
                                }
                                if (isCasting) {
                                    if (castIsPlaying) {
                                        castHandler?.pause()
                                    } else {
                                        castHandler?.play()
                                    }
                                } else if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                            size = if (compactMode) 58.dp else 74.dp,
                            containerColor = textButtonColor,
                            iconColor = iconButtonColor,
                            contentDescription = stringResource(R.string.play_pause),
                            modifier = Modifier.focusRequester(focusRequester),
                        )


                        RegaIconButton(
                            onClick = playerConnection::seekToNext,
                            enabled = canSkipNext && !isListenTogetherGuest,
                            size = if (compactMode) 44.dp else 56.dp,
                            shape = RegaSquircleShape,
                            containerColor = LocalRegaColors.current.glassTint,
                            borderColor = LocalRegaColors.current.glassBorder,
                            contentColor = TextBackgroundColor,
                            contentDescription = stringResource(R.string.next),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_next),
                                contentDescription = null,
                                modifier = Modifier.size(if (compactMode) 24.dp else 28.dp),
                            )
                        }


                        RegaIconButton(
                            onClick = {
                                if (!isListenTogetherGuest) {
                                    playerConnection.player.toggleRepeatMode()
                                }
                            },
                            enabled = !isListenTogetherGuest,
                            size = if (compactMode) 36.dp else 42.dp,
                            shape = CircleShape,
                            containerColor = if (repeatMode != Player.REPEAT_MODE_OFF) {
                                TextBackgroundColor.copy(alpha = 0.14f)
                            } else {
                                Color.Transparent
                            },
                            borderColor = if (repeatMode != Player.REPEAT_MODE_OFF) {
                                TextBackgroundColor.copy(alpha = 0.35f)
                            } else {
                                Color.Transparent
                            },
                            contentColor = TextBackgroundColor,
                            contentDescription = stringResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                    Player.REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                    else -> R.string.repeat_mode_off
                                },
                            ),
                        ) {
                            Icon(
                                painter = painterResource(
                                    when (repeatMode) {
                                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                        Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                        else -> R.drawable.repeat
                                    },
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(if (compactMode) 19.dp else 21.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(if (compactMode) 8.dp else 12.dp))

                    val dockButtonSize = if (compactMode) 38.dp else 44.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = controlsHorizontalPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerDockButton(
                            painter = painterResource(R.drawable.lyrics),
                            contentDescription = stringResource(R.string.lyrics),
                            active = false,
                            onClick = { viewMode = NowPlayingViewMode.LYRICS },
                            contentColor = TextBackgroundColor,
                            activeColor = textButtonColor,
                            size = dockButtonSize,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlayerDockButton(
                                painter = painterResource(R.drawable.share),
                                contentDescription = stringResource(R.string.share),
                                active = false,
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "https://music.youtube.com/watch?v=${mediaMetadata.id}",
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, null))
                                },
                                contentColor = TextBackgroundColor,
                                activeColor = textButtonColor,
                                size = dockButtonSize,
                            )

                            PlayerDockButton(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = stringResource(R.string.queue),
                                active = false,
                                onClick = { viewMode = NowPlayingViewMode.QUEUE },
                                contentColor = TextBackgroundColor,
                                activeColor = textButtonColor,
                                size = dockButtonSize,
                            )
                        }
                    }

                    Spacer(Modifier.height(if (compactMode) 10.dp else 14.dp))

                    LiveLyricsStrip(
                        lyricsEntity = currentLyrics,
                        currentSong = currentSong,
                        songId = mediaMetadata.id,
                        playerConnection = playerConnection,
                        sliderPositionProvider = { sliderPosition },
                        textColor = TextBackgroundColor,
                        isExpanded = (state.isExpanded || state.progress > 0.25f) && viewMode == NowPlayingViewMode.MAIN,
                        isLoading = isFetchingLyrics,
                        compactMode = compactMode,
                        onOpenLyrics = { viewMode = NowPlayingViewMode.LYRICS },
                        modifier = Modifier.padding(horizontal = controlsHorizontalPadding),
                    )
                }
            }
        }
        val toggleMainPlayPause: () -> Unit = {
            if (isListenTogetherGuest) {
                playerConnection.toggleMute()
            } else if (isCasting) {
                if (castIsPlaying) {
                    castHandler?.pause()
                } else {
                    castHandler?.play()
                }
            } else if (playbackState == STATE_ENDED) {
                playerConnection.player.seekTo(0, 0)
                playerConnection.player.playWhenReady = true
            } else {
                playerConnection.player.togglePlayPause()
            }
        }

        val isCurrentTrackVideo = remember(mediaMetadata, currentSong?.song) {
            isVideoTrack(mediaMetadata, currentSong?.song)
        }
        var videoModeOverride by remember(mediaMetadata?.id, videoPlaybackEnabled) {
            mutableStateOf<Boolean?>(null)
        }
        val isVideoModeActive = isCurrentTrackVideo && (videoModeOverride ?: videoPlaybackEnabled)
        var videoUiState by remember(mediaMetadata?.id, isVideoModeActive) {
            mutableStateOf(VideoPlaybackUiState.IDLE)
        }
        val isVideoHostVisible =
            isPlayerForeground &&
                !isCasting &&
                viewMode == NowPlayingViewMode.MAIN &&
                (state.isExpanded || state.progress > 0.25f)

        CompositionLocalProvider(LocalRegaBackdrop provides playerBackdropState) {
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = {
                    fadeIn(tween(260)).togetherWith(fadeOut(tween(220)))
                },
                label = "nowPlayingViewMode",
            ) { currentView ->
                when (currentView) {
                    NowPlayingViewMode.MAIN -> {
                        when (LocalConfiguration.current.orientation) {
                            Configuration.ORIENTATION_LANDSCAPE -> {
                                val density = LocalDensity.current
                                val verticalPadding =
                                    max(
                                        WindowInsets.systemBars.getTop(density),
                                        WindowInsets.systemBars.getBottom(density),
                                    )
                                val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                                val verticalWindowInsets = WindowInsets(
                                    left = 0.dp,
                                    top = verticalPaddingDp,
                                    right = 0.dp,
                                    bottom = verticalPaddingDp,
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .windowInsetsPadding(
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .add(verticalWindowInsets),
                                        ),
                                ) {
                                    PlayerTopBar(
                                        subtitle = topBarSubtitle,
                                        textColor = TextBackgroundColor,
                                        onCollapse = { state.collapseSoft() },
                                        onMore = {
                                            mediaMetadata?.let { meta ->
                                                menuState.show {
                                                    PlayerMenu(
                                                        mediaMetadata = meta,
                                                        playerBottomSheetState = state,
                                                        onShowDetailsDialog = {
                                                            bottomSheetPageState.show { ShowMediaInfo(meta.id) }
                                                        },
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        },
                                        sleepTimerRemainingMs = if (sleepTimerEnabled && sleepTimerTimeLeft > 0L) sleepTimerTimeLeft else null,
                                        onClearSleepTimer = { playerConnection.service.sleepTimer?.clear() },
                                        compact = true,
                                        isPlaying = effectiveIsPlaying,
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = 8.dp),
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .weight(0.95f)
                                                .fillMaxSize()
                                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                                        ) {
                                            AnimatedVisibility(
                                                visible = isCurrentTrackVideo && !hidePlayerThumbnail,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically(),
                                            ) {
                                                PlayerVideoModeToggle(
                                                    isVideoMode = isVideoModeActive,
                                                    isLoading = videoUiState == VideoPlaybackUiState.LOADING,
                                                    textColor = TextBackgroundColor,
                                                    onSelectVideoMode = { enableVideo ->
                                                        videoModeOverride = enableVideo
                                                    },
                                                    modifier = Modifier.padding(bottom = 6.dp),
                                                )
                                            }
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                            ) {
                                                val currentSliderPosition by rememberUpdatedState(sliderPosition)
                                                val sliderPositionProvider = remember { { currentSliderPosition } }
                                                val isExpandedProvider = remember(state) { { state.isExpanded } }
                                                Thumbnail(
                                                    sliderPositionProvider = sliderPositionProvider,
                                                    modifier = Modifier.animateContentSize(),
                                                    isPlayerExpanded = isExpandedProvider,
                                                    isLandscape = true,
                                                    isListenTogetherGuest = isListenTogetherGuest,
                                                    isVideoModeEnabled = isVideoModeActive,
                                                    isVideoHostVisible = isVideoHostVisible,
                                                    onVideoStateChanged = { videoUiState = it },
                                                )
                                            }
                                        }

                                        BoxWithConstraints(
                                            modifier = Modifier
                                                .weight(1.05f, false)
                                                .fillMaxSize(),
                                        ) {
                                            val scrollState = rememberScrollState()
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(scrollState)
                                                    .padding(vertical = 4.dp),
                                            ) {
                                                mediaMetadata?.let {
                                                    controlsContent(it, true)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val isCompactHeight = maxHeight < 660.dp
                                    val coverSize = (maxWidth - 48.dp)
                                        .coerceAtMost(if (isCompactHeight) 240.dp else 320.dp)
                                        .coerceAtLeast(160.dp)
                                    val videoIdealHeight =
                                        (((maxWidth - 48.dp) * 9f / 16f) + 20.dp).coerceIn(170.dp, coverSize)
                                    val scrollState = rememberScrollState()
                                    val coverPx = with(LocalDensity.current) { coverSize.toPx() }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                            .statusBarsPadding()
                                            .navigationBarsPadding(),
                                    ) {
                                        PlayerTopBar(
                                            subtitle = topBarSubtitle,
                                            textColor = TextBackgroundColor,
                                            onCollapse = { state.collapseSoft() },
                                            onMore = {
                                            mediaMetadata?.let { meta ->
                                                menuState.show {
                                                    PlayerMenu(
                                                        mediaMetadata = meta,
                                                        playerBottomSheetState = state,
                                                        onShowDetailsDialog = {
                                                            bottomSheetPageState.show { ShowMediaInfo(meta.id) }
                                                        },
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        },
                                            sleepTimerRemainingMs = if (sleepTimerEnabled && sleepTimerTimeLeft > 0L) sleepTimerTimeLeft else null,
                                            onClearSleepTimer = { playerConnection.service.sleepTimer?.clear() },
                                            compact = isCompactHeight,
                                            isPlaying = effectiveIsPlaying,
                                        )

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .verticalScroll(scrollState),
                                        ) {
                                            val currentSliderPosition by rememberUpdatedState(sliderPosition)
                                            val sliderPositionProvider = remember { { currentSliderPosition } }
                                            val isExpandedProvider = remember(state) { { state.isExpanded } }
                                            val artworkContainerHeight by animateDpAsState(
                                                targetValue = if (isVideoModeActive && !hidePlayerThumbnail) videoIdealHeight else coverSize,
                                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 360f),
                                                label = "artworkContainerHeight",
                                            )

                                            AnimatedVisibility(
                                                visible = isCurrentTrackVideo && !hidePlayerThumbnail,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically(),
                                            ) {
                                                PlayerVideoModeToggle(
                                                    isVideoMode = isVideoModeActive,
                                                    isLoading = videoUiState == VideoPlaybackUiState.LOADING,
                                                    textColor = TextBackgroundColor,
                                                    onSelectVideoMode = { enableVideo ->
                                                        videoModeOverride = enableVideo
                                                    },
                                                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                                                )
                                            }

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(artworkContainerHeight)
                                                    .graphicsLayer {
                                                        val shrink =
                                                            (scrollState.value / coverPx).coerceIn(0f, 1f)
                                                        val scale = 1f - shrink * 0.06f
                                                        scaleX = scale
                                                        scaleY = scale
                                                        alpha = 1f - shrink * 0.22f
                                                    },
                                            ) {
                                                Thumbnail(
                                                    sliderPositionProvider = sliderPositionProvider,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 8.dp)
                                                        .nestedScroll(state.preUpPostDownNestedScrollConnection),
                                                    isPlayerExpanded = isExpandedProvider,
                                                    isListenTogetherGuest = isListenTogetherGuest,
                                                    isVideoModeEnabled = isVideoModeActive,
                                                    isVideoHostVisible = isVideoHostVisible,
                                                    onVideoStateChanged = { videoUiState = it },
                                                )
                                            }

                                            mediaMetadata?.let {
                                                controlsContent(it, isCompactHeight)
                                            }

                                            Spacer(Modifier.height(if (isCompactHeight) 10.dp else 18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    NowPlayingViewMode.LYRICS -> {
                        val currentSliderPosition by rememberUpdatedState(sliderPosition)
                        NowPlayingLyricsView(
                            mediaMetadata = mediaMetadata,
                            lyricsEntity = currentLyrics,
                            sliderPositionProvider = { currentSliderPosition },
                            isPlaying = effectiveIsPlaying,
                            isListenTogetherGuest = isListenTogetherGuest,
                            textColor = TextBackgroundColor,
                            primaryButtonColor = textButtonColor,
                            primaryIconColor = iconButtonColor,
                            onBackToMain = { viewMode = NowPlayingViewMode.MAIN },
                            onOpenQueue = { viewMode = NowPlayingViewMode.QUEUE },
                            onShowOffsetDialog = {
                                bottomSheetPageState.show {
                                    ShowOffsetDialog { currentSong?.song }
                                }
                            },
                            onPrev = playerConnection::seekToPrevious,
                            onPlayPause = toggleMainPlayPause,
                            onNext = playerConnection::seekToNext,
                            canSkipPrev = canSkipPrevious,
                            canSkipNext = canSkipNext,
                        )
                    }

                    NowPlayingViewMode.QUEUE -> {
                        NowPlayingQueueView(
                            playerBottomSheetState = state,
                            mediaMetadata = mediaMetadata,
                            pureBlack = pureBlack,
                            isPlaying = effectiveIsPlaying,
                            isListenTogetherGuest = isListenTogetherGuest,
                            textColor = TextBackgroundColor,
                            primaryButtonColor = textButtonColor,
                            primaryIconColor = iconButtonColor,
                            canSkipPrev = canSkipPrevious,
                            canSkipNext = canSkipNext,
                            onBackToMain = { viewMode = NowPlayingViewMode.MAIN },
                            onOpenLyrics = { viewMode = NowPlayingViewMode.LYRICS },
                            onPrev = playerConnection::seekToPrevious,
                            onPlayPause = toggleMainPlayPause,
                            onNext = playerConnection::seekToNext,
                        )
                    }
                }
            }
        }

        BackHandler(enabled = state.isExpanded && viewMode != NowPlayingViewMode.MAIN) {
            viewMode = NowPlayingViewMode.MAIN
        }
    }
}

@Composable
private fun PlayerTopBar(
    subtitle: String?,
    textColor: Color,
    onCollapse: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    sleepTimerRemainingMs: Long? = null,
    onClearSleepTimer: () -> Unit = {},
    compact: Boolean = false,
    isPlaying: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 16.dp else PlayerHorizontalPadding,
                vertical = if (compact) 4.dp else 8.dp,
            ),
    ) {
        RegaIconButton(
            onClick = onCollapse,
            size = if (compact) 38.dp else 42.dp,
            containerColor = LocalRegaColors.current.glassTint,
            borderColor = LocalRegaColors.current.glassBorder,
            contentColor = textColor,
        ) {
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                AnimatedWaveform(
                    isPlaying = isPlaying,
                    color = textColor,
                    barCount = 3,
                    barWidth = 1.5.dp,
                    barGap = 2.dp,
                    minBarHeight = 2.dp,
                    maxBarHeight = 10.dp,
                    modifier = Modifier.size(width = 10.dp, height = 10.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.now_playing).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = textColor.copy(alpha = 0.56f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 2500, velocity = 24.dp),
                )
            }

            if (sleepTimerRemainingMs != null) {
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .regaGlassSpecular(
                            shape = RegaPillShape,
                            tintColor = LocalRegaColors.current.glassTint,
                            borderColor = LocalRegaColors.current.glassBorder,
                        )
                        .clip(RegaPillShape)
                        .clickable(onClick = onClearSleepTimer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = stringResource(R.string.sleep_timer),
                        tint = textColor,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = makeTimeString(sleepTimerRemainingMs),
                        style = LocalTabularTimeStyle.current.copy(fontSize = 11.sp),
                        color = textColor,
                    )
                }
            }
        }

        RegaIconButton(
            onClick = onMore,
            size = if (compact) 38.dp else 42.dp,
            containerColor = LocalRegaColors.current.glassTint,
            borderColor = LocalRegaColors.current.glassBorder,
            contentColor = textColor,
            contentDescription = stringResource(R.string.more_options),
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PlayerLyricsPanel(
    isDarkSurface: Boolean,
    content: @Composable () -> Unit,
) {
    val tint = if (isDarkSurface) Color(0x470B0B0F) else Color(0x52FFFFFF)
    val border = if (isDarkSurface) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)
    RegaGlassSurface(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PlayerHorizontalPadding, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        tintColor = tint,
        borderColor = border,
    ) {
        content()
    }
}

@Composable
private fun PlayerDockButton(
    painter: Painter,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    contentColor: Color,
    activeColor: Color,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "dockButtonScale",
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(200),
        label = "dockActiveAlpha",
    )
    val resolvedTint by animateColorAsState(
        targetValue = when {
            !enabled -> contentColor.copy(alpha = 0.30f)
            active -> activeColor
            else -> contentColor.copy(alpha = 0.72f)
        },
        animationSpec = tween(180),
        label = "dockTint",
    )

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                },
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (indicatorAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(size - 6.dp)
                    .graphicsLayer { alpha = indicatorAlpha }
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.16f))
                    .border(0.75.dp, activeColor.copy(alpha = 0.28f), CircleShape),
            )
        }
        Icon(
            painter = painter,
            contentDescription = null,
            tint = resolvedTint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    sliderPositionProvider: () -> Long? = { null },
    useExperimentalLyrics: Boolean = true,
    onUserInteraction: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle(initialValue = -1)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    var appInForeground by remember {
        mutableStateOf(
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )
    }
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer =
            LifecycleEventObserver { _, _ ->
                appInForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val nextMetadata =
        remember(queueWindows, currentWindowIndex) {
            if (currentWindowIndex >= 0 && currentWindowIndex + 1 < queueWindows.size) {
                queueWindows[currentWindowIndex + 1].mediaItem.metadata
            } else {
                null
            }
        }

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint =
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.sonara.music.di.LyricsHelperEntryPoint::class.java,
                        )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(
        nextMetadata?.id,
        showLyrics,
        appInForeground,
        mediaMetadata?.id,
        currentLyrics,
    ) {
        if (!showLyrics || !appInForeground || nextMetadata == null) return@LaunchedEffect
        val loadedForCurrent =
            currentLyrics?.let { lyrics ->
                mediaMetadata == null || lyrics.id == mediaMetadata.id
            } == true
        if (mediaMetadata != null && !loadedForCurrent) return@LaunchedEffect
        val nextId = nextMetadata.id
        delay(400)
        if (!showLyrics || !appInForeground || !isActive) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val existing = database.lyrics(nextId).first()
                if (existing != null) return@withContext
                val entryPoint =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.sonara.music.di.LyricsHelperEntryPoint::class.java,
                    )
                val lyricsHelper = entryPoint.lyricsHelper()
                val fetched = lyricsHelper.getLyrics(nextMetadata)
                database.query {
                    upsert(LyricsEntity(nextId, fetched.lyrics, fetched.provider))
                }
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            lyrics == null -> {
                ContainedLoadingIndicator()
            }

            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    if (useExperimentalLyrics) {
                        ExperimentalLyrics(
                            sliderPositionProvider = sliderPositionProvider,
                            modifier = Modifier.padding(horizontal = 20.dp),
                            showLyrics = showLyrics,
                            onUserInteraction = onUserInteraction,
                        )
                    } else {
                        OriginalLyrics(
                            sliderPositionProvider = sliderPositionProvider,
                            modifier = Modifier.padding(horizontal = 20.dp),
                            showLyrics = showLyrics,
                        )
                    }
                }
                ProvideTextStyle(
                    value =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        ),
                ) {
                    lyricsContent()
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerPlaybackProgress(
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    sliderPositionProvider: () -> Long?,
    onSliderPositionChange: (Long?) -> Unit,
    sliderStyle: SliderStyle,
    squigglySlider: Boolean,
    isListenTogetherGuest: Boolean,
    textButtonColor: Color,
    textBackgroundColor: Color,
    playerBackground: PlayerBackgroundStyle,
    useDarkTheme: Boolean,
    effectiveIsPlaying: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp = PlayerHorizontalPadding,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = durationProvider()
    val safeDuration = if (duration == C.TIME_UNSET || duration < 0L) 0f else duration.toFloat()
    val sliderPosition = sliderPositionProvider()
    val displayPosition = sliderPosition ?: positionProvider()
    val clampedValue = displayPosition.toFloat().coerceIn(0f, safeDuration)
    val sliderColors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme)

    Column(modifier = modifier) {
        when (sliderStyle) {
            SliderStyle.DEFAULT -> {
                val interactionSource = remember { MutableInteractionSource() }
                val isDragged by interactionSource.collectIsDraggedAsState()
                val isPressed by interactionSource.collectIsPressedAsState()
                val isInteracting = isDragged || isPressed

                val trackHeight by animateDpAsState(
                    targetValue = if (isInteracting) 7.dp else 4.dp,
                    animationSpec = tween(150),
                    label = "trackHeight",
                )
                val thumbSize by animateDpAsState(
                    targetValue = if (isInteracting) 16.dp else 10.dp,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                    label = "thumbSize",
                )
                val sliderRange = 0f..safeDuration
                val sliderState = remember(sliderRange) {
                    SliderState(value = clampedValue, trackRange = sliderRange)
                }
                sliderState.value = clampedValue

                val inactiveColor =
                    if (playerBackground == PlayerBackgroundStyle.DEFAULT && !useDarkTheme) {
                        Color.Black.copy(alpha = 0.14f)
                    } else {
                        Color.White.copy(alpha = 0.22f)
                    }
                val activeColor = textButtonColor

                Slider(
                    state = sliderState,
                    onValueChange = {
                        if (!isListenTogetherGuest) {
                            onSliderPositionChange(it.toLong())
                        }
                    },
                    onValueChangeFinished = {
                        if (!isListenTogetherGuest) {
                            sliderPosition?.let { onSeek(it) }
                            onSliderPositionChange(null)
                        }
                    },
                    enabled = !isListenTogetherGuest,
                    interactionSource = interactionSource,
                    thumb = {
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(thumbSize)
                                    .shadow(
                                        elevation = if (isInteracting) 6.dp else 3.dp,
                                        shape = CircleShape,
                                        spotColor = Color.Black.copy(alpha = 0.45f),
                                    )
                                    .clip(CircleShape)
                                    .background(activeColor),
                            )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(trackHeight),
                            ) {
                                val h = size.height
                                val radius = CornerRadius(h / 2f, h / 2f)
                                drawRoundRect(
                                    color = inactiveColor,
                                    topLeft = Offset.Zero,
                                    size = size,
                                    cornerRadius = radius,
                                )
                                val activeW = (size.width * frac).coerceAtLeast(if (frac > 0f) h else 0f)
                                if (activeW > 0f) {
                                    drawRoundRect(
                                        color = activeColor,
                                        topLeft = Offset.Zero,
                                        size = Size(activeW, h),
                                        cornerRadius = radius,
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            }

            SliderStyle.WAVY -> {
                if (squigglySlider) {
                    SquigglySlider(
                        value = clampedValue,
                        valueRange = 0f..safeDuration,
                        onValueChange = {
                            if (!isListenTogetherGuest) {
                                onSliderPositionChange(it.toLong())
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherGuest) {
                                sliderPosition?.let { onSeek(it) }
                                onSliderPositionChange(null)
                            }
                        },
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                        colors = sliderColors,
                        isPlaying = effectiveIsPlaying,
                    )
                } else {
                    WavySlider(
                        value = clampedValue,
                        valueRange = 0f..safeDuration,
                        onValueChange = {
                            if (!isListenTogetherGuest) {
                                onSliderPositionChange(it.toLong())
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherGuest) {
                                sliderPosition?.let { onSeek(it) }
                                onSliderPositionChange(null)
                            }
                        },
                        colors = sliderColors,
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                        isPlaying = effectiveIsPlaying,
                    )
                }
            }

            SliderStyle.SLIM -> {
                val sliderRange = 0f..safeDuration
                val sliderState = remember(sliderRange) {
                    SliderState(value = clampedValue, trackRange = sliderRange)
                }
                sliderState.value = clampedValue
                Slider(
                    state = sliderState,
                    onValueChange = {
                        if (!isListenTogetherGuest) {
                            onSliderPositionChange(it.toLong())
                        }
                    },
                    onValueChangeFinished = {
                        if (!isListenTogetherGuest) {
                            sliderPosition?.let { onSeek(it) }
                            onSliderPositionChange(null)
                        }
                    },
                    enabled = !isListenTogetherGuest,
                    thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                    track = { sState ->
                        PlayerSliderTrack(
                            sliderState = sState,
                            colors = sliderColors,
                        )
                    },
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding + 4.dp),
        ) {
            Text(
                text = makeTimeString(displayPosition),
                style = LocalTabularTimeStyle.current,
                color = textBackgroundColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = if (duration != C.TIME_UNSET && duration > 0L) makeTimeString(duration) else "",
                style = LocalTabularTimeStyle.current,
                color = textBackgroundColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
