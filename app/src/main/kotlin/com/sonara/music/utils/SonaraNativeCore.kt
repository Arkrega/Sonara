package com.sonara.music.utils

object SonaraNativeCore {
    val isAvailable: Boolean =
        runCatching {
            System.loadLibrary("sonara_core")
            true
        }.getOrDefault(false)

    fun generateShazamSignature(samples: ByteArray): String? {
        if (!isAvailable) return null
        return runCatching { nativeGenerateShazamSignature(samples) }.getOrNull()
    }

    fun cleanTitleForSearch(title: String): String? {
        if (!isAvailable) return null
        return runCatching { nativeCleanTitleForSearch(title) }.getOrNull()
    }

    fun filterLyricsCreditLines(lyrics: String): String? {
        if (!isAvailable) return null
        return runCatching { nativeFilterLyricsCreditLines(lyrics) }.getOrNull()
    }

    @JvmStatic
    private external fun nativeGenerateShazamSignature(samples: ByteArray): String?

    @JvmStatic
    private external fun nativeCleanTitleForSearch(title: String): String?

    @JvmStatic
    private external fun nativeFilterLyricsCreditLines(lyrics: String): String?
}
