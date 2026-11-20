/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sonara.music.BuildConfig
import com.sonara.music.LocalPlayerAwareWindowInsets
import com.sonara.music.R
import com.sonara.music.constants.CheckForUpdatesKey
import com.sonara.music.constants.UpdateNotificationsEnabledKey
import com.sonara.music.ui.component.IconButton
import com.sonara.music.ui.component.Material3SettingsGroup
import com.sonara.music.ui.component.Material3SettingsItem
import com.sonara.music.ui.utils.backToMain
import android.content.Intent
import android.net.Uri
import com.sonara.music.utils.AppUpdateDownloader
import com.sonara.music.utils.UpdateDownloadState
import com.sonara.music.utils.Updater
import com.sonara.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterScreen(
    navController: NavController
) {
    val (checkForUpdates, onCheckForUpdatesChange) = rememberPreference(CheckForUpdatesKey, true)
    val (updateNotifications, onUpdateNotificationsChange) = rememberPreference(UpdateNotificationsEnabledKey, true)

    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var showChangelog by remember { mutableStateOf(false) }
    var changelogContent by remember { mutableStateOf<String?>(null) }
    var checkError by remember { mutableStateOf<String?>(null) }
        val failedToCheckUpdatesTemplate = stringResource(R.string.failed_to_check_updates)
    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val current = downloadState
                if (current is UpdateDownloadState.PermissionRequired) {
                    if (AppUpdateDownloader.canInstallPackages(context)) {
                        downloadState = UpdateDownloadState.ReadyToInstall(current.apkFile)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    fun performManualCheck() {
        coroutineScope.launch {
            isChecking = true
            checkError = null
            withContext(Dispatchers.IO) {
                Updater
                    .checkForUpdate(forceRefresh = true)
                    .onSuccess { (releaseInfo, hasUpdate) ->
                        if (releaseInfo != null) {
                            latestVersion = releaseInfo.versionName
                            updateAvailable = hasUpdate
                            changelogContent = releaseInfo.description
                        }
                    }.onFailure {
                        checkError = String.format(failedToCheckUpdatesTemplate, it.message ?: "Unknown error")
                    }
            }
            isChecking = false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top,
                ),
            ),
        )

        Spacer(Modifier.height(4.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.current_version),
            items =
                listOf(
                    Material3SettingsItem(
                        title = {
                            Text(stringResource(R.string.version_format, BuildConfig.VERSION_NAME))
                        },
                        description = {
                            val arch = BuildConfig.ARCHITECTURE
                            val variant = if (BuildConfig.CAST_AVAILABLE) "GMS" else "FOSS"
                            Text("$arch - $variant")
                        },
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.update_settings),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.check_for_updates)) },
                            icon = painterResource(R.drawable.update),
                            trailingContent = {
                                Switch(
                                    checked = checkForUpdates,
                                    onCheckedChange = onCheckForUpdatesChange,
                                )
                            },
                            onClick = { onCheckForUpdatesChange(!checkForUpdates) },
                        ),
                    )

                    if (checkForUpdates) {
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(R.string.update_notifications)) },
                                icon = painterResource(R.drawable.notification),
                                trailingContent = {
                                    Switch(
                                        checked = updateNotifications,
                                        onCheckedChange = onUpdateNotificationsChange,
                                    )
                                },
                                onClick = { onUpdateNotificationsChange(!updateNotifications) },
                            ),
                        )
                    }
                },
        )

        Spacer(Modifier.height(16.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.check_for_updates_title),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.refresh),
                        title = {
                            if (isChecking) {
                                Text(stringResource(R.string.checking_for_updates))
                            } else if (latestVersion != null) {
                                Text(stringResource(R.string.latest_version_format, latestVersion!!))
                            } else {
                                Text(stringResource(R.string.check_for_updates_button))
                            }
                        },
                        trailingContent = {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else if (updateAvailable) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = stringResource(R.string.update_available_title),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = { if (!isChecking) performManualCheck() },
                    ),
                ),
        )

        checkError?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (updateAvailable && latestVersion != null) {
            Spacer(Modifier.height(16.dp))

            when (val state = downloadState) {
                is UpdateDownloadState.Downloading -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        val percent = if (state.progress >= 0f) " ${(state.progress * 100).toInt()}%" else ""
                        Text("Downloading$percent…")
                    }
                }
                is UpdateDownloadState.VerifyingChecksum -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text("Verifying checksum…")
                    }
                }
                is UpdateDownloadState.PermissionRequired -> {
                    Text(
                        text = stringResource(R.string.install_permission_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    Button(
                        onClick = {
                            AppUpdateDownloader.openInstallPermissionSettings(context)
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                    ) {
                        Text(stringResource(R.string.grant_install_permission))
                    }
                }
                is UpdateDownloadState.ReadyToInstall -> {
                    Button(
                        onClick = {
                            if (!AppUpdateDownloader.canInstallPackages(context)) {
                                downloadState = UpdateDownloadState.PermissionRequired(state.apkFile)
                                AppUpdateDownloader.openInstallPermissionSettings(context)
                            } else {
                                AppUpdateDownloader.triggerInstall(context, state.apkFile)
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                    ) {
                        Text(stringResource(R.string.install_update))
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            val releaseInfo = Updater.getCachedLatestRelease()
                            val currentAsset = releaseInfo?.let { Updater.getAssetForCurrentVariant(it) }
                            val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }
                            if (currentAsset != null) {
                                coroutineScope.launch {
                                    AppUpdateDownloader.downloadApk(
                                        context = context,
                                        asset = currentAsset,
                                        sha256SumsUrl = releaseInfo.sha256SumsUrl,
                                        onProgress = { newState ->
                                            downloadState = newState
                                        },
                                    ).onFailure {
                                        downloadUrl?.let { url ->
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    }
                                }
                            } else if (downloadUrl != null) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                    ) {
                        Text(stringResource(R.string.update_action))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showChangelog = !showChangelog },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            ) {
                Text(if (showChangelog) stringResource(R.string.hide_changelog) else stringResource(R.string.view_changelog))
            }

            if (showChangelog && changelogContent != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = changelogContent!!,
                    style = MaterialTheme.typography.bodySmall,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.updater)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
