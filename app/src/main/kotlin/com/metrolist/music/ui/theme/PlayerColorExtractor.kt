/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.palette.graphics.Palette

/**
 * Linear-style ambient tones.
 *
 * The cover still drives the brightness of the room, but the hue is clamped to
 * SONARA's indigo/blue wash so the player never turns into a colour wash.
 */
object PlayerColorExtractor {
    fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int,
    ): List<Color> {
        val swatches = palette.swatches.ifEmpty { null }
        val artworkLuminance = swatches
            ?.maxByOrNull { it.population }
            ?.let { Color(it.rgb).luminance() }
            ?: Color(fallbackColor).luminance()

        // Keep the ambient light low so white text always wins.
        val level = artworkLuminance.coerceIn(0f, 1f)
        val primary = Color(
            red = 0.075f + level * 0.055f,
            green = 0.082f + level * 0.055f,
            blue = 0.150f + level * 0.085f,
        )
        val secondary = Color(
            red = 0.040f,
            green = 0.045f,
            blue = 0.075f,
        )
        val tertiary = Color(
            red = 0.030f + level * 0.020f,
            green = 0.065f + level * 0.035f,
            blue = 0.105f + level * 0.055f,
        )

        return listOf(primary, secondary, tertiary)
    }
}
