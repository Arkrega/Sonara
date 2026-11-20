/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.sonara.innertube.utils.parseCookieString
import com.sonara.music.BuildConfig
import com.sonara.music.LocalPlayerAwareWindowInsets
import com.sonara.music.R
import com.sonara.music.constants.InnerTubeCookieKey
import com.sonara.music.ui.component.AccountSettingsDialog
import com.sonara.music.ui.theme.SonaraHairline
import com.sonara.music.ui.theme.SonaraLabelStyle
import com.sonara.music.ui.theme.SonaraLink
import com.sonara.music.ui.theme.SonaraSpacing
import com.sonara.music.ui.theme.sonaraPanel
import com.sonara.music.utils.rememberPreference
import com.sonara.music.viewmodels.HomeViewModel

/**
 * Profile dock destination. Everything that used to sit as anonymous icons in
 * the top bar now lives here, so the header can stay a pure wordmark.
 */
@Composable
fun ProfileScreen(
    navController: NavController,
    latestVersionName: String,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    var showAccountDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        item(key = "profile_header") {
            ProfileIdentity(
                name = accountName,
                imageUrl = accountImageUrl.takeIf { isLoggedIn },
                isLoggedIn = isLoggedIn,
                onConnect = { navController.navigate("login") { launchSingleTop = true } },
            )
        }

        item(key = "profile_account_label") {
            ProfileSectionLabel(stringResource(R.string.sonara_profile_section_account))
        }
        item(key = "profile_manage_account") {
            ProfileRow(
                title = stringResource(R.string.sonara_profile_manage_account),
                subtitle = if (isLoggedIn) {
                    stringResource(R.string.sonara_profile_manage_account_signed_in)
                } else {
                    stringResource(R.string.sonara_profile_manage_account_guest)
                },
                iconId = R.drawable.account,
                onClick = { showAccountDialog = true },
            )
        }
        item(key = "profile_settings") {
            ProfileRow(
                title = stringResource(R.string.settings),
                iconId = R.drawable.settings,
                onClick = { navController.navigate("settings") },
            )
        }

        item(key = "profile_shortcuts_label") {
            ProfileSectionLabel(stringResource(R.string.sonara_profile_section_music))
        }
        item(key = "profile_shortcuts") {
            Column(modifier = Modifier.padding(horizontal = SonaraSpacing.gutter)) {
                Row(horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.sm)) {
                    ProfileShortcut(
                        title = stringResource(R.string.history),
                        iconId = R.drawable.history,
                        onClick = { navController.navigate("history") },
                        modifier = Modifier.weight(1f),
                    )
                    ProfileShortcut(
                        title = stringResource(R.string.stats),
                        iconId = R.drawable.stats,
                        onClick = { navController.navigate("stats") },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(SonaraSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.sm)) {
                    ProfileShortcut(
                        title = stringResource(R.string.equalizer),
                        iconId = R.drawable.equalizer,
                        onClick = { navController.navigate("equalizer") },
                        modifier = Modifier.weight(1f),
                    )
                    ProfileShortcut(
                        title = stringResource(R.string.mood_and_genres),
                        iconId = R.drawable.explore_outlined,
                        onClick = { navController.navigate("mood_and_genres") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "profile_about_label") {
            ProfileSectionLabel(stringResource(R.string.sonara_profile_section_about))
        }
        item(key = "profile_version") {
            val updateAvailable = latestVersionName != BuildConfig.BASE_VERSION_NAME
            ProfileRow(
                title = stringResource(R.string.sonara_profile_version),
                subtitle = "${BuildConfig.VERSION_NAME} · ${stringResource(R.string.app_name)}",
                iconId = R.drawable.info,
                trailing = {
                    if (updateAvailable) {
                        SonaraLink(
                            text = stringResource(R.string.sonara_profile_update),
                            onClick = { navController.navigate("settings/updater") },
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.sonara_profile_up_to_date),
                            style = SonaraLabelStyle.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = { navController.navigate("settings/about") },
            )
        }
        item(key = "profile_footer") {
            Text(
                text = stringResource(R.string.sonara_intro_label),
                style = SonaraLabelStyle.copy(fontSize = 9.sp, letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = SonaraSpacing.xl, bottom = SonaraSpacing.xxl),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (showAccountDialog) {
        AccountSettingsDialog(
            onDismiss = { showAccountDialog = false },
            latestVersionName = latestVersionName,
        )
    }
}

@Composable
internal fun ProfileIdentity(
    name: String,
    imageUrl: String?,
    isLoggedIn: Boolean,
    onConnect: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = SonaraSpacing.gutter,
                    end = SonaraSpacing.gutter,
                    top = SonaraSpacing.xl,
                    bottom = SonaraSpacing.sm,
                ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.size(SonaraSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { stringResource(R.string.sonara_profile_guest) },
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.4).sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text =
                        if (isLoggedIn) {
                            stringResource(R.string.sonara_profile_signed_in)
                        } else {
                            stringResource(R.string.sonara_profile_guest)
                        },
                    style = SonaraLabelStyle.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!isLoggedIn) {
            Spacer(Modifier.height(SonaraSpacing.md))
            SonaraLink(
                text = stringResource(R.string.sonara_guest_notice_action),
                onClick = onConnect,
            )
        }

        Spacer(Modifier.height(SonaraSpacing.lg))
        SonaraHairline(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

@Composable
internal fun ProfileSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = SonaraLabelStyle.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier.padding(
                start = SonaraSpacing.gutter,
                end = SonaraSpacing.gutter,
                top = SonaraSpacing.lg,
                bottom = SonaraSpacing.sm,
            ),
    )
}

@Composable
internal fun ProfileRow(
    title: String,
    iconId: Int,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier =
            Modifier
                .padding(horizontal = SonaraSpacing.gutter, vertical = SonaraSpacing.xs)
                .fillMaxWidth()
                .clip(shape)
                .sonaraPanel(shape = shape)
                .clickable(onClick = onClick)
                .padding(horizontal = SonaraSpacing.md, vertical = SonaraSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(SonaraSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
internal fun ProfileShortcut(
    title: String,
    iconId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            modifier
                .clip(shape)
                .sonaraPanel(shape = shape)
                .clickable(onClick = onClick)
                .padding(horizontal = SonaraSpacing.md, vertical = SonaraSpacing.md),
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.height(SonaraSpacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
