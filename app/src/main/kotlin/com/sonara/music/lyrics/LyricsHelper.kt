/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.lyrics

import android.content.Context
import android.util.LruCache
import com.sonara.music.constants.LyricsProviderOrderKey
import com.sonara.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.sonara.music.models.MediaMetadata
import com.sonara.music.utils.NetworkConnectivityObserver
import com.sonara.music.utils.dataStore
import com.sonara.music.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

private const val MAX_LYRICS_FETCH_MS = 18000L
private const val PER_PROVIDER_TIMEOUT_MS = 7000L
private const val PROVIDER_NONE = ""

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    val preferred =
        context.dataStore.data
            .map { preferences ->
                resolveLyricsProviders(preferences)
            }.distinctUntilChanged()

    private val bestCache = LruCache<String, LyricsWithProvider>(MAX_BEST_CACHE_SIZE)
    private val allCache = LruCache<String, List<LyricsResult>>(MAX_ALL_CACHE_SIZE)
    private val jobLock = Any()
    private var currentBestMediaId: String? = null
    private var currentBestDeferred: Deferred<LyricsWithProvider>? = null
    private var currentAllLyricsJob: Job? = null

    private data class ScoredProviderCandidate(
        val providerIndex: Int,
        val providerName: String,
        val lyrics: String,
        val evaluation: LyricsCandidateEvaluation,
    )

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        val orderedProviders = context.dataStore.data
            .map { preferences -> resolveLyricsProviders(preferences) }
            .first()
        val enabledProviders = orderedProviders.filter { it.isEnabled(context) }
        val signature = enabledProviders.joinToString("|") { it.name }
        val cacheKey = "${mediaMetadata.id}:$signature"

        bestCache.get(cacheKey)?.let { return it }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (_: Exception) {
            true
        }
        if (!isNetworkAvailable) {
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        if (enabledProviders.isEmpty()) {
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        val existingDeferred = synchronized(jobLock) {
            val active = currentBestDeferred
            if (currentBestMediaId == cacheKey && active != null && active.isActive) {
                active
            } else {
                active?.cancel()
                currentBestMediaId = cacheKey
                null
            }
        }
        if (existingDeferred != null) {
            val shared = try {
                existingDeferred.await()
            } catch (_: CancellationException) {
                currentCoroutineContext().ensureActive()
                null
            } catch (_: Exception) {
                null
            }
            if (shared != null) {
                return shared
            }
            synchronized(jobLock) {
                if (currentBestDeferred === existingDeferred) {
                    currentBestDeferred = null
                }
            }
        }

        val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
        val artistsText = mediaMetadata.artists.joinToString { it.name }

        val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            coroutineScope {
                val fetchDeferred = async {
                    val deferreds: List<Deferred<ScoredProviderCandidate?>> = enabledProviders.mapIndexed { index, provider ->
                        async {
                            val rawResult = try {
                                withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                                    provider.getLyrics(
                                        context,
                                        mediaMetadata.id,
                                        cleanedTitle,
                                        artistsText,
                                        mediaMetadata.duration,
                                        mediaMetadata.album?.title,
                                    )
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.tag("LyricsHelper").w("${provider.name} threw: ${e.message}")
                                null
                            }

                            val rawLyrics = rawResult?.getOrNull() ?: return@async null
                            val filtered = LyricsUtils.filterLyricsCreditLines(rawLyrics)
                            val isExactIdSource = provider.name.equals("BetterLyrics", ignoreCase = true) ||
                                provider.name.equals("Zemer", ignoreCase = true)
                            val evaluation = LyricsQualityAnalyzer.evaluateCandidate(
                                rawLyrics = filtered,
                                providerName = provider.name,
                                providerPriorityIndex = index,
                                totalProviders = enabledProviders.size,
                                trackDurationSec = mediaMetadata.duration,
                                isExactVideoIdSource = isExactIdSource,
                            )
                            if (!evaluation.isValid) return@async null

                            ScoredProviderCandidate(
                                providerIndex = index,
                                providerName = provider.name,
                                lyrics = filtered,
                                evaluation = evaluation,
                            )
                        }
                    }

                    val candidates = mutableListOf<ScoredProviderCandidate>()
                    for ((idx, deferred) in deferreds.withIndex()) {
                        val candidate = try {
                            deferred.await()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            null
                        }
                        if (candidate != null) {
                            candidates.add(candidate)
                            if (candidate.evaluation.syncType == LyricsSyncType.RICH_SYNCED &&
                                idx == 0 &&
                                candidate.evaluation.wordSyncedLineCount >= 3
                            ) {
                                deferreds.drop(idx + 1).forEach { it.cancel() }
                                break
                            }
                        }
                    }

                    val best = candidates.maxByOrNull { it.evaluation.score }
                    if (best != null) {
                        Timber.tag("LyricsHelper").i(
                            "Selected ${best.providerName} (${best.evaluation.syncType}, score=${best.evaluation.score})"
                        )
                        val resolved = LyricsWithProvider(best.lyrics, best.providerName)
                        bestCache.put(cacheKey, resolved)
                        resolved
                    } else {
                        LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
                    }
                }
                synchronized(jobLock) {
                    currentBestDeferred = fetchDeferred
                }
                try {
                    fetchDeferred.await()
                } finally {
                    synchronized(jobLock) {
                        if (currentBestDeferred === fetchDeferred) {
                            currentBestDeferred = null
                        }
                    }
                }
            }
        }

        return result ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        val allProviders = context.dataStore.data
            .map { preferences -> resolveLyricsProviders(preferences) }
            .first()
        val enabledProviders = allProviders.filter { it.isEnabled(context) }
        val signature = enabledProviders.joinToString("|") { it.name }
        val cacheKey = "$mediaId:${songArtists.trim()}:${songTitle.trim()}:$signature"

        allCache.get(cacheKey)?.let { results ->
            results.forEach { callback(it) }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (_: Exception) {
            true
        }
        if (!isNetworkAvailable) return

        val cleanedTitle = LyricsUtils.cleanTitleForSearch(songTitle)
        val collected = mutableListOf<ScoredProviderCandidate>()
        val callbackMutex = Any()

        coroutineScope {
            val job = coroutineContext[Job]
            synchronized(jobLock) {
                currentAllLyricsJob?.cancel()
                currentAllLyricsJob = job
            }

            val otherProviders = enabledProviders.filter { it.name != "LyricsPlus" }
            val lyricsPlusProvider = enabledProviders.find { it.name == "LyricsPlus" }

            val otherJobs = otherProviders.mapIndexed { idx, provider ->
                launch {
                    try {
                        withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                            provider.getAllLyrics(context, mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                                val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                                val eval = LyricsQualityAnalyzer.evaluateCandidate(
                                    rawLyrics = filteredLyrics,
                                    providerName = provider.name,
                                    providerPriorityIndex = idx,
                                    totalProviders = enabledProviders.size,
                                    trackDurationSec = duration,
                                    isExactVideoIdSource = provider.name.equals("BetterLyrics", ignoreCase = true) ||
                                        provider.name.equals("Zemer", ignoreCase = true),
                                )
                                if (eval.isValid) {
                                    val result = LyricsResult(provider.name, filteredLyrics)
                                    synchronized(callbackMutex) {
                                        collected += ScoredProviderCandidate(idx, provider.name, filteredLyrics, eval)
                                        callback(result)
                                    }
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            otherJobs.forEach { it.join() }

            if (lyricsPlusProvider != null && collected.count { it.providerName != "LyricsPlus" } <= 2) {
                val lpIndex = enabledProviders.indexOfFirst { it.name == "LyricsPlus" }.coerceAtLeast(0)
                try {
                    withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                        lyricsPlusProvider.getAllLyrics(context, mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                            val eval = LyricsQualityAnalyzer.evaluateCandidate(
                                rawLyrics = filteredLyrics,
                                providerName = lyricsPlusProvider.name,
                                providerPriorityIndex = lpIndex,
                                totalProviders = enabledProviders.size,
                                trackDurationSec = duration,
                                isExactVideoIdSource = false,
                            )
                            if (eval.isValid) {
                                val result = LyricsResult(lyricsPlusProvider.name, filteredLyrics)
                                synchronized(callbackMutex) {
                                    collected += ScoredProviderCandidate(lpIndex, lyricsPlusProvider.name, filteredLyrics, eval)
                                    callback(result)
                                }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    reportException(e)
                }
            }
        }

        if (collected.isNotEmpty()) {
            val sortedResults = collected
                .sortedByDescending { it.evaluation.score }
                .map { LyricsResult(it.providerName, it.lyrics) }
            allCache.put(cacheKey, sortedResults)
        }
    }

    private fun resolveLyricsProviders(preferences: androidx.datastore.preferences.core.Preferences): List<LyricsProvider> {
        val providerOrder = preferences[LyricsProviderOrderKey].orEmpty()
        if (providerOrder.isNotBlank()) {
            return LyricsProviderRegistry.getOrderedProviders(providerOrder)
        }

        return LyricsProviderRegistry.getDefaultProviderOrder()
            .mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
    }

    companion object {
        private const val MAX_BEST_CACHE_SIZE = 16
        private const val MAX_ALL_CACHE_SIZE = 8
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)
