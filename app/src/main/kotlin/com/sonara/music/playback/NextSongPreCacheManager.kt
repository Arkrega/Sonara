package com.sonara.music.playback

import android.net.ConnectivityManager
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.metrolist.innertubex.extraction.ContentHints
import com.sonara.music.constants.AudioQuality
import com.sonara.music.utils.InnerTubeXPlayer
import com.sonara.music.utils.SharedHttpPool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
object NextSongPreCacheManager {
    private const val TAG = "NextSongPreCache"
    const val MAX_PRECACHE_BYTES = 8L * 1024L * 1024L

    fun shouldPreCacheNextTrack(
        enabled: Boolean,
        songCacheEnabled: Boolean,
        maxSongCacheSizeMb: Int,
        wifiOnly: Boolean,
        isNetworkMetered: Boolean,
        isAlreadyDownloaded: Boolean,
        isAlreadyCached: Boolean,
    ): Boolean =
        enabled &&
            songCacheEnabled &&
            maxSongCacheSizeMb != 0 &&
            !(wifiOnly && isNetworkMetered) &&
            !isAlreadyDownloaded &&
            !isAlreadyCached

    suspend fun preCacheTrack(
        mediaId: String,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        playerCache: Cache,
        contentHints: ContentHints,
    ): Boolean {
        val playbackData =
            InnerTubeXPlayer.playerResponseForPlayback(
                videoId = mediaId,
                audioQuality = audioQuality,
                connectivityManager = connectivityManager,
                contentHints = contentHints,
            ).getOrNull() ?: return false

        val totalLength = playbackData.format.contentLength
        val targetBytes =
            if (totalLength != null && totalLength > 0L) {
                minOf(totalLength, MAX_PRECACHE_BYTES)
            } else {
                MAX_PRECACHE_BYTES
            }

        val okHttpClient = SharedHttpPool.proxiedClient()

        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient).apply {
            if (playbackData.streamHeaders.isNotEmpty()) {
                setDefaultRequestProperties(playbackData.streamHeaders)
            }
        }

        val cacheDataSource =
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(
                    CacheDataSink
                        .Factory()
                        .setCache(playerCache)
                        .setFragmentSize(CacheDataSink.DEFAULT_FRAGMENT_SIZE),
                )
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSource()

        val dataSpec =
            DataSpec
                .Builder()
                .setUri(playbackData.streamUrl)
                .setKey(mediaId)
                .setPosition(0L)
                .setLength(targetBytes)
                .build()

        return try {
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val writer = CacheWriter(cacheDataSource, dataSpec, null, null)
                    continuation.invokeOnCancellation {
                        runCatching { writer.cancel() }
                    }
                    try {
                        writer.cache()
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            }
            Timber.tag(TAG).d("Pre-cached %d bytes for next track %s", targetBytes, mediaId)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Skipped pre-cache for %s", mediaId)
            false
        }
    }
}
