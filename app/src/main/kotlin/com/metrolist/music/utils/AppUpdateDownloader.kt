/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progress: Float) : UpdateDownloadState
    data object VerifyingChecksum : UpdateDownloadState
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState
    data class PermissionRequired(val apkFile: File) : UpdateDownloadState
    data class Failed(val error: String) : UpdateDownloadState
}

object AppUpdateDownloader {
    private val httpClient = HttpClient()

    private fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun fetchExpectedSha256(sumsUrl: String, apkFileName: String): String? {
        return runCatching {
            val content = httpClient.get(sumsUrl).bodyAsText()
            content.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.contains(apkFileName, ignoreCase = true) }
                ?.split(Regex("\\s+"))
                ?.firstOrNull()
                ?.lowercase()
        }.getOrNull()
    }

    suspend fun downloadApk(
        context: Context,
        asset: ReleaseAsset,
        sha256SumsUrl: String?,
        onProgress: (UpdateDownloadState) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val targetFile = File(updatesDir, asset.name)

            if (targetFile.exists()) {
                targetFile.delete()
            }

            val connection = (URL(asset.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30000
                readTimeout = 30000
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Server returned HTTP $responseCode")
            }

            val totalSize = connection.contentLengthLong.takeIf { it > 0 } ?: asset.size
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val progress = if (totalSize > 0) downloadedBytes.toFloat() / totalSize else -1f
                        onProgress(UpdateDownloadState.Downloading(progress))
                    }
                    output.flush()
                }
            }

            if (sha256SumsUrl != null) {
                onProgress(UpdateDownloadState.VerifyingChecksum)
                val expectedSha = fetchExpectedSha256(sha256SumsUrl, asset.name)
                if (expectedSha != null) {
                    val actualSha = calculateFileSha256(targetFile)
                    if (!expectedSha.equals(actualSha, ignoreCase = true)) {
                        targetFile.delete()
                        throw IllegalStateException("Checksum verification failed: expected $expectedSha, got $actualSha")
                    }
                }
            }

            val finalState = if (canInstallPackages(context)) {
                UpdateDownloadState.ReadyToInstall(targetFile)
            } else {
                UpdateDownloadState.PermissionRequired(targetFile)
            }
            onProgress(finalState)
            targetFile
        }.onFailure {
            onProgress(UpdateDownloadState.Failed(it.message ?: "Download failed"))
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun triggerInstall(context: Context, apkFile: File): Result<Unit> {
        return runCatching {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        }
    }

    fun installDownloadedApk(context: Context, apkFile: File): Result<Boolean> {
        return runCatching {
            if (!canInstallPackages(context)) {
                openInstallPermissionSettings(context)
                false
            } else {
                triggerInstall(context, apkFile).getOrThrow()
                true
            }
        }
    }
}
