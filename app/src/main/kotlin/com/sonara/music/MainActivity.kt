/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonara.innertube.YouTube
import com.sonara.innertube.models.SongItem
import com.sonara.innertube.models.WatchEndpoint
import com.sonara.music.constants.AppBarHeight
import com.sonara.music.constants.AppLanguageKey
import com.sonara.music.constants.CheckForUpdatesKey
import com.sonara.music.constants.DarkModeKey
import com.sonara.music.constants.InnerTubeCookieKey
import com.sonara.music.constants.IntroCompletedKey
import com.sonara.music.constants.OnboardingStateKey
import com.sonara.music.constants.DefaultOpenTabKey
import com.sonara.music.constants.DismissedKmpUpdateKey
import com.sonara.music.constants.DismissedStandaloneUpdateKey
import com.sonara.music.constants.DensityScaleKey
import com.sonara.music.constants.DisableScreenshotKey
import com.sonara.music.constants.EnableHighRefreshRateKey
import com.sonara.music.constants.EnableLandscapeScalingKey
import com.sonara.music.constants.EnableLiquidGlassKey
import com.sonara.music.constants.ExperimentalLyricsKey
import com.sonara.music.constants.ForceBottomNavBarKey
import com.sonara.music.constants.LastSeenVersionKey
import com.sonara.music.constants.ListenTogetherInTopBarKey
import com.sonara.music.constants.ListenTogetherUsernameKey
import com.sonara.music.constants.LyricsProviderOrderKey
import com.sonara.music.constants.MiniPlayerBottomSpacing
import com.sonara.music.constants.MiniPlayerHeight
import com.sonara.music.constants.NavigationBarAnimationSpec
import com.sonara.music.constants.NavigationBarHeight
import com.sonara.music.constants.PauseListenHistoryKey
import com.sonara.music.constants.PauseSearchHistoryKey
import com.sonara.music.constants.PreferredLyricsProvider
import com.sonara.music.constants.PreferredLyricsProviderKey
import com.sonara.music.constants.PureBlackKey
import com.sonara.music.constants.SYSTEM_DEFAULT
import com.sonara.music.constants.SelectedThemeColorKey
import com.sonara.music.constants.SimpMusicMigrationDoneKey
import com.sonara.music.constants.SlimNavBarHeight
import com.sonara.music.constants.SlimNavBarKey
import com.sonara.music.constants.StopMusicOnTaskClearKey
import com.sonara.music.constants.UpdateNotificationsEnabledKey
import com.sonara.music.constants.UpdatePromptDateKey
import com.sonara.music.constants.UseNewMiniPlayerDesignKey
import com.sonara.music.constants.VideoThumbnailMigrationDoneKey
import com.sonara.music.db.MusicDatabase
import com.sonara.music.db.entities.SearchHistory
import com.sonara.music.extensions.toEnum
import com.sonara.music.lyrics.LyricsProviderRegistry
import com.sonara.music.models.toMediaMetadata
import com.sonara.music.playback.DownloadUtil
import com.sonara.music.playback.MusicService
import com.sonara.music.playback.MusicService.MusicBinder
import com.sonara.music.playback.PlayerConnection
import com.sonara.music.playback.queues.YouTubeQueue
import com.sonara.music.ui.component.AccountSettingsDialog
import com.sonara.music.ui.component.AppNavigationBar
import com.sonara.music.ui.component.AppNavigationRail
import com.sonara.music.ui.component.BottomSheetMenu
import com.sonara.music.ui.component.BottomSheetPage
import com.sonara.music.ui.component.LocalBottomSheetPageState
import com.sonara.music.ui.component.LocalMenuState
import com.sonara.music.ui.component.rememberBottomSheetState
import com.sonara.music.ui.component.glass.LocalLiquidGlassEnabled
import com.sonara.music.ui.component.glass.LocalRegaBackdrop
import com.sonara.music.ui.component.glass.regaBackdropSource
import com.sonara.music.ui.component.glass.regaGlassSpecular
import com.sonara.music.ui.component.glass.rememberRegaBackdropState
import com.sonara.music.ui.menu.YouTubeSongMenu
import com.sonara.music.ui.player.BottomSheetPlayer
import com.sonara.music.ui.screens.Screens
import com.sonara.music.ui.screens.SonaraOnboarding
import com.sonara.music.ui.screens.navigationBuilder
import com.sonara.music.ui.screens.settings.ChangelogScreen
import com.sonara.music.ui.screens.settings.DarkMode
import com.sonara.music.ui.screens.settings.NavigationTab
import com.sonara.music.ui.theme.DefaultThemeColor
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.ui.theme.SonaraMotion
import com.sonara.music.ui.theme.SonaraTheme
import com.sonara.music.ui.utils.appBarScrollBehavior
import com.sonara.music.ui.utils.resetHeightOffset
import com.sonara.music.utils.ReleaseInfo
import com.sonara.music.utils.SearchRoutes
import com.sonara.music.utils.SyncUtils
import com.sonara.music.utils.ArtistNameAliases
import com.sonara.music.utils.Updater
import com.sonara.music.utils.OnboardingStatus
import com.sonara.music.utils.dataStore
import com.sonara.music.utils.resolveOnboardingStatus
import com.sonara.music.utils.safeDataStoreEdit
import com.sonara.music.utils.get
import com.sonara.music.utils.rememberEnumPreference
import com.sonara.music.utils.applyRefreshRatePreference
import com.sonara.music.utils.rememberPreference
import com.sonara.music.utils.reportException
import com.sonara.music.utils.setAppLocale
import com.sonara.music.viewmodels.HomeViewModel
import com.sonara.music.widget.PlaylistWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

import com.sonara.music.utils.AppUpdateDownloader
import com.sonara.music.utils.UpdateDownloadState
import androidx.compose.ui.text.font.FontWeight

private data class AvailableUpdate(
    val release: ReleaseInfo,
    val downloadUrl: String,
    val isKmp: Boolean = false,
) {
    val dismissalKey = DismissedStandaloneUpdateKey
}

/** Every step the in-app updater can be in, so the UI can always explain itself. */
private sealed interface UpdateFlowState {
    data object PermissionRequired : UpdateFlowState
    data class Downloading(val progress: Float) : UpdateFlowState
    data object Verifying : UpdateFlowState
    data class Failed(val detail: String?) : UpdateFlowState
}

@Composable
internal fun UpdateProgressContent(
    status: String,
    progress: Float?,
    hint: String,
) {
    Column {
        Text(text = status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpdateProgressDialog(
    title: String,
    status: String,
    progress: Float?,
    hint: String,
    onHide: () -> Unit,
) {
    AlertDialog(
        // A running download must not be lost to a stray tap outside the dialog.
        onDismissRequest = {},
        title = { Text(title) },
        text = { UpdateProgressContent(status = status, progress = progress, hint = hint) },
        confirmButton = {
            TextButton(onClick = onHide) {
                Text(stringResource(R.string.update_hide_progress))
            }
        },
    )
}

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    companion object {
        private const val ACTION_SEARCH = "com.sonara.music.action.SEARCH"
        private const val ACTION_LIBRARY = "com.sonara.music.action.LIBRARY"
        const val ACTION_RECOGNITION = "com.sonara.music.action.RECOGNITION"
        const val ACTION_OPEN_WIDGET_TARGET = "com.sonara.music.action.OPEN_WIDGET_TARGET"
        const val EXTRA_AUTO_START_RECOGNITION = "auto_start_recognition"
        const val EXTRA_WIDGET_TARGET_TYPE = "widget_target_type"
        const val EXTRA_WIDGET_TARGET_ID = "widget_target_id"

        // Intro replays on every cold launch, but survives activity recreation
        // (rotation, theme change) without restarting the five second card.
        @Volatile
        private var introPlayedThisProcess = false
    }

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var listenTogetherManager: com.sonara.music.listentogether.ListenTogetherManager

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var latestVersionName by mutableStateOf(BuildConfig.BASE_VERSION_NAME)

    // Keep PlayerConnection as regular property - NOT mutableStateOf to prevent UI recomposition
    // when it becomes null during onStop. Only update the snapshot for Compose when needed.
    private var playerConnection: PlayerConnection? = null

    // This is the snapshot we pass to Compose - changes here trigger recomposition
    private var playerConnectionSnapshot by mutableStateOf<PlayerConnection?>(null)

    private var isServiceBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    playerConnectionSnapshot = playerConnection
                    listenTogetherManager.setPlayerConnection(playerConnection)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Disconnect Listen Together manager
                listenTogetherManager.setPlayerConnection(null)
                playerConnection?.dispose()
                // DO NOT null out playerConnection here - keep it for when service reconnects
                // DO NOT update playerConnectionSnapshot - this is the key to preventing recomposition
            }
        }

    private fun safeUnbindService(source: String) {
        if (!isServiceBound) return
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            Timber.tag("MainActivity").w(e, "Service was not bound when attempting to unbind in $source")
        } finally {
            isServiceBound = false
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
            // DO NOT null out playerConnection here - keep it for reconnection
            // DO NOT update playerConnectionSnapshot - this prevents UI recomposition
        }
    }

    override fun onStart() {
        super.onStart()
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }

        // Start the playback service explicitly once so it can outlive binding.
        // Re-issuing startForegroundService() while an existing service instance is already
        // running can trigger "did not then call startForeground" on some Android 9 devices
        // when the framework expects a fresh foreground promotion for that start request.
        if (!MusicService.isRunning) {
            val serviceIntent = Intent(this, MusicService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.startForegroundService(this, serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Timber.w(e, "Cannot start foreground service from background")
            } catch (e: IllegalStateException) {
                Timber.w(e, "Failed to start foreground service")
            }
        }

        // Bind to service - if already bound, this is a no-op but ensures we stay connected
        if (!isServiceBound) {
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE,
            )
            isServiceBound = true
        }
    }

    override fun onStop() {
        // Keep the service binding, PlayerConnection and Listen Together wiring alive while
        // the Activity is backgrounded. The MusicService is a foreground service and keeps
        // running, so the host must keep reporting playback state to the LT server; detaching
        // the player listener here used to break LT for any host that wasn't staring at the
        // app the whole session. Full teardown happens in onDestroy() via safeUnbindService().
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) {
            listenTogetherManager.disconnect()
        }
        super.onDestroy()
        val stopServiceOnClear = dataStore.get(StopMusicOnTaskClearKey, false) && isFinishing

        // Full cleanup - only on actual destroy
        playerConnection?.dispose()
        playerConnection = null
        playerConnectionSnapshot = null

        // Unbind before stopService: a started+bound service does not stop until all clients unbind.
        safeUnbindService("onDestroy()")

        if (stopServiceOnClear) {
            stopService(Intent(this, MusicService::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            handleWidgetTargetIntent(intent, navController)
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Listen Together manager
        listenTogetherManager.initialize()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale =
                dataStore[AppLanguageKey]
                    ?.takeUnless { it == SYSTEM_DEFAULT }
                    ?.let { Locale.forLanguageTag(it) }
                    ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        // Defer migration and version tracking to avoid blocking first frame
        lifecycleScope.launch(Dispatchers.IO) {
            val preferences = dataStore.data.first()
            val currentVersion = BuildConfig.BASE_VERSION_NAME

            // SimpMusic Removal Migration
            if (preferences[SimpMusicMigrationDoneKey] != true) {
                safeDataStoreEdit { settings ->
                    val currentOrder = settings[LyricsProviderOrderKey] ?: ""
                    if (currentOrder.contains("SimpMusic")) {
                        val orderList =
                            currentOrder
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() && it != "SimpMusic" }
                                .toMutableList()
                        if (orderList.isEmpty()) {
                            settings[LyricsProviderOrderKey] = ""
                        } else {
                            settings[LyricsProviderOrderKey] = orderList.joinToString(",")
                        }
                    }
                    if (settings[PreferredLyricsProviderKey] == "SIMPMUSIC") {
                        settings[PreferredLyricsProviderKey] = PreferredLyricsProvider.LRCLIB.name
                    }
                    settings[SimpMusicMigrationDoneKey] = true
                    settings[LastSeenVersionKey] = currentVersion
                }
            }

            if (preferences[VideoThumbnailMigrationDoneKey] != true) {
                database.repairMissingVideoThumbnails()
                safeDataStoreEdit { settings ->
                    settings[VideoThumbnailMigrationDoneKey] = true
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            safeDataStoreEdit { settings ->
                settings[LastSeenVersionKey] = BuildConfig.BASE_VERSION_NAME
            }
        }

        setContent {
            SonaraApp(
                latestVersionName = latestVersionName,
                onLatestVersionNameChange = { latestVersionName = it },
                playerConnection = playerConnectionSnapshot,
                database = database,
                downloadUtil = downloadUtil,
                syncUtils = syncUtils,
            )
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SonaraApp(
        latestVersionName: String,
        onLatestVersionNameChange: (String) -> Unit,
        playerConnection: PlayerConnection?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        syncUtils: SyncUtils,
    ) {
        val checkForUpdates by rememberPreference(CheckForUpdatesKey, defaultValue = true)
        var availableUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }
        // Opened either by the once-a-day prompt or by the "New Update" chip.
        var showUpdateDialog by remember { mutableStateOf(false) }

        if (BuildConfig.UPDATER_AVAILABLE) {
            LaunchedEffect(checkForUpdates) {
                if (checkForUpdates) {
                    val preferences = dataStore.data.first()
                    val notificationsEnabled = preferences[UpdateNotificationsEnabledKey] ?: true
                    val (releaseInfo, hasUpdate) = Updater.checkForUpdate().getOrNull() ?: (null to false)
                    releaseInfo?.let { onLatestVersionNameChange(it.versionName) }

                    val standaloneUpdate =
                        releaseInfo
                            ?.takeIf { hasUpdate }
                            ?.let { release ->
                                Updater.getDownloadUrlForCurrentVariant(release)?.let { downloadUrl ->
                                    AvailableUpdate(release, downloadUrl, isKmp = false)
                                }
                            }
                    val update = standaloneUpdate
                    // Keep the release around: the top bar chip shows it until the
                    // user actually installs it, however often they dismissed it.
                    availableUpdate = update
                    if (update != null) {
                        val today = LocalDate.now().toEpochDay()
                        val lastPrompt = preferences[UpdatePromptDateKey] ?: 0L
                        if (today > lastPrompt) showUpdateDialog = true
                    }

                    if (update != null && notificationsEnabled) {
                        // Open the in-app sheet: the APK is downloaded and installed
                        // here, no detour through the browser or GitHub.
                        val intent =
                            Intent(this@MainActivity, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        val pending = PendingIntent.getActivity(this@MainActivity, 1001, intent, flags)
                        val notificationText = update.release.versionName
                        val notification =
                            NotificationCompat
                                .Builder(this@MainActivity, "updates")
                                .setSmallIcon(R.drawable.update)
                                .setContentTitle(getString(R.string.update_available_title))
                                .setContentText(notificationText)
                                .setContentIntent(pending)
                                .setAutoCancel(true)
                                .build()

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            NotificationManagerCompat.from(this@MainActivity).notify(1001, notification)
                        }
                    }
                } else {
                    onLatestVersionNameChange(BuildConfig.BASE_VERSION_NAME)
                    availableUpdate = null
                }
            }
        }

        val enableHighRefreshRate by rememberPreference(EnableHighRefreshRateKey, defaultValue = true)

        // MONOCHROME SONIC supports 120/144 Hz panels: animations run on Compose's
        // frame clock and the window asks the display for its fastest mode.
        LaunchedEffect(enableHighRefreshRate) {
            applyRefreshRatePreference(enableHighRefreshRate)
        }

        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme =
            remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }

        LaunchedEffect(useDarkTheme) {
            setSystemBarAppearance(useDarkTheme)
        }

        val enableLandscapeScaling by rememberPreference(EnableLandscapeScalingKey, defaultValue = false)
        val userDensityScale by rememberPreference(DensityScaleKey, defaultValue = 1f)
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val pureBlack =
            remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            }

        val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
        val selectedThemeColor = Color(selectedThemeColorInt)

        val showChangelog = rememberSaveable { mutableStateOf(false) }

        // Launch gate. `resolved` stays false until DataStore answers so a cold
        // start shows a black frame instead of Home before the intro.
        var onboardingResolved by remember { mutableStateOf(false) }
        var onboardingPending by remember { mutableStateOf(false) }
        var launchGateVisible by remember { mutableStateOf(!introPlayedThisProcess) }
        LaunchedEffect(Unit) {
            val preferences = runCatching { dataStore.data.first() }.getOrNull()
            val stored = preferences?.get(OnboardingStateKey)
            val hasAccountSession = preferences?.get(InnerTubeCookieKey)?.contains("SAPISID") == true
            val status = resolveOnboardingStatus(
                storedValue = stored,
                legacyIntroCompleted = preferences?.get(IntroCompletedKey) == true,
                hasAccountSession = hasAccountSession,
            )
            if (preferences != null && stored == null && status != OnboardingStatus.PENDING) {
                safeDataStoreEdit {
                    it[OnboardingStateKey] = status.value
                    it[IntroCompletedKey] = true
                }
            }
            onboardingPending = status == OnboardingStatus.PENDING
            if (onboardingPending) launchGateVisible = true
            onboardingResolved = true
        }

        // The intro is always black, so force light system-bar icons until the
        // gate is gone even when the app is configured for the light theme.
        LaunchedEffect(useDarkTheme, onboardingResolved, launchGateVisible) {
            val onboardingActive = !onboardingResolved || launchGateVisible
            setSystemBarAppearance(if (onboardingActive) true else useDarkTheme)
        }

        // MONOCHROME SONIC: the palette is fixed. Album-art colour extraction used to
        // run on every track change (bitmap decode + palette scoring) and no longer
        // has any visual effect, so it is gone along with its per-track work.
        val themeColor = selectedThemeColor

        SonaraTheme(
            darkTheme = useDarkTheme,
            pureBlack = pureBlack,
            themeColor = themeColor,
        ) {
            val currentDensity = LocalDensity.current
            val windowInfo = LocalWindowInfo.current
            val containerSize = windowInfo.containerDpSize
            val smallestDimensionDp = minOf(containerSize.width, containerSize.height)

            val landscapeDensityScale =
                remember(smallestDimensionDp, enableLandscapeScaling) {
                    if (enableLandscapeScaling) {
                        when {
                            smallestDimensionDp >= 840.dp -> 1.15f
                            smallestDimensionDp >= 720.dp -> 1.1f
                            smallestDimensionDp >= 600.dp -> 1.05f
                            else -> 1.0f
                        }
                    } else {
                        1.0f
                    }
                }
            val scaledDensity: Density = remember(currentDensity, landscapeDensityScale, userDensityScale) {
                Density(
                    density = currentDensity.density * landscapeDensityScale * userDensityScale,
                    fontScale = currentDensity.fontScale,
                )
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface),
            ) {
                val density = LocalDensity.current
                val configuration = LocalWindowInfo.current
                val cutoutInsets = WindowInsets.displayCutout
                val windowsInsets = WindowInsets.systemBars
                val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    val lastSeenVersion = dataStore.data.first()[LastSeenVersionKey] ?: ""
                    val currentVersion = BuildConfig.BASE_VERSION_NAME
                    if (lastSeenVersion != currentVersion) {
                        showChangelog.value = true
                    }
                }

                val homeViewModel: HomeViewModel = hiltViewModel()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

                val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
                val navigationItems =
                    remember(listenTogetherInTopBar) {
                        if (listenTogetherInTopBar) {
                            Screens.MainScreens.filter { it != Screens.ListenTogether }
                        } else {
                            Screens.MainScreens
                        }
                    }
                val routeIndexMap = remember(navigationItems) {
                    navigationItems.mapIndexed { i, s -> s.route to i }.toMap()
                }
                val (forceBottomNav) = rememberPreference(ForceBottomNavBarKey, defaultValue = false)
                val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
                val (defaultOpenTabInt) = rememberPreference(DefaultOpenTabKey, defaultValue = NavigationTab.HOME.name)
                val defaultOpenTab = remember(defaultOpenTabInt) {
                    try {
                        NavigationTab.valueOf(defaultOpenTabInt)
                    } catch (_: IllegalArgumentException) {
                        NavigationTab.HOME
                    }
                }
                val tabOpenedFromShortcut =
                    remember {
                        when (intent?.action) {
                            ACTION_SEARCH -> NavigationTab.SEARCH
                            ACTION_LIBRARY -> NavigationTab.LIBRARY
                            else -> null
                        }
                    }

                val topLevelScreens =
                    remember {
                        listOf(
                            Screens.Home.route,
                            Screens.Library.route,
                            Screens.ListenTogether.route,
                            "settings",
                        )
                    }

                val (query, onQueryChange) =
                    rememberSaveable(stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue())
                    }

                val onSearch: (String) -> Unit =
                    remember {
                        { searchQuery ->
                            if (searchQuery.isNotEmpty()) {
                                navController.navigate(SearchRoutes.resultRoute(searchQuery))

                                if (dataStore[PauseSearchHistoryKey] != true) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        runCatching {
                                            database.insert(SearchHistory(query = searchQuery))
                                        }.onFailure { throwable ->
                                            Timber
                                                .tag("MainActivity")
                                                .w(throwable, "Failed to save search history for query: %s", searchQuery)
                                        }
                                    }
                                }
                            }
                        }
                    }

                val currentRoute by remember {
                    derivedStateOf { navBackStackEntry?.destination?.route }
                }

                val inSearchScreen by remember {
                    derivedStateOf { currentRoute?.startsWith("search/") == true }
                }
                val navigationItemRoutes =
                    remember(navigationItems) {
                        navigationItems.map { it.route }.toSet()
                    }

                val shouldShowNavigationBar =
                    remember(currentRoute, navigationItemRoutes) {
                        currentRoute == null ||
                            navigationItemRoutes.contains(currentRoute) ||
                            currentRoute!!.startsWith("search/")
                    }

                val isLandscape = configuration.containerDpSize.width > configuration.containerDpSize.height
                val isTablet = configuration.containerDpSize.width >= 600.dp

                val showRail = !forceBottomNav && (isLandscape || isTablet) && !inSearchScreen

                val navPadding =
                    if (shouldShowNavigationBar && !showRail) {
                        if (slimNav) SlimNavBarHeight else NavigationBarHeight
                    } else {
                        0.dp
                    }

                val navigationBarHeight by animateDpAsState(
                    targetValue = if (shouldShowNavigationBar && !showRail) NavigationBarHeight else 0.dp,
                    animationSpec = NavigationBarAnimationSpec,
                    label = "navBarHeight",
                )

                val playerBottomSheetState =
                    rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound =
                            bottomInset +
                                (if (!showRail && shouldShowNavigationBar) navPadding else 0.dp) +
                                (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                                MiniPlayerHeight,
                        expandedBound = maxHeight,
                    )

                val playerReadyState =
                    playerConnection?.service?.isPlayerReady?.collectAsStateWithLifecycle()
                        ?: remember { mutableStateOf(false) }
                val playerReady by playerReadyState
                val activePlayerConnection = if (playerReady) playerConnection else null

                val playerAwareWindowInsets =
                    remember(
                        bottomInset,
                        shouldShowNavigationBar,
                        playerBottomSheetState.isDismissed,
                        showRail,
                    ) {
                        var bottom = bottomInset
                        if (shouldShowNavigationBar && !showRail) {
                            bottom += NavigationBarHeight
                        }
                        if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }
                appBarScrollBehavior(
                    canScroll = {
                        !inSearchScreen &&
                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                    },
                )

                val topAppBarScrollBehavior =
                    appBarScrollBehavior(
                        canScroll = {
                            !inSearchScreen &&
                                (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        },
                    )

                // Navigation tracking
                LaunchedEffect(navBackStackEntry) {
                    if (inSearchScreen) {
                        val searchQuery =
                            SearchRoutes.decodeQuery(
                                navBackStackEntry?.arguments?.getString("query").orEmpty(),
                            )
                        onQueryChange(
                            TextFieldValue(
                                searchQuery,
                                TextRange(searchQuery.length),
                            ),
                        )
                    } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        onQueryChange(TextFieldValue())
                    }

                    // Reset scroll behavior for main navigation items
                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        if (navigationItems.fastAny { it.route == previousTab }) {
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    topAppBarScrollBehavior.state.resetHeightOffset()

                    // Collapse player when navigating to equalizer
                    if (navBackStackEntry?.destination?.route == "equalizer" &&
                        playerBottomSheetState.isExpanded
                    ) {
                        playerBottomSheetState.collapseSoft()
                    }

                    // Track previous tab for animations
                    navController.currentBackStackEntry?.destination?.route?.let {
                        setPreviousTab(it)
                    }
                }

                LaunchedEffect(activePlayerConnection) {
                    val player = runCatching { activePlayerConnection?.player }.getOrNull()
                    if (player?.currentMediaItem == null) {
                        if (!playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.dismiss()
                        }
                        return@LaunchedEffect
                    }

                    if (playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.collapseSoft()
                    }
                }

                DisposableEffect(activePlayerConnection, playerBottomSheetState) {
                    val player = runCatching { activePlayerConnection?.player }.getOrNull()
                        ?: return@DisposableEffect onDispose { }
                    val listener =
                        object : Player.Listener {
                            override fun onMediaItemTransition(
                                mediaItem: MediaItem?,
                                reason: Int,
                            ) {
                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                    mediaItem != null &&
                                    playerBottomSheetState.isDismissed
                                ) {
                                    playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                    player.addListener(listener)
                    onDispose {
                        player.removeListener(listener)
                    }
                }

                var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(navBackStackEntry, listenTogetherInTopBar) {
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isListenTogetherScreen =
                        currentRoute == Screens.ListenTogether.route ||
                            currentRoute == "listen_together_from_topbar"
                    shouldShowTopBar = currentRoute in topLevelScreens &&
                        currentRoute != "settings" &&
                        !(isListenTogetherScreen && listenTogetherInTopBar)
                }

                val coroutineScope = rememberCoroutineScope()
                var sharedSong: SongItem? by remember {
                    mutableStateOf(null)
                }
                val snackbarHostState = remember { SnackbarHostState() }

                // Offline notice. Shown once per launch when there is no network, with the
                // connection state coming straight from the player service, and removed
                // again the moment the device is back online.
                val connectivityObserver =
                    remember(playerConnection) { playerConnection?.service?.connectivityObserver }
                val isOnline by
                    (connectivityObserver?.networkStatus ?: flowOf(true))
                        .collectAsState(initial = true)

                var offlineNoticeShown by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(isOnline) {
                    if (!isOnline && !offlineNoticeShown) {
                        offlineNoticeShown = true
                        snackbarHostState.showSnackbar(
                            message = getString(R.string.offline_notice_message),
                            actionLabel = getString(R.string.close),
                            withDismissAction = true,
                            duration = SnackbarDuration.Indefinite,
                        )
                    } else if (isOnline && offlineNoticeShown) {
                        offlineNoticeShown = false
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                }

                // In-app updater. Downloading 9 MB silently and then throwing the user at a
                // system setting is why the update felt broken; every step now has a dialog.
                var updateFlow by remember { mutableStateOf<UpdateFlowState?>(null) }
                var pendingUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }
                var pendingApkFile by remember { mutableStateOf<File?>(null) }
                var awaitingInstallPermission by remember { mutableStateOf(false) }

                fun downloadAndInstall(update: AvailableUpdate) {
                    val asset = Updater.getAssetForCurrentVariant(update.release)
                    if (asset == null) {
                        updateFlow = UpdateFlowState.Failed(null)
                        return
                    }
                    pendingApkFile = null
                    updateFlow = UpdateFlowState.Downloading(0f)
                    lifecycleScope.launch {
                        AppUpdateDownloader.downloadApk(
                            context = this@MainActivity,
                            asset = asset,
                            sha256SumsUrl = update.release.sha256SumsUrl,
                            onProgress = { state ->
                                when (state) {
                                    is UpdateDownloadState.Downloading ->
                                        updateFlow =
                                            UpdateFlowState.Downloading(state.progress.coerceIn(0f, 1f))
                                    UpdateDownloadState.VerifyingChecksum ->
                                        updateFlow = UpdateFlowState.Verifying
                                    is UpdateDownloadState.ReadyToInstall -> {
                                        // Hand over to the system installer; its own sheet
                                        // takes over from here, so the dialog can go.
                                        updateFlow = null
                                        runOnUiThread {
                                            AppUpdateDownloader
                                                .triggerInstall(this@MainActivity, state.apkFile)
                                                .onFailure { updateFlow = UpdateFlowState.Failed(it.message) }
                                        }
                                    }
                                    is UpdateDownloadState.PermissionRequired -> {
                                        pendingApkFile = state.apkFile
                                        updateFlow = UpdateFlowState.PermissionRequired
                                    }
                                    is UpdateDownloadState.Failed ->
                                        updateFlow = UpdateFlowState.Failed(state.error)
                                    UpdateDownloadState.Idle -> Unit
                                }
                            },
                        )
                    }
                }

                // Ask for the install permission *before* downloading, so the user is not
                // left with a finished file and no way to open it.
                fun beginUpdate(update: AvailableUpdate) {
                    pendingUpdate = update
                    if (AppUpdateDownloader.canInstallPackages(this@MainActivity)) {
                        downloadAndInstall(update)
                    } else {
                        updateFlow = UpdateFlowState.PermissionRequired
                    }
                }

                val mainLifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(mainLifecycleOwner, awaitingInstallPermission) {
                    val observer =
                        LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME && awaitingInstallPermission) {
                                awaitingInstallPermission = false
                                if (AppUpdateDownloader.canInstallPackages(this@MainActivity)) {
                                    val apk = pendingApkFile
                                    if (apk != null) {
                                        updateFlow = null
                                        AppUpdateDownloader
                                            .triggerInstall(this@MainActivity, apk)
                                            .onFailure { updateFlow = UpdateFlowState.Failed(it.message) }
                                    } else {
                                        pendingUpdate?.let { downloadAndInstall(it) }
                                    }
                                }
                            }
                        }
                    mainLifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { mainLifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(Unit) {
                    if (pendingIntent != null) {
                        handleWidgetTargetIntent(pendingIntent!!, navController)
                        handleRecognitionIntent(pendingIntent!!, navController)
                        handleDeepLinkIntent(pendingIntent!!, navController)
                        pendingIntent = null
                    } else {
                        handleWidgetTargetIntent(intent, navController)
                        handleRecognitionIntent(intent, navController)
                        handleDeepLinkIntent(intent, navController)
                    }
                }

                DisposableEffect(Unit) {
                    val listener =
                        Consumer<Intent> { intent ->
                            handleWidgetTargetIntent(intent, navController)
                            handleRecognitionIntent(intent, navController)
                            handleDeepLinkIntent(intent, navController)
                        }

                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                val currentTitleRes =
                    remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Home.route -> R.string.home
                            Screens.Search.route -> R.string.search
                            Screens.Library.route -> R.string.filter_library
                            Screens.ListenTogether.route -> R.string.together
                            else -> null
                        }
                    }

                var showAccountDialog by remember { mutableStateOf(false) }

                val pauseListenHistory by rememberPreference(PauseListenHistoryKey, defaultValue = false)
                val eventCount by database.eventCount().collectAsStateWithLifecycle(initialValue = 0)
                val showHistoryButton =
                    remember(pauseListenHistory, eventCount) {
                        !(pauseListenHistory && eventCount == 0)
                    }

                val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
                val artistNameAliases by ArtistNameAliases.aliases.collectAsStateWithLifecycle()
                val enableLiquidGlass by rememberPreference(EnableLiquidGlassKey, defaultValue = true)
                val appBackdropState = rememberRegaBackdropState(enabled = enableLiquidGlass)

                CompositionLocalProvider(
                    LocalDatabase provides database,
                    LocalNavController provides navController,
                    LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                    LocalPlayerConnection provides playerConnection,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalDownloadUtil provides downloadUtil,
                    LocalSyncUtils provides syncUtils,
                    LocalListenTogetherManager provides listenTogetherManager,
                    LocalChangelogState provides showChangelog,
                    LocalArtistNameAliases provides artistNameAliases,
                    LocalLiquidGlassEnabled provides enableLiquidGlass,
                    LocalRegaBackdrop provides appBackdropState,
                ) {
                    if (showChangelog.value) {
                        ChangelogScreen(onDismiss = { showChangelog.value = false })
                    }

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            AnimatedVisibility(
                                visible = shouldShowTopBar,
                                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                            ) {
                                val useTopBarLiquidGlass = enableLiquidGlass && currentRoute != Screens.Home.route
                                Row {
                                    TopAppBar(
                                        title = {
                                            if (currentRoute == Screens.Home.route) {
                                                Image(
                                                    painter = painterResource(R.drawable.sonara_logo_horizontal),
                                                    contentDescription = null,
                                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .height(22.dp)
                                                        .widthIn(max = 124.dp),
                                                )
                                            } else {
                                                Text(
                                                    text = currentTitleRes?.let { stringResource(it) } ?: "",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        },
                                        actions = {
                                            if (!isOnline) {
                                                Surface(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                message = getString(R.string.offline_notice_message),
                                                                actionLabel = getString(R.string.close),
                                                                withDismissAction = true,
                                                                duration = SnackbarDuration.Indefinite,
                                                            )
                                                        }
                                                    },
                                                    shape = CircleShape,
                                                    color = if (useTopBarLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier =
                                                        Modifier
                                                            .padding(end = 8.dp)
                                                            .height(32.dp)
                                                            .then(
                                                                if (useTopBarLiquidGlass) {
                                                                    Modifier.regaGlassSpecular(
                                                                        shape = CircleShape,
                                                                        tintColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                                        borderColor = LocalRegaColors.current.glassBorder,
                                                                    )
                                                                } else {
                                                                    Modifier
                                                                },
                                                            ),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = stringResource(R.string.offline_badge),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 12.dp),
                                                        )
                                                    }
                                                }
                                            }
                                            if (currentRoute == Screens.Home.route && availableUpdate != null) {
                                                Surface(
                                                    onClick = { availableUpdate?.let { beginUpdate(it) } },
                                                    shape = CircleShape,
                                                    color = if (useTopBarLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                                    contentColor = MaterialTheme.colorScheme.primary,
                                                    modifier =
                                                        Modifier
                                                            .padding(end = 12.dp)
                                                            .height(32.dp)
                                                            .then(
                                                                if (useTopBarLiquidGlass) {
                                                                    Modifier.regaGlassSpecular(
                                                                        shape = CircleShape,
                                                                        tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                                                    )
                                                                } else {
                                                                    Modifier
                                                                },
                                                            ),
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 12.dp),
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.update),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp),
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = stringResource(R.string.new_update_badge),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                    }
                                                }
                                            }
                                            if (showHistoryButton && currentRoute != Screens.Home.route) {
                                                IconButton(onClick = { navController.navigate("history") }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.history),
                                                        contentDescription = stringResource(R.string.history),
                                                    )
                                                }
                                            }
                                            if (currentRoute != Screens.Home.route) IconButton(onClick = { navController.navigate("stats") }) {
                                                Icon(
                                                    painter = painterResource(R.drawable.stats),
                                                    contentDescription = stringResource(R.string.stats),
                                                )
                                            }
                                            if (listenTogetherInTopBar && currentRoute != Screens.Home.route) {
                                                IconButton(onClick = { navController.navigate("listen_together_from_topbar") }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.group_outlined),
                                                        contentDescription = stringResource(R.string.together),
                                                    )
                                                }
                                            }
                                        },
                                        scrollBehavior = topAppBarScrollBehavior,
                                        colors =
                                            TopAppBarDefaults.topAppBarColors(
                                                containerColor = when {
                                                    useTopBarLiquidGlass -> Color.Transparent
                                                    pureBlack -> Color.Black
                                                    else -> MaterialTheme.colorScheme.background
                                                },
                                                scrolledContainerColor = when {
                                                    useTopBarLiquidGlass -> Color.Transparent
                                                    pureBlack -> Color.Black
                                                    else -> MaterialTheme.colorScheme.background
                                                },
                                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                            ),
                                        modifier =
                                            Modifier
                                                .then(
                                                    if (useTopBarLiquidGlass) {
                                                        Modifier.regaGlassSpecular(
                                                            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                                                            tintColor = baseBg.copy(alpha = 0.14f),
                                                            borderColor = LocalRegaColors.current.glassBorder,
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                                )
                                                .windowInsetsPadding(
                                                    if (showRail) {
                                                        WindowInsets(left = NavigationBarHeight)
                                                            .add(cutoutInsets.only(WindowInsetsSides.Start))
                                                    } else {
                                                        cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                                                    },
                                                ),
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            val currentBackStackEntry = navController.currentBackStackEntry // reads reactively outside remember

                            val onNavItemClick: (Screens, Boolean) -> Unit =
                                remember(
                                    navController,
                                    coroutineScope,
                                    topAppBarScrollBehavior,
                                    playerBottomSheetState,
                                    currentBackStackEntry,
                                ) {
                                    { screen: Screens, isSelected: Boolean ->
                                        if (playerBottomSheetState.isExpanded) {
                                            playerBottomSheetState.collapseSoft()
                                        }
                                        if (isSelected) {
                                            val targetEntry =
                                                try {
                                                    val route = navController.currentBackStackEntry?.destination?.route
                                                    if (route == SearchRoutes.ROUTE || route == "search_input") {
                                                        // For search screens, use search_input entry
                                                        navController.getBackStackEntry("search_input")
                                                    } else {
                                                        // For other screens, use current entry
                                                        navController.currentBackStackEntry
                                                    }
                                                } catch (e: Exception) {
                                                    null
                                                }

                                            // Use appropriate key based on screen type
                                            if (screen == Screens.Search) {
                                                val current = targetEntry?.savedStateHandle?.get<Int>("scrollToTopCount") ?: 0
                                                targetEntry?.savedStateHandle?.set("scrollToTopCount", current + 1)
                                            } else {
                                                targetEntry?.savedStateHandle?.set("scrollToTop", true)
                                            }

                                            coroutineScope.launch {
                                                topAppBarScrollBehavior.state.resetHeightOffset()
                                            }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }

                            val onSearchLongClick: () -> Unit =
                                remember(navController) {
                                    {
                                        navController.navigate("recognition") {
                                            launchSingleTop = true
                                        }
                                    }
                                }

                            // Pre-calculate values for graphicsLayer to avoid reading state during composition
                            val navBarTotalHeight = bottomInset + NavigationBarHeight

                            if (!showRail && currentRoute != "wrapped") {
                                Box {
                                    if (activePlayerConnection != null) {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack,
                                        )
                                    }

                                    AppNavigationBar(
                                        navigationItems = navigationItems,
                                        currentRoute = currentRoute,
                                        onItemClick = onNavItemClick,
                                        pureBlack = pureBlack,
                                        slimNav = slimNav,
                                        onSearchLongClick = onSearchLongClick,
                                        onHomeLongHold = { showAccountDialog = true },
                                        modifier =
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .height(bottomInset + navPadding)
                                                // Use graphicsLayer instead of offset to avoid recomposition
                                                // graphicsLayer runs during draw phase, not composition phase
                                                .graphicsLayer {
                                                    val navBarHeightPx = navigationBarHeight.toPx()
                                                    val totalHeightPx = navBarTotalHeight.toPx()

                                                    translationY =
                                                        if (navBarHeightPx == 0f) {
                                                            totalHeightPx
                                                        } else {
                                                            // Read progress only during draw phase
                                                            val progress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                            val slideOffset = totalHeightPx * progress
                                                            val hideOffset =
                                                                totalHeightPx * (1 - navBarHeightPx / NavigationBarHeight.toPx())
                                                            slideOffset + hideOffset
                                                        }
                                                },
                                    )

                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .height(bottomInsetDp)
                                                // Use graphicsLayer for background color changes
                                                .graphicsLayer {
                                                    val progress = playerBottomSheetState.progress
                                                    alpha =
                                                        if (enableLiquidGlass ||
                                                            progress > 0f ||
                                                            (useNewMiniPlayerDesign && !shouldShowNavigationBar)
                                                        ) {
                                                            0f
                                                        } else {
                                                            1f
                                                        }
                                                }.background(baseBg),
                                    )
                                }
                            } else {
                                if (currentRoute != "wrapped") {
                                    if (activePlayerConnection != null) {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack,
                                        )
                                    }
                                }

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(bottomInsetDp)
                                            .graphicsLayer {
                                                val progress = playerBottomSheetState.progress
                                                alpha =
                                                    if (enableLiquidGlass || progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) 0f else 1f
                                            }.background(baseBg),
                                )
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                    ) {
                        Row(
                            Modifier
                                .fillMaxSize()
                                .regaBackdropSource(appBackdropState)
                                .background(baseBg),
                        ) {
                            CompositionLocalProvider(LocalRegaBackdrop provides null) {
                            val onRailItemClick: (Screens, Boolean) -> Unit =
                                remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
                                    { screen: Screens, isSelected: Boolean ->
                                        if (playerBottomSheetState.isExpanded) {
                                            playerBottomSheetState.collapseSoft()
                                        }

                                        if (isSelected) {
                                            navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                            coroutineScope.launch {
                                                topAppBarScrollBehavior.state.resetHeightOffset()
                                            }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }

                            val onRailSearchLongClick: () -> Unit =
                                remember(navController) {
                                    {
                                        navController.navigate("recognition") {
                                            launchSingleTop = true
                                        }
                                    }
                                }

                            if (showRail && currentRoute != "wrapped") {
                                AppNavigationRail(
                                    navigationItems = navigationItems,
                                    currentRoute = currentRoute,
                                    onItemClick = onRailItemClick,
                                    pureBlack = pureBlack,
                                    onSearchLongClick = onRailSearchLongClick,
                                    onHomeLongHold = { showAccountDialog = true },
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                // NavHost with animations (Material 3 Expressive style)
                                NavHost(
                                    navController = navController,
                                    startDestination =
                                        when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                            NavigationTab.HOME -> Screens.Home
                                            NavigationTab.SEARCH -> Screens.Search
                                            NavigationTab.LIBRARY -> Screens.Library
                                        }.route,
                                    enterTransition = {
                                        val currentRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
                                        val previousRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
                                        val forward = currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex

                                        // MONOCHROME SONIC: a shallow parallax slide under a soft fade.
                                        // The borrowed distance stays small so the transition reads as
                                        // depth rather than a page flip, and it never leaves the frame budget.
                                        fadeIn(tween(SonaraMotion.Medium, easing = SonaraMotion.Standard)) +
                                            slideInHorizontally(tween(SonaraMotion.Slow, easing = SonaraMotion.EmphasizedDecel)) {
                                                if (forward) it / 18 else -it / 18
                                            }
                                    },
                                    exitTransition = {
                                        val currentRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
                                        val targetRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
                                        val forward = targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex

                                        fadeOut(tween(SonaraMotion.Fast, easing = SonaraMotion.EmphasizedAccel)) +
                                            slideOutHorizontally(tween(SonaraMotion.Medium, easing = SonaraMotion.EmphasizedAccel)) {
                                                if (forward) -it / 24 else it / 24
                                            }
                                    },
                                    popEnterTransition = {
                                        val currentRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
                                        val previousRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
                                        val backwards = previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex

                                        fadeIn(tween(SonaraMotion.Medium, easing = SonaraMotion.Standard)) +
                                            slideInHorizontally(tween(SonaraMotion.Slow, easing = SonaraMotion.EmphasizedDecel)) {
                                                if (backwards) it / 18 else -it / 18
                                            }
                                    },
                                    popExitTransition = {
                                        val currentRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
                                        val targetRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
                                        val backwards = currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex

                                        fadeOut(tween(SonaraMotion.Fast, easing = SonaraMotion.EmphasizedAccel)) +
                                            slideOutHorizontally(tween(SonaraMotion.Medium, easing = SonaraMotion.EmphasizedAccel)) {
                                                if (backwards) it / 24 else -it / 24
                                            }
                                    },
                                    modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                                ) {
                                    navigationBuilder(
                                        navController = navController,
                                        scrollBehavior = topAppBarScrollBehavior,
                                        latestVersionName = latestVersionName,
                                        activity = this@MainActivity,
                                        snackbarHostState = snackbarHostState,
                                    )
                                }
                            }
                            }
                        }
                    }

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    BottomSheetPage(
                        state = LocalBottomSheetPageState.current,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    if (showAccountDialog) {
                        AccountSettingsDialog(
                            onDismiss = {
                                showAccountDialog = false
                                homeViewModel.refresh()
                            },
                            latestVersionName = latestVersionName,
                        )
                    }

                    sharedSong?.let { song ->
                        playerConnection?.let {
                            Dialog(
                                onDismissRequest = { sharedSong = null },
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                Surface(
                                    modifier = Modifier.padding(24.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = AlertDialogDefaults.containerColor,
                                    tonalElevation = AlertDialogDefaults.TonalElevation,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        YouTubeSongMenu(
                                            song = song,
                                            onDismiss = { sharedSong = null },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showUpdateDialog && !showChangelog.value && onboardingResolved && !launchGateVisible) {
                        availableUpdate?.let { update ->
                            val dismissUpdate: () -> Unit = {
                                showUpdateDialog = false
                                lifecycleScope.launch {
                                    safeDataStoreEdit {
                                        it[UpdatePromptDateKey] = LocalDate.now().toEpochDay()
                                        it[update.dismissalKey] = update.release.tagName
                                    }
                                }
                            }
                            AlertDialog(
                                onDismissRequest = dismissUpdate,
                                title = { Text(stringResource(R.string.update_available_title)) },
                                text = {
                                    Column(
                                        modifier =
                                            Modifier
                                                .heightIn(max = 480.dp)
                                                .verticalScroll(rememberScrollState()),
                                    ) {
                                        Text(
                                            text =
                                                stringResource(
                                                    R.string.update_available_message,
                                                    update.release.versionName,
                                                ),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            text = stringResource(R.string.changelog),
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                        )
                                        Text(
                                            text = update.release.description.ifBlank { stringResource(R.string.changelog_empty) },
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            dismissUpdate()
                                            beginUpdate(update)
                                        },
                                    ) {
                                        Text(stringResource(R.string.update_now))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = dismissUpdate) {
                                        Text(stringResource(R.string.update_later))
                                    }
                                },
                            )
                        }
                    }

                    updateFlow?.let { flow ->
                        val version = pendingUpdate?.release?.versionName ?: ""
                        when (flow) {
                            UpdateFlowState.PermissionRequired ->
                                AlertDialog(
                                    onDismissRequest = { updateFlow = null },
                                    title = { Text(stringResource(R.string.update_permission_title)) },
                                    text = { Text(stringResource(R.string.update_permission_message)) },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                awaitingInstallPermission = true
                                                AppUpdateDownloader.openInstallPermissionSettings(this@MainActivity)
                                            },
                                        ) {
                                            Text(stringResource(R.string.update_permission_open_settings))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { updateFlow = null }) {
                                            Text(stringResource(R.string.update_permission_later))
                                        }
                                    },
                                )

                            is UpdateFlowState.Downloading ->
                                UpdateProgressDialog(
                                    title = stringResource(R.string.update_progress_title, version),
                                    status =
                                        if (flow.progress > 0f) {
                                            stringResource(
                                                R.string.update_downloading_progress,
                                                (flow.progress * 100).toInt(),
                                            )
                                        } else {
                                            stringResource(R.string.update_downloading_starting)
                                        },
                                    progress = flow.progress.takeIf { it > 0f },
                                    hint = stringResource(R.string.update_install_hint),
                                    onHide = { updateFlow = null },
                                )

                            UpdateFlowState.Verifying ->
                                UpdateProgressDialog(
                                    title = stringResource(R.string.update_progress_title, version),
                                    status = stringResource(R.string.update_verifying_download),
                                    progress = null,
                                    hint = stringResource(R.string.update_install_hint),
                                    onHide = { updateFlow = null },
                                )

                            is UpdateFlowState.Failed ->
                                AlertDialog(
                                    onDismissRequest = { updateFlow = null },
                                    title = { Text(stringResource(R.string.update_available_title)) },
                                    text = {
                                        Text(
                                            text =
                                                buildString {
                                                    append(stringResource(R.string.update_download_failed))
                                                    flow.detail?.takeIf { it.isNotBlank() }?.let {
                                                        append("\n\n")
                                                        append(it)
                                                    }
                                                },
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                val retry = pendingUpdate
                                                updateFlow = null
                                                retry?.let { beginUpdate(it) }
                                            },
                                        ) {
                                            Text(stringResource(R.string.retry))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { updateFlow = null }) {
                                            Text(stringResource(R.string.close))
                                        }
                                    },
                                )
                        }
                    }
                }
                if (!onboardingResolved) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF050505)),
                    )
                }

                AnimatedVisibility(
                    visible = onboardingResolved && launchGateVisible,
                    // The gate must be opaque on its very first frame: a fade-in
                    // let Home show through for a beat before the intro started.
                    enter = EnterTransition.None,
                    exit = fadeOut(tween(320)),
                ) {
                    SonaraOnboarding(
                        navController = navController,
                        needsWelcome = onboardingPending,
                        startAtWelcome = introPlayedThisProcess,
                        onIntroFinished = {
                            introPlayedThisProcess = true
                            if (!onboardingPending) launchGateVisible = false
                        },
                        onGuest = {
                            lifecycleScope.launch {
                                safeDataStoreEdit {
                                    it[OnboardingStateKey] = OnboardingStatus.GUEST.value
                                    it[IntroCompletedKey] = true
                                }
                                launchGateVisible = false
                            }
                        },
                    )
                }
            }
            }
        }
    }

    /**
     * Handles the ACTION_RECOGNITION intent sent from the Music Recognizer Widget.
     * Always navigates to the recognition screen to show the result.
     */
    private fun handleRecognitionIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        if (intent.action != ACTION_RECOGNITION) return
        val autoStart = intent.getBooleanExtra(EXTRA_AUTO_START_RECOGNITION, false)
        intent.action = null
        intent.removeExtra(EXTRA_AUTO_START_RECOGNITION)
        navController.navigate(if (autoStart) "recognition?autoStart=true" else "recognition") {
            launchSingleTop = true
        }
    }

    private sealed class WidgetTargetRoute(val route: String) {
        data class LocalPlaylist(val id: String) : WidgetTargetRoute("local_playlist/$id")
        data class OnlinePlaylist(val id: String) : WidgetTargetRoute("online_playlist/$id")
        data object LikedSongs : WidgetTargetRoute("auto_playlist/liked")
        data object DownloadedSongs : WidgetTargetRoute("auto_playlist/downloaded")
        data class TopSongs(val limit: String) : WidgetTargetRoute("top_playlist/$limit")
    }

    private fun handleWidgetTargetIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        if (intent.action != ACTION_OPEN_WIDGET_TARGET) return

        val targetType = intent.getStringExtra(EXTRA_WIDGET_TARGET_TYPE)
        val targetId = intent.getStringExtra(EXTRA_WIDGET_TARGET_ID)
        intent.action = null
        intent.removeExtra(EXTRA_WIDGET_TARGET_TYPE)
        intent.removeExtra(EXTRA_WIDGET_TARGET_ID)

        val normalizedTargetId = targetId?.takeIf { it.isNotBlank() }

        val targetRoute = when (targetType) {
            PlaylistWidgetReceiver.TARGET_TYPE_LOCAL ->
                normalizedTargetId?.let { WidgetTargetRoute.LocalPlaylist(it) }

            PlaylistWidgetReceiver.TARGET_TYPE_ONLINE ->
                normalizedTargetId?.let { WidgetTargetRoute.OnlinePlaylist(it) }

            PlaylistWidgetReceiver.TARGET_TYPE_LIKED ->
                WidgetTargetRoute.LikedSongs

            PlaylistWidgetReceiver.TARGET_TYPE_DOWNLOADED ->
                WidgetTargetRoute.DownloadedSongs

            PlaylistWidgetReceiver.TARGET_TYPE_TOP ->
                WidgetTargetRoute.TopSongs(normalizedTargetId ?: "50")

            else -> null
        } ?: return

        navController.navigate(targetRoute.route)
    }

    private fun handleDeepLinkIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        intent.data = null
        intent.removeExtra(Intent.EXTRA_TEXT)
        val coroutineScope = lifecycle.coroutineScope

        val listenCode =
            uri.getQueryParameter("code")
                ?: uri.getQueryParameter("room")
                ?: uri.pathSegments.getOrNull(1)
        val isListenLink = uri.pathSegments.firstOrNull() == "listen" || uri.host?.equals("listen", ignoreCase = true) == true
        if (!listenCode.isNullOrBlank() && isListenLink) {
            val username = dataStore.get(ListenTogetherUsernameKey, "").ifBlank { "Guest" }
            listenTogetherManager.joinRoom(listenCode, username)
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> {
                val playlistId = uri.getQueryParameter("list")
                if (playlistId.isNullOrBlank() || playlistId.equals("null", ignoreCase = true)) {
                    Toast.makeText(this, R.string.playlist_unavailable, Toast.LENGTH_SHORT).show()
                    return
                }

                if (playlistId.startsWith("OLAK5uy_")) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .albumSongs(playlistId)
                            .onSuccess { songs ->
                                songs.firstOrNull()?.album?.id?.let { browseId ->
                                    withContext(Dispatchers.Main) {
                                        navController.navigate("album/$browseId")
                                    }
                                }
                            }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> {
                uri.lastPathSegment?.let { browseId ->
                    navController.navigate("album/$browseId")
                }
            }

            "channel", "c" -> {
                uri.lastPathSegment?.let { artistId ->
                    navController.navigate("artist/$artistId")
                }
            }

            "search" -> {
                uri.getQueryParameter("q")?.let {
                    navController.navigate(SearchRoutes.resultRoute(it))
                }
            }

            else -> {
                val videoId =
                    when {
                        path == "watch" -> uri.getQueryParameter("v")
                        uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                        else -> null
                    }

                val playlistId = uri.getQueryParameter("list")

                if (videoId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .queue(listOf(videoId), playlistId)
                            .onSuccess { queue ->
                                withContext(Dispatchers.Main) {
                                    playerConnection?.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                            queue.firstOrNull()?.toMediaMetadata(),
                                        ),
                                    )
                                }
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else if (playlistId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .queue(null, playlistId)
                            .onSuccess { queue ->
                                val firstItem = queue.firstOrNull()
                                withContext(Dispatchers.Main) {
                                    playerConnection?.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = firstItem?.id, playlistId = playlistId),
                                            firstItem?.toMediaMetadata(),
                                        ),
                                    )
                                }
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalNavController = staticCompositionLocalOf<NavController> { error("No NavController provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalListenTogetherManager = staticCompositionLocalOf<com.sonara.music.listentogether.ListenTogetherManager?> { null }
val LocalChangelogState = staticCompositionLocalOf<MutableState<Boolean>> { error("No LocalChangelogState provided") }
val LocalArtistNameAliases = staticCompositionLocalOf<Map<String, String>> { emptyMap() }
val LocalIsPlayerExpanded = compositionLocalOf { false }
