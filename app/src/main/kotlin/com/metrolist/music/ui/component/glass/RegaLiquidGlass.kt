/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * MONOCHROME SONIC surface.
 *
 * The original implementation rendered a real-time AGSL refraction shader for
 * every glass panel. That cost a backdrop capture plus a per-frame GPU shader
 * pass on every card, capsule and dialog. The monochrome direction only needs
 * a surface colour and a hairline border, so this file keeps the original
 * public API (style presets, material tiers, call sites) but draws flat.
 */

package com.metrolist.music.ui.component.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.LocalRegaColors

enum class RegaGlassMaterial {
    Clear,
    Thin,
    Regular,
    Thick,
}

enum class RegaGlassAppearance {
    Auto,
    Light,
    Dark,
}

enum class RegaGlassQuality {
    Low,
    Medium,
    High,
}

@Immutable
data class RegaGlassStyle(
    val material: RegaGlassMaterial = RegaGlassMaterial.Regular,
    val tint: Color? = null,
    val blurRadius: Dp = 14.dp,
    val refractionStrength: Float = 14f,
    val bezelWidth: Dp = 26.dp,
    val thickness: Dp = 20.dp,
    val ior: Float = 1.5f,
    val chromaticAberration: Float = 0.14f,
    val saturation: Float = 1.45f,
    val borderRadius: Dp = 24.dp,
    val highlightIntensity: Float = 0.45f,
    val rimLighting: Float = 0.52f,
    val elevation: Dp = 8.dp,
    val appearance: RegaGlassAppearance = RegaGlassAppearance.Auto,
    val quality: RegaGlassQuality = RegaGlassQuality.High,
) {
    companion object {
        fun preset(
            material: RegaGlassMaterial = RegaGlassMaterial.Regular,
            tint: Color? = null,
            borderRadius: Dp = 24.dp,
            appearance: RegaGlassAppearance = RegaGlassAppearance.Auto,
            quality: RegaGlassQuality = RegaGlassQuality.High,
            highlightIntensity: Float = 0.45f,
            rimLighting: Float = 0.52f,
            elevation: Dp = 8.dp,
        ): RegaGlassStyle = RegaGlassStyle(
            material = material,
            tint = tint,
            borderRadius = borderRadius,
            appearance = appearance,
            quality = quality,
            highlightIntensity = highlightIntensity,
            rimLighting = rimLighting,
            elevation = elevation,
        )
    }
}

/**
 * Kept for compatibility with callers/diagnostics. Flat monochrome surfaces do
 * not need a backdrop or a GPU capability tier.
 */
fun resolveGlassRuntimeCapabilities(
    sdkInt: Int,
    hasBackdropSource: Boolean,
    quality: RegaGlassQuality,
): String = "MONOCHROME_FLAT"

@Composable
fun RegaLiquidGlass(
    modifier: Modifier = Modifier,
    style: RegaGlassStyle = RegaGlassStyle(),
    shape: Shape = RoundedCornerShape(style.borderRadius),
    borderColor: Color? = null,
    contentAlignment: Alignment = Alignment.Center,
    backdropState: RegaBackdropState? = LocalRegaBackdrop.current,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalRegaColors.current

    val surface = style.tint ?: when (style.material) {
        RegaGlassMaterial.Clear -> colors.surface.copy(alpha = 0.72f)
        RegaGlassMaterial.Thin -> colors.surface
        RegaGlassMaterial.Regular -> colors.surface
        RegaGlassMaterial.Thick -> colors.surfaceElevated
    }

    val hairline = borderColor ?: colors.border

    val topSurface = androidx.compose.ui.graphics.lerp(surface, Color.White, 0.035f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(topSurface, surface),
                ),
            )
            .border(width = 1.dp, color = hairline, shape = shape),
        contentAlignment = contentAlignment,
        content = content,
    )
}
