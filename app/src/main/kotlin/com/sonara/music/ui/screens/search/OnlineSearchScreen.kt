/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.screens.search

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.sonara.music.LocalNavController
import com.sonara.music.ui.screens.MoodAndGenresTile
import com.sonara.innertube.models.AlbumItem
import com.sonara.innertube.models.ArtistItem
import com.sonara.innertube.models.EpisodeItem
import com.sonara.innertube.models.PlaylistItem
import com.sonara.innertube.models.PodcastItem
import com.sonara.innertube.models.SongItem
import com.sonara.innertube.models.YTItem
import com.sonara.music.LocalDatabase
import com.sonara.music.LocalPlayerConnection
import com.sonara.music.R
import com.sonara.music.constants.AutoRadioQueueKey
import com.sonara.music.constants.SuggestionItemHeight
import com.sonara.music.extensions.toMediaItem
import com.sonara.music.models.toMediaMetadata
import com.sonara.music.playback.queues.ListQueue
import com.sonara.music.playback.queues.YouTubeQueue
import com.sonara.music.ui.component.LocalMenuState
import com.sonara.music.ui.component.YouTubeListItem
import com.sonara.music.ui.component.shimmer.ListItemPlaceHolder
import com.sonara.music.ui.component.shimmer.ShimmerHost
import com.sonara.music.ui.menu.YouTubeAlbumMenu
import com.sonara.music.ui.menu.YouTubeArtistMenu
import com.sonara.music.ui.menu.YouTubePlaylistMenu
import com.sonara.music.ui.menu.YouTubeSongMenu
import com.sonara.music.utils.makeTimeString
import com.sonara.music.utils.rememberPreference
import com.sonara.music.viewmodels.MoodAndGenresViewModel
import com.sonara.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
    moodAndGenresViewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val coroutineScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val moodAndGenresList by moodAndGenresViewModel.moodAndGenres.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()
    val localConfiguration = LocalConfiguration.current
    val categoriesPerRow = if (localConfiguration.orientation == ORIENTATION_LANDSCAPE) 3 else 2

    val autoRadioQueue by rememberPreference(AutoRadioQueueKey, defaultValue = true)

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        snapshotFlow { query }.debounce(300L).collectLatest {
            viewModel.query.value = it
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background),
    ) {
        if (viewState.isUrlQuery && viewState.parsedUrlItem != null) {
            item(key = "parsed_url_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.parsed_from_link),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            item(key = "parsed_url_item") {
                val item = viewState.parsedUrlItem!!
                YouTubeListItem(
                    item = item,
                    isActive = when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        is EpisodeItem -> mediaMetadata?.id == item.id
                        else -> false
                    },
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(
                                            song = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is AlbumItem -> YouTubeAlbumMenu(
                                            albumItem = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is ArtistItem -> YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is PlaylistItem -> YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is PodcastItem -> YouTubePlaylistMenu(
                                            playlist = item.asPlaylistItem(),
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is EpisodeItem -> YouTubeSongMenu(
                                            song = item.asSongItem(),
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is SongItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                if (autoRadioQueue) {
                                                    YouTubeQueue.radio(item.toMediaMetadata())
                                                } else {
                                                    ListQueue(
                                                        title = item.title,
                                                        items = listOf(item.toMediaItem()),
                                                    )
                                                },
                                            )
                                            onDismiss()
                                        }
                                    }
                                    is AlbumItem -> {
                                        navController.navigate("album/${item.id}")
                                        onDismiss()
                                    }
                                    is ArtistItem -> {
                                        navController.navigate("artist/${item.id}")
                                        onDismiss()
                                    }
                                    is PlaylistItem -> {
                                        navController.navigate("online_playlist/${item.id}")
                                        onDismiss()
                                    }
                                    is PodcastItem -> {
                                        navController.navigate("online_podcast/${item.id}")
                                        onDismiss()
                                    }
                                    is EpisodeItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(item.toMediaMetadata()),
                                            )
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(
                                            song = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is AlbumItem -> YouTubeAlbumMenu(
                                            albumItem = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is ArtistItem -> YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is PlaylistItem -> YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is PodcastItem -> YouTubePlaylistMenu(
                                            playlist = item.asPlaylistItem(),
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                        is EpisodeItem -> YouTubeSongMenu(
                                            song = item.asSongItem(),
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }
                                }
                            },
                        )
                        .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                        .animateItem(),
                )
            }

            item(key = "parsed_url_divider") {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .animateItem(),
                )
            }
        }

        if (viewState.history.isNotEmpty()) {
            item(key = "history_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.search_history),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        items(viewState.history, key = { "history_${it.query}" }) { history ->
            SuggestionItem(
                query = history.query,
                online = false,
                onClick = {
                    onSearch(history.query)
                    onDismiss()
                },
                onDelete = {
                    database.query {
                        delete(history)
                    }
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(history.query, TextRange(history.query.length)))
                },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack,
            )
        }

        items(viewState.suggestions, key = { "suggestion_$it" }) { queryItem ->
            SuggestionItem(
                query = queryItem,
                online = true,
                onClick = {
                    onSearch(queryItem)
                    onDismiss()
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(queryItem, TextRange(queryItem.length)))
                },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack,
            )
        }

        if (viewState.items.isNotEmpty() && viewState.history.size + viewState.suggestions.size > 0) {
            item(key = "search_divider") {
                HorizontalDivider(
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "search_divider_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (viewState.items.isNotEmpty()) {
            val topItem = viewState.items.first()
            item(key = "spotlight_${topItem.id}") {
                SpotlightItemCard(
                    item = topItem,
                    isPlaying = isPlaying,
                    isActive = when (topItem) {
                        is SongItem -> mediaMetadata?.id == topItem.id
                        is AlbumItem -> mediaMetadata?.album?.id == topItem.id
                        is EpisodeItem -> mediaMetadata?.id == topItem.id
                        else -> false
                    },
                    pureBlack = pureBlack,
                    onClick = {
                        when (topItem) {
                            is SongItem -> {
                                if (topItem.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        if (autoRadioQueue) {
                                            YouTubeQueue.radio(topItem.toMediaMetadata())
                                        } else {
                                            ListQueue(
                                                title = topItem.title,
                                                items = listOf(topItem.toMediaItem()),
                                            )
                                        },
                                    )
                                    onDismiss()
                                }
                            }
                            is AlbumItem -> {
                                navController.navigate("album/${topItem.id}")
                                onDismiss()
                            }
                            is ArtistItem -> {
                                navController.navigate("artist/${topItem.id}")
                                onDismiss()
                            }
                            is PlaylistItem -> {
                                navController.navigate("online_playlist/${topItem.id}")
                                onDismiss()
                            }
                            is PodcastItem -> {
                                navController.navigate("online_podcast/${topItem.id}")
                                onDismiss()
                            }
                            is EpisodeItem -> {
                                if (topItem.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue.radio(topItem.toMediaMetadata()),
                                    )
                                    onDismiss()
                                }
                            }
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (topItem) {
                                is SongItem -> YouTubeSongMenu(song = topItem, onDismiss = { menuState.dismiss(); onDismiss() })
                                is AlbumItem -> YouTubeAlbumMenu(albumItem = topItem, onDismiss = { menuState.dismiss(); onDismiss() })
                                is ArtistItem -> YouTubeArtistMenu(artist = topItem, onDismiss = { menuState.dismiss(); onDismiss() })
                                is PlaylistItem -> YouTubePlaylistMenu(playlist = topItem, coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                is PodcastItem -> YouTubePlaylistMenu(playlist = topItem.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                is EpisodeItem -> YouTubeSongMenu(song = topItem.asSongItem(), onDismiss = { menuState.dismiss(); onDismiss() })
                            }
                        }
                    },
                    onMenuClick = {
                        menuState.show {
                            when (topItem) {
                                is SongItem -> YouTubeSongMenu(song = topItem, onDismiss = { menuState.dismiss(); onDismiss() })
                                is AlbumItem -> YouTubeAlbumMenu(albumItem = topItem, onDismiss = { menuState.dismiss(); onDismiss() })
                                is ArtistItem -> YouTubeArtistMenu(artist = topItem, onDismiss = { menuState.dismiss(); onDismiss() })
                                is PlaylistItem -> YouTubePlaylistMenu(playlist = topItem, coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                is PodcastItem -> YouTubePlaylistMenu(playlist = topItem.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                is EpisodeItem -> YouTubeSongMenu(song = topItem.asSongItem(), onDismiss = { menuState.dismiss(); onDismiss() })
                            }
                        }
                    },
                    onPlayToggle = {
                        when (topItem) {
                            is SongItem -> {
                                if (topItem.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        if (autoRadioQueue) {
                                            YouTubeQueue.radio(topItem.toMediaMetadata())
                                        } else {
                                            ListQueue(
                                                title = topItem.title,
                                                items = listOf(topItem.toMediaItem()),
                                            )
                                        },
                                    )
                                    onDismiss()
                                }
                            }
                            is EpisodeItem -> {
                                if (topItem.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue.radio(topItem.toMediaMetadata()),
                                    )
                                    onDismiss()
                                }
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        items(viewState.items.drop(1), key = { "item_${it.id}" }) { item ->
            YouTubeListItem(
                item = item,
                isActive = when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    is EpisodeItem -> mediaMetadata?.id == item.id
                    else -> false
                },
                isPlaying = isPlaying,
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is PodcastItem -> YouTubePlaylistMenu(
                                        playlist = item.asPlaylistItem(),
                                        coroutineScope = coroutineScope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is EpisodeItem -> YouTubeSongMenu(
                                        song = item.asSongItem(),
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                        )
                    }
                },
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            if (autoRadioQueue) {
                                                YouTubeQueue.radio(item.toMediaMetadata())
                                            } else {
                                                ListQueue(
                                                    title = item.title,
                                                    items = listOf(item.toMediaItem()),
                                                )
                                            },
                                        )
                                        onDismiss()
                                    }
                                }
                                is AlbumItem -> {
                                    navController.navigate("album/${item.id}")
                                    onDismiss()
                                }
                                is ArtistItem -> {
                                    navController.navigate("artist/${item.id}")
                                    onDismiss()
                                }
                                is PlaylistItem -> {
                                    navController.navigate("online_playlist/${item.id}")
                                    onDismiss()
                                }
                                is PodcastItem -> {
                                    navController.navigate("online_podcast/${item.id}")
                                    onDismiss()
                                }
                                is EpisodeItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue.radio(item.toMediaMetadata()),
                                        )
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is PodcastItem -> YouTubePlaylistMenu(
                                        playlist = item.asPlaylistItem(),
                                        coroutineScope = coroutineScope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                    is EpisodeItem -> YouTubeSongMenu(
                                        song = item.asSongItem(),
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                }
                            }
                        },
                    )
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                    .animateItem(),
            )
        }

        if (query.isEmpty()) {
            item(key = "atmospheric_categories_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                        .animateItem(),
                ) {
                    Text(
                        text = stringResource(R.string.mood_and_genres),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Explore & Moods",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (moodAndGenresList == null) {
                item(key = "atmospheric_categories_shimmer") {
                    ShimmerHost(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    ) {
                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            } else {
                moodAndGenresList?.forEachIndexed { sectionIndex, moodAndGenres ->
                    item(key = "mood_section_$sectionIndex") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .animateItem(),
                        ) {
                            if (sectionIndex > 0) {
                                Text(
                                    text = moodAndGenres.title,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 6.dp),
                                )
                            }
                            moodAndGenres.items.chunked(categoriesPerRow).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    rowItems.forEach { catItem ->
                                        MoodAndGenresTile(
                                            title = catItem.title,
                                            accentColor = catItem.stripeColor,
                                            onClick = {
                                                navController.navigate("youtube_browse/${catItem.endpoint.browseId}?params=${catItem.endpoint.params}")
                                                onDismiss()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(6.dp)
                                                .aspectRatio(2.1f),
                                        )
                                    }
                                    repeat(categoriesPerRow - rowItems.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpotlightItemCard(
    item: YTItem,
    isPlaying: Boolean,
    isActive: Boolean,
    pureBlack: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    onPlayToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnailUrl = when (item) {
        is SongItem -> item.thumbnail
        is AlbumItem -> item.thumbnail
        is ArtistItem -> item.thumbnail
        is PlaylistItem -> item.thumbnail
        is EpisodeItem -> item.thumbnail
        is PodcastItem -> item.thumbnail
        else -> null
    }

    val subtitle = when (item) {
        is SongItem -> listOfNotNull(
            item.artists.joinToString { it.name }.takeIf { it.isNotEmpty() },
            item.duration?.let { makeTimeString(it * 1000L) },
        ).joinToString(" • ")
        is AlbumItem -> listOfNotNull(
            item.artists?.joinToString { it.name },
            item.year?.toString(),
        ).joinToString(" • ")
        is ArtistItem -> stringResource(R.string.artists)
        is PlaylistItem -> listOfNotNull(
            item.author?.name,
            item.songCountText,
        ).joinToString(" • ")
        is EpisodeItem -> item.publishDateText ?: ""
        is PodcastItem -> item.author?.name ?: ""
        else -> ""
    }

    val typeBadge = when (item) {
        is SongItem -> "TOP TRACK"
        is AlbumItem -> "ALBUM"
        is ArtistItem -> "ARTIST"
        is PlaylistItem -> "PLAYLIST"
        is EpisodeItem -> "EPISODE"
        is PodcastItem -> "PODCAST"
        else -> "FEATURED"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        if (pureBlack) Color(0xFF141416) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(if (item is ArtistItem) CircleShape else RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = typeBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (item is SongItem || item is EpisodeItem) {
                IconButton(
                    onClick = onPlayToggle,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                ) {
                    Icon(
                        painter = painterResource(if (isActive && isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    pureBlack: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(SuggestionItemHeight)
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Icon(
            painterResource(if (online) R.drawable.search else R.drawable.history),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .alpha(0.5f),
        )

        Text(
            text = query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (!online) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.alpha(0.5f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                )
            }
        }

        IconButton(
            onClick = onFillTextField,
            modifier = Modifier.alpha(0.5f),
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_top_left),
                contentDescription = null,
            )
        }
    }
}
