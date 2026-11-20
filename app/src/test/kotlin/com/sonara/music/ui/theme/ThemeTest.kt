package com.sonara.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.TextUnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {

    private fun calculateContrastRatio(foreground: Color, background: Color): Float {
        val lum1 = foreground.luminance()
        val lum2 = background.luminance()
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    @Test
    fun defaultThemeColorIsLinearIndigo() {
        assertEquals(Color(0xFF5E6AD2), DefaultThemeColor)
        assertEquals(DefaultThemeColor, RegaColor.PrimaryAccent)
    }

    @Test
    fun regaColorMonochromeDarkPaletteValues() {
        assertEquals(Color(0xFF08090A), RegaColor.Background)
        assertEquals(Color(0xFF101113), RegaColor.Surface)
        assertEquals(Color(0xFF17181B), RegaColor.SurfaceVariant)
        assertEquals(Color(0xFF17181B), RegaColor.SurfaceElevated)
        assertEquals(Color(0xFF101113), RegaColor.GlassBase)
        assertEquals(Color(0xFFF7F8F8), RegaColor.TextPrimary)
        assertEquals(Color(0xFF8A8F98), RegaColor.TextSecondary)
        assertEquals(Color(0xFF62666D), RegaColor.TextMuted)
        assertEquals(Color(0x24FFFFFF), RegaColor.BorderMuted)
        assertEquals(Color(0x24FFFFFF), RegaColor.Border)
    }

    @Test
    fun surfacesAreOpaqueMonochrome() {
        assertEquals(1f, RegaDarkColors.glassTint.alpha)
        assertEquals(1f, RegaLightColors.glassTint.alpha)
        assertEquals(RegaDarkColors.surface, RegaDarkColors.glassTint)
        assertEquals(RegaDarkColors.border, RegaDarkColors.glassBorder)
        assertEquals(1f, RegaDarkColors.glassModal.alpha)
        assertEquals(1f, RegaLightColors.glassModal.alpha)
    }

    @Test
    fun regaLightPaletteValues() {
        assertEquals(Color(0xFFF7F8FA), RegaLightColors.background)
        assertEquals(Color(0xFFFFFFFF), RegaLightColors.surface)
        assertEquals(Color(0xFFFFFFFF), RegaLightColors.surfaceElevated)
        assertEquals(Color(0xFFFFFFFF), RegaLightColors.glassBase)
        assertEquals(Color(0xFF08090A), RegaLightColors.textPrimary)
        assertEquals(Color(0xFF5B616B), RegaLightColors.textSecondary)
        assertEquals(Color(0xFF868C96), RegaLightColors.textMuted)
        assertEquals(Color(0xFFE3E6EA), RegaLightColors.borderMuted)
        assertEquals(Color(0xFF5E6AD2), RegaLightColors.primaryAccent)
    }

    @Test
    fun cinematicDarkColorSchemeIntegration() {
        assertEquals(RegaColor.PrimaryAccent, CinematicDarkColorScheme.primary)
        assertEquals(RegaColor.OnAccent, CinematicDarkColorScheme.onPrimary)
        assertEquals(RegaColor.Background, CinematicDarkColorScheme.background)
        assertEquals(RegaColor.Surface, CinematicDarkColorScheme.surface)
        assertEquals(RegaColor.TextPrimary, CinematicDarkColorScheme.onSurface)
        assertEquals(RegaColor.SurfaceVariant, CinematicDarkColorScheme.surfaceVariant)
        assertEquals(RegaColor.TextSecondary, CinematicDarkColorScheme.onSurfaceVariant)
    }

    @Test
    fun cinematicLightColorSchemeIntegration() {
        assertEquals(RegaLightColors.primaryAccent, CinematicLightColorScheme.primary)
        assertEquals(RegaLightColors.onAccent, CinematicLightColorScheme.onPrimary)
        assertEquals(RegaLightColors.background, CinematicLightColorScheme.background)
        assertEquals(RegaLightColors.surface, CinematicLightColorScheme.surface)
        assertEquals(RegaLightColors.textPrimary, CinematicLightColorScheme.onSurface)
        assertEquals(RegaLightColors.surfaceVariant, CinematicLightColorScheme.surfaceVariant)
        assertEquals(RegaLightColors.textSecondary, CinematicLightColorScheme.onSurfaceVariant)
    }

    @Test
    fun colorContrastMeetsWcagStandards() {
        val primaryTextContrast = calculateContrastRatio(RegaColor.TextPrimary, RegaColor.Background)
        assertTrue(primaryTextContrast >= 7.0f)

        val secondaryTextContrast = calculateContrastRatio(RegaColor.TextSecondary, RegaColor.Background)
        assertTrue(secondaryTextContrast >= 4.5f)

        val accentContrast = calculateContrastRatio(RegaColor.PrimaryAccent, RegaColor.Background)
        assertTrue(accentContrast >= 3.0f)

        val onAccentContrast = calculateContrastRatio(RegaColor.OnAccent, RegaColor.PrimaryAccent)
        assertTrue(onAccentContrast >= 4.5f)
    }

    @Test
    fun typographyScalesUseSpUnits() {
        assertTrue(RegaTypography.displayLarge.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.displayMedium.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.displaySmall.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.headlineLarge.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.headlineMedium.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.headlineSmall.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.titleLarge.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.titleMedium.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.titleSmall.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.bodyLarge.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.bodyMedium.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.bodySmall.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.labelLarge.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.labelMedium.fontSize.type == TextUnitType.Sp)
        assertTrue(RegaTypography.labelSmall.fontSize.type == TextUnitType.Sp)

        assertTrue(TabularTimeStyle.fontSize.type == TextUnitType.Sp)
        assertEquals("tnum", TabularTimeStyle.fontFeatureSettings)
        assertTrue(ActiveLyricStyle.fontSize.type == TextUnitType.Sp)
        assertTrue(InactiveLyricStyle.fontSize.type == TextUnitType.Sp)
    }
}
