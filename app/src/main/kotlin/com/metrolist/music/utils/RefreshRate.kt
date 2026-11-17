/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.app.Activity
import android.os.Build

/**
 * Applies the user's display refresh preference.
 *
 * Compose renders every animation from the frame clock, so the interface can
 * follow 90/120/144 Hz panels. Some OEM skins still pin apps to 60 Hz unless the
 * window explicitly asks for a mode, so we request the fastest mode the display
 * advertises. Panels with variable refresh still scale down when idle.
 */
fun Activity.applyRefreshRatePreference(highRefreshRate: Boolean) {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
    } ?: return

    val modes = runCatching { display.supportedModes?.toList() }.getOrNull() ?: return
    if (modes.isEmpty()) return

    val targetMode = if (highRefreshRate) {
        modes.maxByOrNull { it.refreshRate }
    } else {
        modes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }
    } ?: return

    val attributes = window.attributes
    if (attributes.preferredDisplayModeId == targetMode.modeId) return
    attributes.preferredDisplayModeId = targetMode.modeId
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        attributes.preferredRefreshRate = targetMode.refreshRate
    }
    window.attributes = attributes
}
