/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.sonara.music.BuildConfig
import com.sonara.music.LocalPlayerAwareWindowInsets
import com.sonara.music.R
import com.sonara.music.ui.component.IconButton
import com.sonara.music.ui.component.Material3SettingsGroup
import com.sonara.music.ui.component.Material3SettingsItem
import com.sonara.music.ui.component.ReleaseNotesCard
import com.sonara.music.ui.theme.RegaColor
import com.sonara.music.ui.utils.backToMain
import com.sonara.music.utils.Updater
import com.sonara.music.ui.component.glass.regaTopAppBarGlass
import com.sonara.music.ui.component.glass.regaTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val showChangelog = com.sonara.music.LocalChangelogState.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hasAndroidAuto = remember {
        try {
            context.packageManager.getPackageInfo(
                "com.google.android.projection.gearhead", 0
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    Scaffold(
        containerColor = RegaColor.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        color = RegaColor.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            tint = RegaColor.TextPrimary
                        )
                    }
                },
                colors = regaTopAppBarColors(TopAppBarDefaults.topAppBarColors(
                    containerColor = RegaColor.Background,
                    titleContentColor = RegaColor.TextPrimary
                )),
                modifier = Modifier.regaTopAppBarGlass(),
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .background(RegaColor.Background)
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_playback),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.play),
                        title = { Text(stringResource(R.string.player_and_audio)) },
                        description = { Text("Crossfade, gapless, queue, skip silence, loudness") },
                        onClick = { navController.navigate("settings/player") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_audio_eq),
                items = buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.equalizer),
                            title = { Text(stringResource(R.string.equalizer_header)) },
                            description = { Text("10-band parametric EQ, presets, spatial reverb") },
                            onClick = { navController.navigate("equalizer") }
                        )
                    )
                    if (hasAndroidAuto) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.ic_android_auto),
                                title = { Text(stringResource(R.string.android_auto)) },
                                description = { Text("Car audio integration and playlist options") },
                                onClick = { navController.navigate("settings/android_auto") }
                            )
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_appearance),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.appearance)) },
                        description = { Text("Linear dark, Pure Black, indigo accents, layout density") },
                        onClick = { navController.navigate("settings/appearance") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_library),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.library_music),
                        title = { Text(stringResource(R.string.content)) },
                        description = { Text(stringResource(R.string.library_settings_desc)) },
                        onClick = { navController.navigate("settings/content") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.translate),
                        title = { Text(stringResource(R.string.ai_lyrics_translation)) },
                        description = { Text("AI-powered multilingual lyrics translation") },
                        onClick = { navController.navigate("settings/ai") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_downloads),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.download),
                        title = { Text(stringResource(R.string.storage)) },
                        description = { Text(stringResource(R.string.downloads_settings_desc)) },
                        onClick = { navController.navigate("settings/storage") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.restore),
                        title = { Text(stringResource(R.string.backup_restore)) },
                        description = { Text("Export and restore local playlists and preferences") },
                        onClick = { navController.navigate("settings/backup_restore") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_notifications),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.notification),
                        title = { Text(stringResource(R.string.privacy)) },
                        description = { Text(stringResource(R.string.notifications_settings_desc)) },
                        onClick = { navController.navigate("settings/privacy") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_updates),
                items = buildList {
                    if (BuildConfig.UPDATER_AVAILABLE) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.update),
                                title = { Text(stringResource(R.string.updater)) },
                                description = { Text("Software updates and release channels") },
                                onClick = { navController.navigate("settings/updater") }
                            )
                        )
                    }
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.newspaper),
                            title = { Text(stringResource(R.string.changelog)) },
                            description = { Text("What\'s new in SONARA") },
                            onClick = { showChangelog.value = true }
                        )
                    )
                    if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.BASE_VERSION_NAME) {
                        val releaseInfo = Updater.getCachedLatestRelease()
                        val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }

                        if (downloadUrl != null) {
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.update),
                                    title = {
                                        Text(text = stringResource(R.string.new_version_available))
                                    },
                                    description = {
                                        Text(
                                            text = latestVersionName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    showBadge = true,
                                    onClick = { uriHandler.openUri(downloadUrl) }
                                )
                            )
                        }
                    }
                }
            )

            if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.BASE_VERSION_NAME) {
                Spacer(modifier = Modifier.height(14.dp))
                ReleaseNotesCard()
            }

            Spacer(modifier = Modifier.height(14.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_about),
                items = buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.info),
                            title = { Text(stringResource(R.string.about)) },
                            description = { Text("SONARA v${BuildConfig.VERSION_NAME} • Open-source licenses") },
                            onClick = { navController.navigate("settings/about") }
                        )
                    )
                    if (isAndroid12OrLater) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.link),
                                title = { Text(stringResource(R.string.default_links)) },
                                description = { Text("Open YouTube Music links directly in SONARA") },
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                            "package:${context.packageName}".toUri()
                                        )
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            R.string.open_app_settings_error,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            )
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
