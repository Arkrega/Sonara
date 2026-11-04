package com.metrolist.music.utils

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdaterTest {
    @Test
    fun parsesRegaMusicAsset() {
        val assetsJson = JSONArray(
            """
            [
              {
                "name": "Rega-Music.apk",
                "browser_download_url": "https://github.com/arkrega/regamusic/releases/download/v1.0.0/Rega-Music.apk",
                "size": 52428800
              },
              {
                "name": "Rega-Music-with-Google-Cast.apk",
                "browser_download_url": "https://github.com/arkrega/regamusic/releases/download/v1.0.0/Rega-Music-with-Google-Cast.apk",
                "size": 55428800
              },
              {
                "name": "SHA256SUMS.txt",
                "browser_download_url": "https://github.com/arkrega/regamusic/releases/download/v1.0.0/SHA256SUMS.txt",
                "size": 256
              }
            ]
            """.trimIndent()
        )

        val assets = Updater.parseAssets(assetsJson)

        assertEquals(2, assets.size)
        val fossAsset = assets.first { it.variant == "foss" }
        assertEquals("Rega-Music.apk", fossAsset.name)
        assertEquals("universal", fossAsset.architecture)
        assertEquals("foss", fossAsset.variant)

        val gmsAsset = assets.first { it.variant == "gms" }
        assertEquals("Rega-Music-with-Google-Cast.apk", gmsAsset.name)
        assertEquals("universal", gmsAsset.architecture)
        assertEquals("gms", gmsAsset.variant)
    }

    @Test
    fun versionComparisonWorksCorrectly() {
        assertTrue(Updater.compareVersions("v1.0.1", "1.0.0") > 0)
        assertTrue(Updater.compareVersions("v1.1.0", "1.0.9") > 0)
        assertTrue(Updater.compareVersions("2.0.0", "v1.99.99") > 0)
        assertEquals(0, Updater.compareVersions("v1.0.0", "1.0.0"))
        assertTrue(Updater.compareVersions("1.0.0", "1.0.1") < 0)
        assertTrue(Updater.compareVersions("v1.0.0-rc1", "1.0.0") == 0)
    }

    @Test
    fun isUpdateAvailableChecks() {
        assertTrue(Updater.isUpdateAvailable("1.0.0", "1.0.1"))
        assertTrue(Updater.isUpdateAvailable("1.0.0", "v1.0.1"))
        assertFalse(Updater.isUpdateAvailable("1.0.0", "1.0.0"))
        assertFalse(Updater.isUpdateAvailable("1.0.1", "1.0.0"))
    }

    @Test
    fun kmpReleaseIsDisabled() {
        assertNull(Updater.parseKmpRelease("{}"))
    }
}
