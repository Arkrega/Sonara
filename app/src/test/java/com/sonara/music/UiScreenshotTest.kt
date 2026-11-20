/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonara.music.ui.component.AppNavigationBar
import com.sonara.music.ui.screens.MoodAndGenresTile
import com.sonara.music.ui.screens.ProfileIdentity
import com.sonara.music.ui.screens.ProfileRow
import com.sonara.music.ui.screens.ProfileSectionLabel
import com.sonara.music.ui.screens.ProfileShortcut
import com.sonara.music.ui.screens.Screens
import com.sonara.music.ui.screens.SonaraGuestNotice
import com.sonara.music.ui.screens.SonaraMasthead
import com.sonara.music.ui.theme.SonaraSpacing
import com.sonara.music.ui.theme.SonaraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the main SONARA surfaces on the JVM (no emulator, no device) and writes
 * one PNG per screen into `app/src/test/snapshots/images/`.
 *
 * Run with:
 *   ./gradlew :app:testFossDebugUnitTest --tests "*UiScreenshotTest*"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class UiScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val outputDir = File("src/test/snapshots/images")

    private fun render(
        name: String,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            SonaraTheme(darkTheme = true) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .testTag("snapshot-root"),
                ) {
                    content()
                }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag("snapshot-root").captureToImage().asAndroidBitmap()
        outputDir.mkdirs()
        val file = File(outputDir, "$name.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        println("SCREENSHOT ${file.absolutePath} ${file.length()} bytes")
    }

    @Test
    fun home() {
        render("screenshot_1_home") {
            PhoneFrame {
                Wordmark()
                SonaraMasthead(
                    accountName = "Rega",
                    onHistory = {},
                    onStats = {},
                    onTogether = {},
                )
                SonaraGuestNotice(onConnect = {})
                ScreenshotChips()
                ScreenshotSectionTitle("Mood & genres")
                Row(
                    modifier = Modifier.padding(horizontal = SonaraSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
                ) {
                    MoodAndGenresTile(
                        title = "Chill",
                        onClick = {},
                        accentColor = 0xFF3F51B5,
                        modifier = Modifier.width(150.dp).height(120.dp),
                    )
                    MoodAndGenresTile(
                        title = "Feel good",
                        onClick = {},
                        accentColor = 0xFF2E7D32,
                        modifier = Modifier.width(150.dp).height(120.dp),
                    )
                }
                ScreenshotSectionTitle("Your music")
                ShortcutGrid(
                    listOf(
                        "History" to R.drawable.history,
                        "Stats" to R.drawable.stats,
                        "Equalizer" to R.drawable.equalizer,
                        "Mood and Genres" to R.drawable.explore_outlined,
                    ),
                )
                ScreenshotSectionTitle("Recently played")
                Row(
                    modifier = Modifier.padding(horizontal = SonaraSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
                ) {
                    MoodAndGenresTile(
                        title = "Evening wind-down",
                        onClick = {},
                        accentColor = 0xFF4527A0,
                        modifier = Modifier.width(150.dp).height(120.dp),
                    )
                    MoodAndGenresTile(
                        title = "Fresh finds",
                        onClick = {},
                        accentColor = 0xFF00695C,
                        modifier = Modifier.width(150.dp).height(120.dp),
                    )
                }
            }
        }
    }

    @Test
    fun moodAndGenres() {
        val tiles =
            listOf(
                "Chill" to 0xFF3F51B5L,
                "Commute" to 0xFF8D6E63L,
                "Energize" to 0xFF827717L,
                "Feel good" to 0xFF2E7D32L,
                "Focus" to 0xFF546E7AL,
                "Gaming" to 0xFF1A237EL,
                "Party" to 0xFF6A1B9AL,
                "Romance" to 0xFFB71C1CL,
            )
        render("screenshot_2_mood_and_genres") {
            PhoneFrame {
                Wordmark()
                ScreenshotSectionTitle("Mood & genres")
                tiles.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.padding(horizontal = SonaraSpacing.md, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
                    ) {
                        row.forEach { (title, color) ->
                            MoodAndGenresTile(
                                title = title,
                                onClick = {},
                                accentColor = color,
                                modifier = Modifier.weight(1f).height(104.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun profile() {
        render("screenshot_3_profile") {
            PhoneFrame(route = Screens.Profile.route) {
                Wordmark()
                ProfileIdentity(
                    name = "Rega",
                    imageUrl = null,
                    isLoggedIn = true,
                    onConnect = {},
                )
                ProfileSectionLabel("Account")
                ProfileRow(
                    title = "Manage account",
                    subtitle = "Switch account, sign out or check update",
                    iconId = R.drawable.account,
                    onClick = {},
                )
                ProfileRow(
                    title = "Settings",
                    iconId = R.drawable.settings,
                    onClick = {},
                )
                ProfileSectionLabel("Your music")
                ShortcutGrid(
                    listOf(
                        "History" to R.drawable.history,
                        "Stats" to R.drawable.stats,
                        "Equalizer" to R.drawable.equalizer,
                        "Mood and Genres" to R.drawable.explore_outlined,
                    ),
                )
                ProfileSectionLabel("About")
                ProfileRow(
                    title = "Version",
                    subtitle = "${BuildConfig.BASE_VERSION_NAME} · SONARA",
                    iconId = R.drawable.info,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun welcome() {
        render("screenshot_4_welcome") {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = SonaraSpacing.xl, vertical = SonaraSpacing.xl),
            ) {
                Wordmark(horizontalPadding = 0.dp)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Welcome to SONARA",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(SonaraSpacing.sm))
                Text(
                    text = "Your music. Your experience. Your choice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(SonaraSpacing.xxl))
                ScreenshotButton("Continue with Google", "Connect your YouTube Music account", primary = true)
                Spacer(Modifier.height(SonaraSpacing.sm))
                ScreenshotButton("Continue as guest", "Keep everything on this device", primary = false)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "YOUR SOUND. YOUR WORLD",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 3.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    @Test
    fun library() {
        render("screenshot_5_library") {
            PhoneFrame(route = Screens.Library.route) {
                Wordmark()
                ScreenshotSectionTitle("Your collection")
                ShortcutGrid(
                    listOf(
                        "Songs" to R.drawable.music_note,
                        "Albums" to R.drawable.library_music_outlined,
                        "Artists" to R.drawable.person,
                        "Playlists" to R.drawable.queue_music,
                        "Downloads" to R.drawable.download,
                        "Liked" to R.drawable.favorite,
                    ),
                )
                ScreenshotSectionTitle("Recently played")
                Row(
                    modifier = Modifier.padding(horizontal = SonaraSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
                ) {
                    MoodAndGenresTile(
                        title = "Late night drive",
                        onClick = {},
                        accentColor = 0xFF283593,
                        modifier = Modifier.width(150.dp).height(120.dp),
                    )
                    MoodAndGenresTile(
                        title = "Morning coffee",
                        onClick = {},
                        accentColor = 0xFF6D4C41,
                        modifier = Modifier.width(150.dp).height(120.dp),
                    )
                }
            }
        }
    }

    /** Screen scaffold: scrollable content on top, the real dock pinned to the bottom. */
    @Composable
    private fun PhoneFrame(
        route: String = Screens.Home.route,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                content = content,
            )
            AppNavigationBar(
                navigationItems = listOf(Screens.Home, Screens.Search, Screens.Library, Screens.Profile),
                currentRoute = route,
                onItemClick = { _, _ -> },
            )
        }
    }

    @Composable
    private fun Wordmark(horizontalPadding: Dp = SonaraSpacing.gutter) {
        Image(
            painter = painterResource(R.drawable.sonara_logo_horizontal),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .padding(start = horizontalPadding, top = SonaraSpacing.xl, bottom = SonaraSpacing.sm)
                    .height(22.dp)
                    .widthIn(max = 124.dp),
        )
    }

    @Composable
    private fun ShortcutGrid(items: List<Pair<String, Int>>) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.padding(horizontal = SonaraSpacing.gutter, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
            ) {
                row.forEach { (title, icon) ->
                    ProfileShortcut(title, icon, {}, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun ScreenshotSectionTitle(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
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
    private fun ScreenshotChips() {
        Row(
            modifier = Modifier.padding(horizontal = SonaraSpacing.gutter, vertical = SonaraSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
        ) {
            listOf("FEEL GOOD", "ENERGIZE", "RELAX").forEach { chip ->
                Box(
                    modifier =
                        Modifier
                            .background(Color(0x1AFFFFFF), MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Composable
    private fun ScreenshotButton(
        title: String,
        subtitle: String,
        primary: Boolean,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .background(
                        if (primary) MaterialTheme.colorScheme.onBackground else Color(0xFF101113),
                        MaterialTheme.shapes.large,
                    )
                    .padding(horizontal = SonaraSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (primary) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (primary) {
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}
