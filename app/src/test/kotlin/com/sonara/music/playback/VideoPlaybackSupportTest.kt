package com.sonara.music.playback

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.sonara.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.sonara.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.sonara.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import com.sonara.music.constants.VideoQuality
import com.sonara.music.db.entities.SongEntity
import com.sonara.music.models.MediaMetadata
import com.sonara.music.ui.player.isVideoTrack
import com.sonara.music.utils.InnerTubeXPlayer.resolveMaxHeight
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VideoPlaybackSupportTest {

    private class FakeRangeUpstream(
        private val payload: ByteArray,
        private val includeContentRange: Boolean = true,
    ) : DataSource {
        val openedSpecs = mutableListOf<DataSpec>()
        private var currentUri: Uri? = null
        private var pos = 0
        private var endExclusive = 0
        private var headers: Map<String, List<String>> = emptyMap()

        override fun addTransferListener(transferListener: TransferListener) = Unit

        override fun open(dataSpec: DataSpec): Long {
            openedSpecs += dataSpec
            currentUri = dataSpec.uri
            val start = dataSpec.position.toInt().coerceIn(0, payload.size)
            val end =
                if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                    payload.size
                } else {
                    (start + dataSpec.length.toInt()).coerceAtMost(payload.size)
                }
            pos = start
            endExclusive = end
            headers =
                if (includeContentRange && end > start) {
                    mapOf("Content-Range" to listOf("bytes $start-${end - 1}/${payload.size}"))
                } else {
                    emptyMap()
                }
            return (end - start).toLong()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (pos >= endExclusive) return C.RESULT_END_OF_INPUT
            val count = minOf(length, endExclusive - pos)
            System.arraycopy(payload, pos, buffer, offset, count)
            pos += count
            return count
        }

        override fun getUri(): Uri? = currentUri

        override fun getResponseHeaders(): Map<String, List<String>> = headers

        override fun close() = Unit
    }

    private fun readAll(dataSource: DataSource, spec: DataSpec): ByteArray {
        dataSource.open(spec)
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(3)
        try {
            while (true) {
                val n = dataSource.read(buf, 0, buf.size)
                if (n == C.RESULT_END_OF_INPUT) break
                out.write(buf, 0, n)
            }
        } finally {
            dataSource.close()
        }
        return out.toByteArray()
    }

    @Test
    fun `chunked range data source streams across multiple bounded chunks`() {
        val payload = ByteArray(10) { (it + 1).toByte() }
        val upstream = FakeRangeUpstream(payload)
        val ds =
            ChunkedRangeDataSource(
                upstream = upstream,
                streamUrl = "https://example.com/video.mp4",
                requestHeaders = mapOf("User-Agent" to "SonaraTest"),
                requireBoundedRange = true,
                useRangeChunks = true,
                rangeChunkSizeBytes = 4L,
                initialContentLengthBytes = null,
            )

        val result = readAll(ds, DataSpec("https://placeholder/video".toUri()))

        assertArrayEquals(payload, result)
        assertEquals(3, upstream.openedSpecs.size)
        assertEquals(0L, upstream.openedSpecs[0].position)
        assertEquals(4L, upstream.openedSpecs[0].length)
        assertEquals(4L, upstream.openedSpecs[1].position)
        assertEquals(4L, upstream.openedSpecs[1].length)
        assertEquals(8L, upstream.openedSpecs[2].position)
        assertEquals(2L, upstream.openedSpecs[2].length)
        assertEquals("SonaraTest", upstream.openedSpecs[0].httpRequestHeaders["User-Agent"])
    }

    @Test
    fun `chunked range data source supports mid-stream seek and bounded length`() {
        val payload = ByteArray(12) { it.toByte() }
        val upstream = FakeRangeUpstream(payload)
        val ds =
            ChunkedRangeDataSource(
                upstream = upstream,
                streamUrl = "https://example.com/video.mp4",
                requestHeaders = emptyMap(),
                requireBoundedRange = true,
                useRangeChunks = false,
                rangeChunkSizeBytes = 3L,
                initialContentLengthBytes = 12L,
            )

        val spec =
            DataSpec
                .Builder()
                .setUri("https://placeholder/video".toUri())
                .setPosition(5L)
                .setLength(5L)
                .build()
        val result = readAll(ds, spec)

        assertArrayEquals(byteArrayOf(5, 6, 7, 8, 9), result)
        assertEquals(2, upstream.openedSpecs.size)
        assertEquals(5L, upstream.openedSpecs[0].position)
        assertEquals(3L, upstream.openedSpecs[0].length)
        assertEquals(8L, upstream.openedSpecs[1].position)
        assertEquals(2L, upstream.openedSpecs[1].length)
    }

    @Test
    fun `chunked range data source passes through when chunking disabled`() {
        val payload = ByteArray(9) { (it * 2).toByte() }
        val upstream = FakeRangeUpstream(payload)
        val ds =
            ChunkedRangeDataSource(
                upstream = upstream,
                streamUrl = "https://example.com/direct.mp4",
                requestHeaders = mapOf("X-Test" to "1"),
                requireBoundedRange = false,
                useRangeChunks = false,
                rangeChunkSizeBytes = 4L,
                initialContentLengthBytes = 9L,
            )

        val result = readAll(ds, DataSpec("https://placeholder/video".toUri()))

        assertArrayEquals(payload, result)
        assertEquals(1, upstream.openedSpecs.size)
        assertEquals(C.LENGTH_UNSET.toLong(), upstream.openedSpecs[0].length)
    }

    @Test
    fun `content range total length parser handles valid and invalid headers`() {
        assertEquals(123456L, ChunkedRangeDataSource.parseTotalLengthFromContentRange("bytes 0-1023/123456"))
        assertNull(ChunkedRangeDataSource.parseTotalLengthFromContentRange("bytes 0-1023/*"))
        assertNull(ChunkedRangeDataSource.parseTotalLengthFromContentRange(null))
    }

    @Test
    fun `video quality resolves wifi and cellular max heights`() {
        assertEquals(720, VideoQuality.AUTO.resolveMaxHeight(isMetered = false))
        assertEquals(480, VideoQuality.AUTO.resolveMaxHeight(isMetered = true))
        assertEquals(360, VideoQuality.LOW.resolveMaxHeight(isMetered = false))
        assertEquals(480, VideoQuality.MEDIUM.resolveMaxHeight(isMetered = false))
        assertEquals(720, VideoQuality.HIGH.resolveMaxHeight(isMetered = true))
        assertEquals(1080, VideoQuality.VERY_HIGH.resolveMaxHeight(isMetered = true))
    }

    @Test
    fun `isVideoTrack distinguishes video songs from audio tracks`() {
        val omv = MediaMetadata(id = "v1", title = "MV", artists = emptyList(), duration = 200, musicVideoType = MUSIC_VIDEO_TYPE_OMV)
        val ugc = MediaMetadata(id = "v2", title = "Live", artists = emptyList(), duration = 200, musicVideoType = MUSIC_VIDEO_TYPE_UGC)
        val atv = MediaMetadata(id = "a1", title = "Song", artists = emptyList(), duration = 200, musicVideoType = MUSIC_VIDEO_TYPE_ATV, thumbnailUrl = "https://i.ytimg.com/vi/a1/hqdefault.jpg")
        val legacyDbVideo = MediaMetadata(id = "v3", title = "Old", artists = emptyList(), duration = 200, musicVideoType = null)
        val dbVideoEntity = SongEntity(id = "v3", title = "Old", isVideo = true)
        val ytLinkVideo = MediaMetadata(id = "v4", title = "Clip", artists = emptyList(), duration = 200, musicVideoType = null, album = null, thumbnailUrl = "https://i.ytimg.com/vi/v4/maxresdefault.jpg")
        val regularAlbumSong = MediaMetadata(id = "a2", title = "Track", artists = emptyList(), duration = 200, musicVideoType = null, album = MediaMetadata.Album("alb1", "Album"), thumbnailUrl = "https://lh3.googleusercontent.com/abc")

        assertTrue(isVideoTrack(omv, null))
        assertTrue(isVideoTrack(ugc, null))
        assertFalse(isVideoTrack(atv, SongEntity(id = "a1", title = "Song", isVideo = true)))
        assertTrue(isVideoTrack(legacyDbVideo, dbVideoEntity))
        assertTrue(isVideoTrack(ytLinkVideo, null))
        assertFalse(isVideoTrack(regularAlbumSong, null))
        assertFalse(isVideoTrack(null, null))
    }
}
