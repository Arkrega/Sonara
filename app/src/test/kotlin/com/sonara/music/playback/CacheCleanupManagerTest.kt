package com.sonara.music.playback

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.DefaultContentMetadata
import com.sonara.music.constants.CacheAutoClearInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.NavigableSet
import java.util.TreeSet

class CacheCleanupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeCache : Cache {
        private val spansByKey = LinkedHashMap<String, NavigableSet<CacheSpan>>()
        private val lengthByKey = HashMap<String, Long>()
        val removedKeys = mutableListOf<String>()

        fun putCachedSpan(
            key: String,
            position: Long,
            length: Long,
            lastTouchTimestamp: Long,
            totalContentLength: Long? = length,
        ) {
            val set = spansByKey.getOrPut(key) { TreeSet() }
            set.add(CacheSpan(key, position, length, lastTouchTimestamp, File("dummy_$key")))
            if (totalContentLength != null) {
                lengthByKey[key] = totalContentLength
            }
        }

        override fun getUid(): Long = 1L

        override fun release() = Unit

        override fun addListener(key: String, listener: Cache.Listener): NavigableSet<CacheSpan> =
            getCachedSpans(key)

        override fun removeListener(key: String, listener: Cache.Listener) = Unit

        override fun getCachedSpans(key: String): NavigableSet<CacheSpan> =
            TreeSet(spansByKey[key].orEmpty())

        override fun getKeys(): MutableSet<String> = LinkedHashSet(spansByKey.keys)

        override fun getCacheSpace(): Long =
            spansByKey.values.sumOf { spans -> spans.sumOf { it.length } }

        override fun startReadWrite(key: String, position: Long, length: Long): CacheSpan =
            throw UnsupportedOperationException()

        override fun startReadWriteNonBlocking(key: String, position: Long, length: Long): CacheSpan? = null

        override fun startFile(key: String, position: Long, length: Long): File =
            throw UnsupportedOperationException()

        override fun commitFile(file: File, length: Long) = Unit

        override fun releaseHoleSpan(holeSpan: CacheSpan) = Unit

        override fun removeResource(key: String) {
            removedKeys += key
            spansByKey.remove(key)
            lengthByKey.remove(key)
        }

        override fun removeSpan(span: CacheSpan) {
            spansByKey[span.key]?.remove(span)
        }

        override fun isCached(key: String, position: Long, length: Long): Boolean {
            val spans = spansByKey[key] ?: return false
            val covered = spans.filter { it.position >= position && it.position + it.length <= position + length }
                .sumOf { it.length }
            return covered >= length
        }

        override fun getCachedLength(key: String, position: Long, length: Long): Long = 0L

        override fun getCachedBytes(key: String, position: Long, length: Long): Long = 0L

        override fun applyContentMetadataMutations(key: String, mutations: ContentMetadataMutations) = Unit

        override fun getContentMetadata(key: String): ContentMetadata {
            val length = lengthByKey[key] ?: return DefaultContentMetadata.EMPTY
            return object : ContentMetadata {
                override fun get(name: String, defaultValue: String?): String? = defaultValue

                override fun get(name: String, defaultValue: ByteArray?): ByteArray? = defaultValue

                override fun get(name: String, defaultValue: Long): Long =
                    if (name == ContentMetadata.KEY_CONTENT_LENGTH) length else defaultValue

                override fun contains(name: String): Boolean = name == ContentMetadata.KEY_CONTENT_LENGTH
            }
        }
    }

    @Test
    fun `evicts entries untouched beyond 7 days and keeps recently played entries`() {
        val now = 20L * CacheCleanupManager.MILLIS_PER_DAY
        val sevenDays = 7L * CacheCleanupManager.MILLIS_PER_DAY
        val cache = FakeCache()
        cache.putCachedSpan("old_song", 0L, 1000L, lastTouchTimestamp = now - sevenDays - 1_000L)
        cache.putCachedSpan("fresh_song", 0L, 1000L, lastTouchTimestamp = now - 2L * CacheCleanupManager.MILLIS_PER_DAY)
        cache.putCachedSpan("mixed_song", 0L, 500L, lastTouchTimestamp = now - 10L * CacheCleanupManager.MILLIS_PER_DAY)
        cache.putCachedSpan("mixed_song", 500L, 500L, lastTouchTimestamp = now - 1_000L)

        val removed = CacheCleanupManager.evictExpiredCacheEntries(cache, sevenDays, now)

        assertEquals(setOf("old_song"), removed)
        assertFalse(cache.keys.contains("old_song"))
        assertTrue(cache.keys.contains("fresh_song"))
        assertTrue(cache.keys.contains("mixed_song"))
    }

    @Test
    fun `does not evict entries when auto clear is disabled`() {
        val now = 20L * CacheCleanupManager.MILLIS_PER_DAY
        val cache = FakeCache()
        cache.putCachedSpan("old_song", 0L, 1000L, lastTouchTimestamp = 1_000L)

        val removed = CacheCleanupManager.evictExpiredCacheEntries(cache, maxAgeMs = 0L, nowMs = now)

        assertTrue(removed.isEmpty())
        assertTrue(cache.keys.contains("old_song"))
    }

    @Test
    fun `evicts expired image files while preserving journal and downloaded thumbnails`() {
        val dir = tempFolder.newFolder("coil")
        val now = 30L * CacheCleanupManager.MILLIS_PER_DAY
        val sevenDays = 7L * CacheCleanupManager.MILLIS_PER_DAY

        val oldThumb = File(dir, "aaaa1111.0").apply { writeText("old"); setLastModified(now - sevenDays - 5_000L) }
        val preservedThumb = File(dir, "bbbb2222.0").apply { writeText("dl"); setLastModified(now - sevenDays - 5_000L) }
        val recentThumb = File(dir, "cccc3333.0").apply { writeText("new"); setLastModified(now - 60_000L) }
        val journalFile = File(dir, "journal").apply { writeText("j"); setLastModified(now - sevenDays - 5_000L) }

        val deleted =
            CacheCleanupManager.evictExpiredImageCacheFiles(
                directory = dir,
                preservedHashes = setOf("bbbb2222"),
                maxAgeMs = sevenDays,
                nowMs = now,
            )

        assertEquals(1, deleted)
        assertFalse(oldThumb.exists())
        assertTrue(preservedThumb.exists())
        assertTrue(recentThumb.exists())
        assertTrue(journalFile.exists())
    }

    @Test
    fun `finds best complete cached video key and ignores incomplete entries`() {
        val cache = FakeCache()
        val partialKey = CacheCleanupManager.videoCacheKey("abc", 720, 136)
        val complete480 = CacheCleanupManager.videoCacheKey("abc", 480, 135)
        val complete720 = CacheCleanupManager.videoCacheKey("abc", 720, 247)

        cache.putCachedSpan(partialKey, 0L, 500L, lastTouchTimestamp = 1_000L, totalContentLength = 1000L)
        cache.putCachedSpan(complete480, 0L, 800L, lastTouchTimestamp = 1_000L, totalContentLength = 800L)
        cache.putCachedSpan(complete720, 0L, 1200L, lastTouchTimestamp = 1_000L, totalContentLength = 1200L)

        assertEquals(complete720, CacheCleanupManager.findCompleteCachedVideoKey(cache, "abc", preferredMaxHeight = 720))
        assertEquals(complete480, CacheCleanupManager.findCompleteCachedVideoKey(cache, "abc", preferredMaxHeight = 480))
        assertEquals(complete480, CacheCleanupManager.findCompleteCachedVideoKey(cache, "abc", preferredMaxHeight = 360))
        assertNull(CacheCleanupManager.findCompleteCachedVideoKey(cache, "other", preferredMaxHeight = 720))
    }

    @Test
    fun `clears stale partial video entry only when expected content length mismatches`() {
        val cache = FakeCache()
        val key = CacheCleanupManager.videoCacheKey("vid1", 720, 136)
        cache.putCachedSpan(key, 0L, 400L, lastTouchTimestamp = 1_000L, totalContentLength = 1000L)

        CacheCleanupManager.clearStalePartialVideoEntry(cache, key, expectedContentLength = 1000L)
        assertTrue(cache.keys.contains(key))

        CacheCleanupManager.clearStalePartialVideoEntry(cache, key, expectedContentLength = 2000L)
        assertFalse(cache.keys.contains(key))
    }

    @Test
    fun `auto clear interval defaults to 7 days`() {
        assertEquals(CacheAutoClearInterval.DAYS_7, CacheAutoClearInterval.fromDays(7))
        assertEquals(CacheAutoClearInterval.OFF, CacheAutoClearInterval.fromDays(0))
        assertEquals(CacheAutoClearInterval.DAYS_7, CacheAutoClearInterval.fromDays(999))
    }
}
