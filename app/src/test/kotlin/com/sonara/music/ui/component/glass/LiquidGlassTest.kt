package com.sonara.music.ui.component.glass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassTest {

    @Test
    fun `runtime capabilities reflect liquid glass toggle and android sdk tier`() {
        assertEquals(
            "MONOCHROME_FLAT",
            resolveGlassRuntimeCapabilities(
                sdkInt = 35,
                hasBackdropSource = true,
                quality = RegaGlassQuality.High,
                liquidGlassEnabled = false,
            ),
        )
        assertEquals(
            "AGSL_LIQUID_GLASS",
            resolveGlassRuntimeCapabilities(
                sdkInt = 33,
                hasBackdropSource = true,
                quality = RegaGlassQuality.High,
                liquidGlassEnabled = true,
            ),
        )
        assertEquals(
            "RENDER_EFFECT_BLUR",
            resolveGlassRuntimeCapabilities(
                sdkInt = 31,
                hasBackdropSource = true,
                quality = RegaGlassQuality.High,
                liquidGlassEnabled = true,
            ),
        )
        assertEquals(
            "FROSTED_FALLBACK",
            resolveGlassRuntimeCapabilities(
                sdkInt = 30,
                hasBackdropSource = true,
                quality = RegaGlassQuality.High,
                liquidGlassEnabled = true,
            ),
        )
        assertEquals(
            "FROSTED_FALLBACK",
            resolveGlassRuntimeCapabilities(
                sdkInt = 35,
                hasBackdropSource = false,
                quality = RegaGlassQuality.High,
                liquidGlassEnabled = true,
            ),
        )
    }

    @Test
    fun `liquid glass surface tint stays crystal clear and preserves lower alpha`() {
        val opaqueDark = Color(0xFF101113)
        val clearDark = resolveLiquidGlassSurfaceTint(opaqueDark, RegaGlassMaterial.Clear, isDark = true, supportsBlur = true)
        val thinDark = resolveLiquidGlassSurfaceTint(opaqueDark, RegaGlassMaterial.Thin, isDark = true, supportsBlur = true)
        val regularDark = resolveLiquidGlassSurfaceTint(opaqueDark, RegaGlassMaterial.Regular, isDark = true, supportsBlur = true)
        val thickDark = resolveLiquidGlassSurfaceTint(opaqueDark, RegaGlassMaterial.Thick, isDark = true, supportsBlur = true)

        assertTrue(clearDark.alpha <= 0.08f)
        assertTrue(thinDark.alpha <= 0.12f)
        assertTrue(regularDark.alpha <= 0.16f)
        assertTrue(thickDark.alpha <= 0.22f)
        assertTrue(clearDark.alpha < thinDark.alpha)
        assertTrue(thinDark.alpha < regularDark.alpha)
        assertTrue(regularDark.alpha < thickDark.alpha)

        val fallbackDark = resolveLiquidGlassSurfaceTint(opaqueDark, RegaGlassMaterial.Regular, isDark = true, supportsBlur = false)
        assertTrue(fallbackDark.alpha > regularDark.alpha)
        assertTrue(fallbackDark.alpha <= 0.56f)

        val subtle = Color.White.copy(alpha = 0.04f)
        val preserved = resolveLiquidGlassSurfaceTint(subtle, RegaGlassMaterial.Clear, isDark = true, supportsBlur = true)
        assertEquals(subtle.alpha, preserved.alpha, 0.0001f)

        val transparent = resolveLiquidGlassSurfaceTint(Color.Transparent, RegaGlassMaterial.Thick, isDark = false, supportsBlur = true)
        assertEquals(0f, transparent.alpha, 0.001f)
    }

    @Test
    fun `resolveLiquidGlassShape upgrades sharp shapes when enabled and preserves them when disabled`() {
        assertEquals(CircleShape, resolveLiquidGlassShape(RegaPillShape, liquidGlassEnabled = true))
        assertEquals(RoundedCornerShape(16.dp), resolveLiquidGlassShape(RegaSquircleShape, liquidGlassEnabled = true))
        assertEquals(RoundedCornerShape(20.dp), resolveLiquidGlassShape(RegaCardShape, liquidGlassEnabled = true))
        assertEquals(RoundedCornerShape(26.dp), resolveLiquidGlassShape(RegaDialogShape, liquidGlassEnabled = true))

        assertEquals(RegaPillShape, resolveLiquidGlassShape(RegaPillShape, liquidGlassEnabled = false))
        assertEquals(RegaSquircleShape, resolveLiquidGlassShape(RegaSquircleShape, liquidGlassEnabled = false))
        assertEquals(RegaCardShape, resolveLiquidGlassShape(RegaCardShape, liquidGlassEnabled = false))
        assertEquals(RegaDialogShape, resolveLiquidGlassShape(RegaDialogShape, liquidGlassEnabled = false))
    }
}
