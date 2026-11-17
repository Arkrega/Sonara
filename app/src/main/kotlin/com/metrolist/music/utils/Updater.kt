/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val description: String,
    val releaseDate: String,
    val assets: List<ReleaseAsset>,
    val sha256SumsUrl: String? = null
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val architecture: String,
    val variant: String
)

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    private var cachedReleaseInfo: ReleaseInfo? = null
    private var cachedAllReleases: List<ReleaseInfo> = emptyList()

    private const val CHECK_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L

    private val GITHUB_API_BASE: String
        get() = "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}"

    fun parseVersionNumbers(version: String): List<Int> {
        val sanitized = version.trim().removePrefix("v").removePrefix("V").substringBefore("-").substringBefore("+")
        return sanitized.split(".").map { it.toIntOrNull() ?: 0 }
    }

    fun compareVersions(v1: String, v2: String): Int {
        val v1Parts = parseVersionNumbers(v1)
        val v2Parts = parseVersionNumbers(v2)
        val maxLength = maxOf(v1Parts.size, v2Parts.size)

        for (i in 0 until maxLength) {
            val part1 = v1Parts.getOrNull(i) ?: 0
            val part2 = v2Parts.getOrNull(i) ?: 0
            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        return 0
    }

    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return compareVersions(latestVersion, currentVersion) > 0
    }

    private fun getCurrentAppVariant(): Pair<String, String> {
        val architecture = BuildConfig.ARCHITECTURE
        val variant = if (BuildConfig.CAST_AVAILABLE) "gms" else "foss"
        return architecture to variant
    }

    fun parseAssets(assetsArray: JSONArray): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()

        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.optString("name", "")

            if (!name.endsWith(".apk")) continue

            val downloadUrl = asset.optString("browser_download_url", "")
            val size = asset.optLong("size", 0L)

            val (arch, variant) = when {
                name == "Sonara.apk" || name == "Rega-Music.apk" -> "universal" to "foss"
                name == "Sonara-with-Google-Cast.apk" || name == "Rega-Music-with-Google-Cast.apk" -> "universal" to "gms"
                name.startsWith("Sonara-") && name.endsWith("-with-Google-Cast.apk") -> {
                    name.removePrefix("Sonara-").removeSuffix("-with-Google-Cast.apk") to "gms"
                }
                name.startsWith("Sonara-") && name.endsWith(".apk") -> {
                    name.removePrefix("Sonara-").removeSuffix(".apk") to "foss"
                }
                name.startsWith("Rega-Music-") && name.endsWith("-with-Google-Cast.apk") -> {
                    name.removePrefix("Rega-Music-").removeSuffix("-with-Google-Cast.apk") to "gms"
                }
                name.startsWith("Rega-Music-") && name.endsWith(".apk") -> {
                    name.removePrefix("Rega-Music-").removeSuffix(".apk") to "foss"
                }
                name.startsWith("app-") && name.endsWith("-release.apk") -> {
                    name.removePrefix("app-").removeSuffix("-release.apk") to "foss"
                }
                name.startsWith("app-") && name.endsWith("-with-Google-Cast.apk") -> {
                    name.removePrefix("app-").removeSuffix("-with-Google-Cast.apk") to "gms"
                }
                else -> null to null
            }

            if (arch != null && variant != null) {
                assets.add(ReleaseAsset(name, downloadUrl, size, arch, variant))
            }
        }

        return assets
    }

    suspend fun getLatestRelease(forceRefresh: Boolean = false): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedReleaseInfo != null && !forceRefresh) {
                    return@runCatching cachedReleaseInfo!!
                }

                val response = client.get("$GITHUB_API_BASE/releases/latest").bodyAsText()
                val json = JSONObject(response)

                if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) {
                    throw IllegalStateException("No stable published release found")
                }

                val assetsJson = json.optJSONArray("assets") ?: JSONArray()
                val assets = parseAssets(assetsJson)
                val tagName = json.getString("tag_name")
                val shaAssetUrl = (0 until assetsJson.length())
                    .map { assetsJson.getJSONObject(it) }
                    .firstOrNull { it.optString("name") == "SHA256SUMS.txt" }
                    ?.optString("browser_download_url")

                val releaseInfo = ReleaseInfo(
                    tagName = tagName,
                    versionName = tagName.removePrefix("v").removePrefix("V"),
                    description = json.optString("body", ""),
                    releaseDate = json.optString("published_at", ""),
                    assets = assets,
                    sha256SumsUrl = shaAssetUrl
                )

                cachedReleaseInfo = releaseInfo
                lastCheckTime = System.currentTimeMillis()
                releaseInfo
            }
        }

    suspend fun getAllReleases(forceRefresh: Boolean = false): Result<List<ReleaseInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedAllReleases.isNotEmpty() && !forceRefresh) {
                    return@runCatching cachedAllReleases
                }

                val releases = mutableListOf<ReleaseInfo>()
                var page = 1
                var hasMore = true

                while (hasMore && page <= 10) {
                    val response = client.get("$GITHUB_API_BASE/releases?page=$page&per_page=30").bodyAsText()
                    val json = JSONArray(response)

                    if (json.length() == 0) {
                        hasMore = false
                        break
                    }

                    for (i in 0 until json.length()) {
                        val releaseObj = json.getJSONObject(i)
                        if (releaseObj.optBoolean("draft", false) || releaseObj.optBoolean("prerelease", false)) {
                            continue
                        }
                        val assetsJson = releaseObj.optJSONArray("assets") ?: JSONArray()
                        val tagName = releaseObj.optString("tag_name", "")
                        val shaAssetUrl = (0 until assetsJson.length())
                            .map { assetsJson.getJSONObject(it) }
                            .firstOrNull { it.optString("name") == "SHA256SUMS.txt" }
                            ?.optString("browser_download_url")

                        releases.add(
                            ReleaseInfo(
                                tagName = tagName,
                                versionName = tagName.removePrefix("v").removePrefix("V"),
                                description = releaseObj.optString("body", ""),
                                releaseDate = releaseObj.optString("published_at", ""),
                                assets = parseAssets(assetsJson),
                                sha256SumsUrl = shaAssetUrl
                            )
                        )
                    }

                    page++
                }

                cachedAllReleases = releases
                releases
            }
        }

    fun parseKmpRelease(response: String): ReleaseInfo? = null

    suspend fun getLatestKmpRelease(): Result<ReleaseInfo?> = Result.success(null)

    fun getDownloadUrlForCurrentVariant(releaseInfo: ReleaseInfo): String? {
        val (currentArch, currentVariant) = getCurrentAppVariant()

        return releaseInfo.assets
            .find { it.architecture == currentArch && it.variant == currentVariant }
            ?.downloadUrl
            ?: releaseInfo.assets.firstOrNull { it.variant == currentVariant }?.downloadUrl
            ?: releaseInfo.assets.firstOrNull()?.downloadUrl
    }

    fun getAssetForCurrentVariant(releaseInfo: ReleaseInfo): ReleaseAsset? {
        val (currentArch, currentVariant) = getCurrentAppVariant()
        return releaseInfo.assets.find { it.architecture == currentArch && it.variant == currentVariant }
            ?: releaseInfo.assets.firstOrNull { it.variant == currentVariant }
            ?: releaseInfo.assets.firstOrNull()
    }

    suspend fun checkForUpdate(forceRefresh: Boolean = false): Result<Pair<ReleaseInfo?, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val shouldFetch = forceRefresh ||
                    (System.currentTimeMillis() - lastCheckTime) > CHECK_INTERVAL_MILLIS

                if (!shouldFetch && cachedReleaseInfo != null) {
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.BASE_VERSION_NAME,
                        cachedReleaseInfo!!.versionName
                    )
                    return@runCatching cachedReleaseInfo!! to hasUpdate
                }

                val result = getLatestRelease(forceRefresh = true)
                if (result.isSuccess) {
                    val releaseInfo = result.getOrThrow()
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.BASE_VERSION_NAME,
                        releaseInfo.versionName
                    )
                    releaseInfo to hasUpdate
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
            }
        }

    fun getCachedLatestRelease(): ReleaseInfo? = cachedReleaseInfo
}
