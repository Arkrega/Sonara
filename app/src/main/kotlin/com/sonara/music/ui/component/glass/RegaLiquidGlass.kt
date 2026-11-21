/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.component.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.sonara.music.ui.theme.LocalRegaColors

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
    val blurRadius: Dp = 2.5.dp,
    val refractionStrength: Float = 14f,
    val bezelWidth: Dp = 26.dp,
    val thickness: Dp = 20.dp,
    val ior: Float = 1.5f,
    val chromaticAberration: Float = 0.22f,
    val saturation: Float = 1.45f,
    val borderRadius: Dp = 24.dp,
    val highlightIntensity: Float = 0.72f,
    val rimLighting: Float = 0.65f,
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
            highlightIntensity: Float = 0.72f,
            rimLighting: Float = 0.65f,
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

fun resolveGlassRuntimeCapabilities(
    sdkInt: Int,
    hasBackdropSource: Boolean,
    quality: RegaGlassQuality,
    liquidGlassEnabled: Boolean = true,
): String {
    if (!liquidGlassEnabled) return "MONOCHROME_FLAT"
    return when {
        sdkInt >= Build.VERSION_CODES.TIRAMISU && hasBackdropSource -> "AGSL_LIQUID_GLASS"
        sdkInt >= Build.VERSION_CODES.S && hasBackdropSource -> "RENDER_EFFECT_BLUR"
        else -> "FROSTED_FALLBACK"
    }
}

internal fun resolveLiquidGlassSurfaceTint(
    baseTint: Color,
    material: RegaGlassMaterial,
    isDark: Boolean,
    supportsBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
): Color {
    if (baseTint.alpha <= 0.01f) return Color.Transparent
    val crystalAlpha =
        when (material) {
            RegaGlassMaterial.Clear -> if (isDark) 0.06f else 0.10f
            RegaGlassMaterial.Thin -> if (isDark) 0.10f else 0.14f
            RegaGlassMaterial.Regular -> if (isDark) 0.14f else 0.20f
            RegaGlassMaterial.Thick -> if (isDark) 0.20f else 0.26f
        }
    val effectiveMaxAlpha = if (supportsBlur) crystalAlpha else (crystalAlpha + 0.22f).coerceAtMost(0.56f)
    return baseTint.copy(alpha = minOf(baseTint.alpha, effectiveMaxAlpha))
}

internal fun resolveLiquidGlassShape(
    shape: Shape,
    liquidGlassEnabled: Boolean,
): Shape {
    if (!liquidGlassEnabled) return shape
    return when (shape) {
        RegaPillShape -> CircleShape
        RegaSquircleShape -> RoundedCornerShape(16.dp)
        RegaCardShape -> RoundedCornerShape(20.dp)
        RegaDialogShape -> RoundedCornerShape(26.dp)
        else -> shape
    }
}

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
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current

    val baseSurface = style.tint ?: when (style.material) {
        RegaGlassMaterial.Clear -> colors.surface.copy(alpha = 0.72f)
        RegaGlassMaterial.Thin -> colors.surface
        RegaGlassMaterial.Regular -> colors.surface
        RegaGlassMaterial.Thick -> colors.surfaceElevated
    }

    val hairline = borderColor ?: colors.border

    if (!liquidGlassEnabled) {
        val topSurface = lerp(baseSurface, Color.White, 0.035f)
        Box(
            modifier = modifier
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(topSurface, baseSurface),
                    ),
                )
                .border(width = 1.dp, color = hairline, shape = shape),
            contentAlignment = contentAlignment,
            content = content,
        )
        return
    }

    val glassShape = resolveLiquidGlassShape(shape, liquidGlassEnabled = true)
    val resolvedTint = resolveLiquidGlassSurfaceTint(
        baseTint = baseSurface,
        material = style.material,
        isDark = colors.isDark,
    )
    val luminousBorder =
        if (borderColor != null && borderColor.alpha <= 0.01f) {
            Color.Transparent
        } else {
            val sheenColor = if (colors.isDark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.68f)
            if (borderColor != null) lerp(borderColor, sheenColor, 0.45f) else sheenColor
        }

    if (backdropState != null && backdropState.enabled) {
        val targetBlurDp = when (style.material) {
            RegaGlassMaterial.Clear -> 1.5.dp
            RegaGlassMaterial.Thin -> 2.dp
            RegaGlassMaterial.Regular -> 2.5.dp
            RegaGlassMaterial.Thick -> 3.5.dp
        }
        val targetRefractionHeightDp = when (style.material) {
            RegaGlassMaterial.Clear -> 12.dp
            RegaGlassMaterial.Thin -> 14.dp
            RegaGlassMaterial.Regular -> 16.dp
            RegaGlassMaterial.Thick -> 18.dp
        }
        val targetRefractionAmountDp = when (style.material) {
            RegaGlassMaterial.Clear -> 22.dp
            RegaGlassMaterial.Thin -> 26.dp
            RegaGlassMaterial.Regular -> 30.dp
            RegaGlassMaterial.Thick -> 34.dp
        }

        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdropState.kyantBackdrop,
                    shape = { glassShape },
                    effects = {
                        vibrancy()
                        blur(targetBlurDp.toPx())
                        val maxLens = (size.minDimension * 0.42f).coerceAtLeast(0f)
                        val safeHeight = minOf(targetRefractionHeightDp.toPx(), maxLens)
                        val safeAmount = minOf(targetRefractionAmountDp.toPx(), maxLens * 1.65f)
                        if (safeHeight >= 1f && safeAmount >= 1f) {
                            lens(
                                refractionHeight = safeHeight,
                                refractionAmount = safeAmount,
                                depthEffect = true,
                                chromaticAberration = style.chromaticAberration > 0f,
                            )
                        }
                    },
                    highlight = {
                        Highlight.Default.copy(
                            alpha = style.highlightIntensity.coerceIn(0.45f, 0.90f),
                        )
                    },
                    shadow = {
                        if (style.elevation > 0.dp) {
                            Shadow(
                                radius = style.elevation * 1.6f,
                                color = Color.Black.copy(alpha = if (colors.isDark) 0.24f else 0.12f),
                            )
                        } else {
                            null
                        }
                    },
                    onDrawSurface = {
                        if (resolvedTint.alpha > 0.005f) {
                            drawRect(resolvedTint)
                        }
                    },
                )
                .then(
                    if (luminousBorder.alpha > 0.005f) {
                        Modifier.border(width = 0.75.dp, color = luminousBorder, shape = glassShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = contentAlignment,
            content = content,
        )
    } else {
        val fallbackTint = resolveLiquidGlassSurfaceTint(
            baseTint = baseSurface,
            material = style.material,
            isDark = colors.isDark,
            supportsBlur = false,
        )
        val topHighlight = lerp(fallbackTint, Color.White, if (colors.isDark) 0.12f else 0.34f)
        Box(
            modifier = modifier
                .clip(glassShape)
                .background(
                    Brush.verticalGradient(
                        listOf(topHighlight, fallbackTint),
                    ),
                )
                .border(width = 0.75.dp, color = luminousBorder, shape = glassShape),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}
