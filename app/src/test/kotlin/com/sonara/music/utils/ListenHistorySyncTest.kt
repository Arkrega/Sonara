package com.sonara.music.utils

import com.sonara.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ListenHistorySyncTest {

    private fun song(id: String) = SongItem(
        id = id,
        title = "Track $id",
        artists = emptyList(),
        thumbnail = "",
    )

    @Test
    fun firstSyncImportsAllRemoteSongsOldestToNewest() {
        val remote = listOf(song("jawa-newest"), song("jawa-mid"), song("jawa-oldest"))

        val selected = selectNewRemoteHistorySongs(
            remoteSongs = remote,
            previousSnapshotIds = null,
            recentLocalEventSongIds = emptySet(),
            allLocalEventSongIds = emptySet(),
        )

        assertEquals(listOf("jawa-oldest", "jawa-mid", "jawa-newest"), selected.map { it.id })
    }

    @Test
    fun repeatSyncWithNoNewRemotePlaysImportsNothing() {
        val remote = listOf(song("jawa-1"), song("jawa-2"))

        val selected = selectNewRemoteHistorySongs(
            remoteSongs = remote,
            previousSnapshotIds = listOf("jawa-1", "jawa-2"),
            recentLocalEventSongIds = emptySet(),
            allLocalEventSongIds = setOf("jawa-1", "jawa-2"),
        )

        assertEquals(emptyList<String>(), selected.map { it.id })
    }

    @Test
    fun sonaraLocalPlayEchoedByYouTubeDoesNotDuplicate() {
        val remote = listOf(song("anime-from-sonara"), song("jawa-1"))

        val selected = selectNewRemoteHistorySongs(
            remoteSongs = remote,
            previousSnapshotIds = listOf("jawa-1"),
            recentLocalEventSongIds = setOf("anime-from-sonara"),
            allLocalEventSongIds = setOf("jawa-1", "anime-from-sonara"),
        )

        assertEquals(emptyList<String>(), selected.map { it.id })
    }

    @Test
    fun newRemotePlayOnYouTubeMusicImportsIntoSonara() {
        val remote = listOf(song("jawa-new-1"), song("jawa-new-2"), song("anime-prev"))

        val selected = selectNewRemoteHistorySongs(
            remoteSongs = remote,
            previousSnapshotIds = listOf("anime-prev"),
            recentLocalEventSongIds = setOf("anime-prev"),
            allLocalEventSongIds = setOf("anime-prev"),
        )

        assertEquals(listOf("jawa-new-2", "jawa-new-1"), selected.map { it.id })
    }
}
