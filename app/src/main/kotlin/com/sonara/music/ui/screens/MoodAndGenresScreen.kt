/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.screens

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.sonara.music.LocalPlayerAwareWindowInsets
import com.sonara.music.R
import com.sonara.music.ui.component.IconButton
import com.sonara.music.ui.component.NavigationTitle
import com.sonara.music.ui.component.shimmer.ListItemPlaceHolder
import com.sonara.music.ui.component.shimmer.ShimmerHost
import com.sonara.music.ui.theme.SonaraLabelStyle
import com.sonara.music.ui.theme.SonaraSpacing
import com.sonara.music.ui.theme.sonaraPanel
import com.sonara.music.ui.utils.backToMain
import com.sonara.music.viewmodels.MoodAndGenresViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Compact carousel tile used on Home. */
val MoodTileHeight = 132.dp

/** Wide tile used by the full Mood & genres grid. */
private val MoodGridTileAspect = 1.72f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val localConfiguration = LocalConfiguration.current
    val itemsPerRow = if (localConfiguration.orientation == ORIENTATION_LANDSCAPE) 3 else 2

    val moodAndGenresList by viewModel.moodAndGenres.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        if (moodAndGenresList == null) {
            item(key = "mood_and_genres_shimmer") {
                ShimmerHost(
                    modifier = Modifier.animateItem()
                ) {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }

        moodAndGenresList?.forEachIndexed { index, moodAndGenres ->
            item(key = "mood_and_genres_section_$index") {
                Column(
                    modifier =
                        Modifier
                            .animateItem()
                            .padding(bottom = SonaraSpacing.sm),
                ) {
                    NavigationTitle(
                        title = moodAndGenres.title,
                    )
                    moodAndGenres.items.chunked(itemsPerRow).forEach { row ->
                        Row(
                            modifier = Modifier.padding(horizontal = SonaraSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
                        ) {
                            row.forEach { item ->
                                MoodAndGenresTile(
                                    title = item.title,
                                    kicker = null,
                                    accentColor = item.stripeColor,
                                    onClick = {
                                        navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .padding(vertical = SonaraSpacing.xs)
                                            .aspectRatio(MoodGridTileAspect),
                                )
                            }

                            repeat(itemsPerRow - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.mood_and_genres)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

/**
 * Mood & genre card. Every destination gets a deterministic monochrome artwork
 * generated from its title, so the wall reads as designed visual language
 * instead of a grid of labelled boxes.
 */
@Composable
fun MoodAndGenresTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    accentColor: Long? = null,
) {
    val seed = remember(title) { abs(title.lowercase().hashCode()) }
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier =
            modifier
                .clip(shape)
                .sonaraPanel(shape = shape)
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        MoodArtwork(
            seed = seed,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = SonaraSpacing.md, vertical = SonaraSpacing.sm),
        ) {
            if (!kicker.isNullOrBlank()) {
                Text(
                    text = kicker.uppercase(),
                    style = SonaraLabelStyle.copy(fontSize = 8.sp, letterSpacing = 0.9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(SonaraSpacing.sm)
                    .size(14.dp),
        )
    }
}

/**
 * Four artwork families (rings, waveform, dot field, streaks) picked from the
 * seed, each tinted with the accent at a low alpha so the card stays legible.
 */
@Composable
private fun MoodArtwork(
    seed: Int,
    accentColor: Long?,
    modifier: Modifier = Modifier,
) {
    // YouTube ships a per-destination stripe colour. It is used only as a low
    // alpha wash so the wall gains identity without breaking the mono palette.
    val accent =
        if (accentColor != null && accentColor != 0L) {
            Color(accentColor.toInt() or 0xFF000000.toInt())
        } else {
            MaterialTheme.colorScheme.primary
        }
    val variant = seed % 4
    val glowX = 0.16f + ((seed / 7) % 60) / 100f
    val glowY = 0.12f + ((seed / 13) % 40) / 100f

    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF15171C),
                1f to Color(0xFF0D0E11),
            ),
            size = size,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * glowX, size.height * glowY),
                radius = size.maxDimension * 0.55f,
            ),
            radius = size.maxDimension * 0.55f,
            center = Offset(size.width * glowX, size.height * glowY),
        )

        val hairline = Color.White.copy(alpha = 0.10f)
        val hairlineStrong = Color.White.copy(alpha = 0.20f)

        when (variant) {
            0 -> {
                val center = Offset(size.width * 0.82f, size.height * 0.5f)
                val base = size.minDimension * 0.20f
                repeat(4) { ring ->
                    drawCircle(
                        color = if (ring == 0) hairlineStrong else hairline,
                        radius = base * (1f + ring * 0.62f),
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
                drawCircle(color = accent.copy(alpha = 0.5f), radius = 2.dp.toPx(), center = center)
            }

            1 -> {
                val bars = 26
                val left = size.width * 0.52f
                val band = size.width * 0.42f
                val stepX = band / (bars - 1)
                val mid = size.height * 0.46f
                val amplitude = size.height * 0.26f
                repeat(bars) { index ->
                    val envelope = sin(Math.PI * index / (bars - 1f)).toFloat()
                    val wobble = 0.35f + 0.65f * abs(sin((seed % 17 + index) * 0.6f))
                    val half = (amplitude * envelope * wobble).coerceAtLeast(1f)
                    drawLine(
                        color = if (index % 4 == 0) hairlineStrong else hairline,
                        start = Offset(left + index * stepX, mid - half),
                        end = Offset(left + index * stepX, mid + half),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            2 -> {
                val columns = 9
                val rows = 5
                val gapX = size.width / (columns + 1)
                val gapY = size.height / (rows + 1)
                for (column in 1..columns) {
                    for (row in 1..rows) {
                        val distance = abs(column - columns * 0.78f) + abs(row - rows * 0.5f)
                        val alpha = (0.26f - distance * 0.035f).coerceAtLeast(0.03f)
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = 1.4.dp.toPx(),
                            center = Offset(gapX * column, gapY * row),
                        )
                    }
                }
                drawCircle(
                    color = accent.copy(alpha = 0.45f),
                    radius = 3.dp.toPx(),
                    center = Offset(gapX * columns * 0.78f, gapY * rows * 0.5f),
                )
            }

            else -> {
                val lines = 7
                repeat(lines) { index ->
                    val startX = size.width * (0.42f + index * 0.09f)
                    val angle = cos((seed % 11 + index) * 0.35f) * size.height * 0.32f
                    drawLine(
                        color = if (index % 3 == 0) hairlineStrong else hairline,
                        start = Offset(startX, 0f),
                        end = Offset(startX + angle * 0.5f, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
        }
    }
}
