package com.sonara.music.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NextSongPreCacheManagerTest {

    @Test
    fun `pre-cache is enabled only when settings and network allow and track is uncached`() {
        assertTrue(
            NextSongPreCacheManager.shouldPreCacheNextTrack(
                enabled = true,
                songCacheEnabled = true,
                maxSongCacheSizeMb = 1024,
                wifiOnly = true,
                isNetworkMetered = false,
                isAlreadyDownloaded = false,
                isAlreadyCached = false,
            ),
        )
        assertTrue(
            NextSongPreCacheManager.shouldPreCacheNextTrack(
                enabled = true,
                songCacheEnabled = true,
                maxSongCacheSizeMb = -1,
                wifiOnly = false,
                isNetworkMetered = true,
                isAlreadyDownloaded = false,
                isAlreadyCached = false,
            ),
        )
    }

    @Test
    fun `pre-cache is skipped when disabled, metered on wifi-only, zero cache limit, or already cached`() {
        assertFalse(
            NextSongPreCacheManager.shouldPreCacheNextTrack(
                enabled = false,
                songCacheEnabled = true,
                maxSongCacheSizeMb = 1024,
                wifiOnly = false,
                isNetworkMetered = false,
                isAlreadyDownloaded = false,
                isAlreadyCached = false,
            ),
        )
        assertFalse(
            NextSongPreCacheManager.shouldPreCacheNextTrack(
                enabled = true,
                songCacheEnabled = false,
                maxSongCacheSizeMb = 1024,
                wifiOnly = false,
                isNetworkMetered = false,
                isAlreadyDownloaded = false,
                isAlreadyCached = false,
            ),
        )
        assertFalse(
            NextSongPreCacheManager.shouldPreCacheNextTrack(
                enabled = true,
                songCacheEnabled = true,
                maxSongCacheSizeMb = 0,
                wifiOnly = false,
                isNetworkMetered = false,
                isAlreadyDownloaded = false,
                isAlreadyCached = false,
            ),
        )
        assertFalse(
            NextSongPreCacheManager.shouldPreCacheNextTrack(
                enabled = true,
                songCacheEnabled = true,
                maxSongCacheSizeMb = 1024,
                wifiOnly = true,
                isNetworkMetered = true,
                isAlreadyDownloaded = false,
                isAlreadyCached = false,
            ),
        )
        assertFalse(
            NextSongPreCacheManager.shouldPreCacheNextTrack(
                enabled = true,
                songCacheEnabled = true,
                maxSongCacheSizeMb = 1024,
                wifiOnly = false,
                isNetworkMetered = false,
                isAlreadyDownloaded = true,
                isAlreadyCached = false,
            ),
        )
        assertFalse(
            NextSongPreCacheManager.shouldPreCacheNextTrack(
                enabled = true,
                songCacheEnabled = true,
                maxSongCacheSizeMb = 1024,
                wifiOnly = false,
                isNetworkMetered = false,
                isAlreadyDownloaded = false,
                isAlreadyCached = true,
            ),
        )
    }
}
