package com.sonara.music.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.sonara.music.db.MusicDatabase
import com.sonara.music.utils.InnerTubeXPlayer
import kotlinx.coroutines.flow.first
import okio.ByteString.Companion.encodeUtf8
import timber.log.Timber
import java.io.File

@OptIn(UnstableApi::class)
object CacheCleanupManager {
    private const val TAG = "CacheCleanupManager"
    const val DEFAULT_AUTO_CLEAR_DAYS = 7
    const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun evictExpiredCacheEntries(
        cache: Cache,
        maxAgeMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): Set<String> {
        if (maxAgeMs <= 0L) return emptySet()
        val cutoffMs = nowMs - maxAgeMs
        val removed = LinkedHashSet<String>()
        val keysSnapshot = runCatching { cache.keys.toList() }.getOrDefault(emptyList())
        for (key in keysSnapshot) {
            val latestTouch =
                runCatching {
                    cache.getCachedSpans(key).maxOfOrNull { it.lastTouchTimestamp }
                }.getOrNull()
            if (latestTouch != null && latestTouch in 1 until cutoffMs) {
                runCatching { cache.removeResource(key) }
                    .onSuccess { removed.add(key) }
            }
        }
        return removed
    }

    fun evictExpiredImageCacheFiles(
        directory: File,
        preservedHashes: Set<String>,
        maxAgeMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): Int {
        if (maxAgeMs <= 0L || !directory.exists() || !directory.isDirectory) return 0
        val cutoffMs = nowMs - maxAgeMs
        var deletedCount = 0
        directory.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith("journal")) {
                val isPreserved = preservedHashes.any { hash -> file.name.startsWith(hash) }
                val modified = file.lastModified()
                if (!isPreserved && modified in 1 until cutoffMs) {
                    if (runCatching { file.delete() }.getOrDefault(false)) {
                        deletedCount++
                    }
                }
            }
        }
        return deletedCount
    }

    @kotlin.OptIn(ExperimentalCoilApi::class)
    suspend fun cleanupExpiredCaches(
        context: Context,
        database: MusicDatabase,
        playerCache: Cache,
        videoCache: Cache,
        maxAgeDays: Int,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        if (maxAgeDays <= 0) return
        val maxAgeMs = maxAgeDays.toLong() * MILLIS_PER_DAY

        val removedSongs = evictExpiredCacheEntries(playerCache, maxAgeMs, nowMs)
        if (removedSongs.isNotEmpty()) {
            database.query {
                removedSongs.forEach { songId ->
                    getSongByIdBlocking(songId)?.let { song ->
                        if (!song.song.isDownloaded && song.song.dateDownload != null) {
                            update(song.song.copy(dateDownload = null))
                        }
                    }
                }
            }
        }

        val removedVideos = evictExpiredCacheEntries(videoCache, maxAgeMs, nowMs)

        val preservedHashes = collectDownloadedThumbnailHashes(database)
        val imageDir = runCatching { context.imageLoader.diskCache?.directory?.toFile() }.getOrNull()
        val removedImages =
            if (imageDir != null) {
                evictExpiredImageCacheFiles(imageDir, preservedHashes, maxAgeMs, nowMs)
            } else {
                0
            }

        if (removedSongs.isNotEmpty() || removedVideos.isNotEmpty() || removedImages > 0) {
            Timber.tag(TAG).i(
                "Auto-cleared expired cache (>%dd): songs=%d videos=%d images=%d",
                maxAgeDays,
                removedSongs.size,
                removedVideos.size,
                removedImages,
            )
        }
    }

    @kotlin.OptIn(ExperimentalCoilApi::class)
    suspend fun clearAllCaches(
        context: Context,
        database: MusicDatabase,
        playerCache: Cache,
        videoCache: Cache,
    ) {
        val songKeys = runCatching { playerCache.keys.toList() }.getOrDefault(emptyList())
        songKeys.forEach { key -> runCatching { playerCache.removeResource(key) } }
        if (songKeys.isNotEmpty()) {
            database.query {
                songKeys.forEach { songId ->
                    getSongByIdBlocking(songId)?.let { song ->
                        if (!song.song.isDownloaded && song.song.dateDownload != null) {
                            update(song.song.copy(dateDownload = null))
                        }
                    }
                }
            }
        }

        val videoKeys = runCatching { videoCache.keys.toList() }.getOrDefault(emptyList())
        videoKeys.forEach { key -> runCatching { videoCache.removeResource(key) } }
        InnerTubeXPlayer.clearStreamClientFailures()

        val imageDiskCache = runCatching { context.imageLoader.diskCache }.getOrNull()
        if (imageDiskCache != null) {
            val preservedHashes = collectDownloadedThumbnailHashes(database)
            val directory = imageDiskCache.directory.toFile()
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    if (file.isFile && !file.name.startsWith("journal")) {
                        val isPreserved = preservedHashes.any { hash -> file.name.startsWith(hash) }
                        if (!isPreserved) {
                            runCatching { file.delete() }
                        }
                    }
                }
            }
            runCatching { imageDiskCache.clear() }
        }
    }

    private suspend fun collectDownloadedThumbnailHashes(database: MusicDatabase): Set<String> {
        val hashes = HashSet<String>()
        val downloadedSongs =
            try {
                database.downloadedSongsByNameAsc().first()
            } catch (_: Exception) {
                emptyList()
            }
        downloadedSongs.forEach { song ->
            song.song.thumbnailUrl?.let { hashes.add(it.encodeUtf8().sha256().hex()) }
            song.album?.thumbnailUrl?.let { hashes.add(it.encodeUtf8().sha256().hex()) }
        }
        return hashes
    }

    fun videoCacheKey(
        videoId: String,
        height: Int,
        itag: Int,
    ): String = "vid_${videoId}_${height}p_${itag}"

    internal fun parseHeightFromVideoCacheKey(
        key: String,
        videoId: String,
    ): Int? {
        val prefix = "vid_${videoId}_"
        if (!key.startsWith(prefix)) return null
        val remainder = key.removePrefix(prefix)
        val pIndex = remainder.indexOf('p')
        if (pIndex <= 0) return null
        return remainder.substring(0, pIndex).toIntOrNull()?.takeIf { it > 0 }
    }

    fun findCompleteCachedVideoKey(
        cache: Cache,
        videoId: String,
        preferredMaxHeight: Int,
    ): String? {
        val prefix = "vid_${videoId}_"
        val candidates =
            runCatching { cache.keys.toList() }
                .getOrDefault(emptyList())
                .asSequence()
                .filter { it.startsWith(prefix) }
                .mapNotNull { key ->
                    val contentLength =
                        runCatching {
                            ContentMetadata.getContentLength(cache.getContentMetadata(key))
                        }.getOrDefault(-1L)
                    val complete =
                        contentLength > 0L &&
                            runCatching { cache.isCached(key, 0L, contentLength) }.getOrDefault(false)
                    if (!complete) return@mapNotNull null
                    val height = parseHeightFromVideoCacheKey(key, videoId) ?: 0
                    key to height
                }
                .toList()
        if (candidates.isEmpty()) return null
        val withinLimit = candidates.filter { it.second in 1..preferredMaxHeight }.maxByOrNull { it.second }
        if (withinLimit != null) return withinLimit.first
        val aboveLimit = candidates.filter { it.second > preferredMaxHeight }.minByOrNull { it.second }
        return aboveLimit?.first ?: candidates.first().first
    }

    fun clearStalePartialVideoEntry(
        cache: Cache,
        cacheKey: String,
        expectedContentLength: Long?,
    ) {
        if (expectedContentLength == null || expectedContentLength <= 0L) return
        val storedLength =
            runCatching {
                ContentMetadata.getContentLength(cache.getContentMetadata(cacheKey))
            }.getOrDefault(-1L)
        if (storedLength > 0L && storedLength != expectedContentLength) {
            runCatching { cache.removeResource(cacheKey) }
        }
    }
}
