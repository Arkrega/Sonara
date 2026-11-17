/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class RegaColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val primaryAccent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderMuted: Color,
    val borderSubtle: Color,
    val border: Color,
    val ripple: Color,
    val glassBase: Color,
    val glassTint: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val glassModal: Color,
    val isDark: Boolean,
)

/**
 * MONOCHROME SONIC palette.
 * Pure black-to-white hierarchy. Album artwork is the only place colour lives.
 */
val RegaDarkColors = RegaColors(
    background = Color(0xFF08090A),
    surface = Color(0xFF101113),
    surfaceVariant = Color(0xFF17181B),
    surfaceElevated = Color(0xFF17181B),
    surfaceContainer = Color(0xFF0C0D0F),
    surfaceContainerHigh = Color(0xFF14161A),
    surfaceContainerHighest = Color(0xFF1B1D22),
    primaryAccent = Color(0xFF5E6AD2),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFF7F8F8),
    textSecondary = Color(0xFF8A8F98),
    textMuted = Color(0xFF62666D),
    borderMuted = Color(0x24FFFFFF),
    borderSubtle = Color(0x14FFFFFF),
    border = Color(0x24FFFFFF),
    ripple = Color(0x24FFFFFF),
    glassBase = Color(0xFF101113),
    glassTint = Color(0xFF101113),
    glassBorder = Color(0x24FFFFFF),
    glassHighlight = Color(0x14FFFFFF),
    glassModal = Color(0xFF14161A),
    isDark = true,
)

val RegaLightColors = RegaColors(
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F2F5),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF4F6F8),
    surfaceContainerHighest = Color(0xFFEBEEF2),
    primaryAccent = Color(0xFF5E6AD2),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF08090A),
    textSecondary = Color(0xFF5B616B),
    textMuted = Color(0xFF868C96),
    borderMuted = Color(0xFFE3E6EA),
    borderSubtle = Color(0xFFEDEFF2),
    border = Color(0xFFE3E6EA),
    ripple = Color(0x14000000),
    glassBase = Color(0xFFFFFFFF),
    glassTint = Color(0xFFFFFFFF),
    glassBorder = Color(0xFFE3E6EA),
    glassHighlight = Color(0x0A000000),
    glassModal = Color(0xFFFFFFFF),
    isDark = false,
)

object RegaColor {
    val Background = Color(0xFF08090A)
    val Surface = Color(0xFF101113)
    val SurfaceVariant = Color(0xFF17181B)
    val SurfaceElevated = Color(0xFF17181B)
    val SurfaceContainer = Color(0xFF0C0D0F)
    val SurfaceContainerHigh = Color(0xFF14161A)
    val SurfaceContainerHighest = Color(0xFF1B1D22)
    val PrimaryAccent = Color(0xFF5E6AD2)
    val OnAccent = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFF7F8F8)
    val TextSecondary = Color(0xFF8A8F98)
    val TextMuted = Color(0xFF62666D)
    val BorderMuted = Color(0x24FFFFFF)
    val BorderSubtle = Color(0x14FFFFFF)
    val Border = Color(0x24FFFFFF)
    val Ripple = Color(0x24FFFFFF)
    val GlassBase = Color(0xFF101113)
    val GlassTint = Color(0xFF101113)
    val GlassBorder = Color(0x24FFFFFF)
    val GlassHighlight = Color(0x14FFFFFF)
    val GlassModal = Color(0xFF14161A)
}

val LocalRegaColors = staticCompositionLocalOf { RegaDarkColors }
typealias SonaraColors = RegaColors
val SonaraDarkColors = RegaDarkColors
val SonaraLightColors = RegaLightColors
val SonaraColor = RegaColor
val LocalSonaraColors = LocalRegaColors
val LocalTabularTimeStyle = staticCompositionLocalOf { TabularTimeStyle }

val CinematicDarkColorScheme: ColorScheme = darkColorScheme(
    primary = RegaDarkColors.primaryAccent,
    onPrimary = RegaDarkColors.onAccent,
    primaryContainer = Color(0xFF23264A),
    onPrimaryContainer = Color(0xFFC9CDFF),
    secondary = RegaDarkColors.textSecondary,
    onSecondary = RegaDarkColors.background,
    secondaryContainer = Color(0xFF1B1D22),
    onSecondaryContainer = RegaDarkColors.textPrimary,
    tertiary = Color(0xFF4EA7FC),
    onTertiary = Color(0xFF04121F),
    background = RegaDarkColors.background,
    onBackground = RegaDarkColors.textPrimary,
    surface = RegaDarkColors.surface,
    onSurface = RegaDarkColors.textPrimary,
    surfaceVariant = RegaDarkColors.surfaceVariant,
    onSurfaceVariant = RegaDarkColors.textSecondary,
    surfaceContainer = RegaDarkColors.surfaceContainer,
    surfaceContainerHigh = RegaDarkColors.surfaceContainerHigh,
    surfaceContainerHighest = RegaDarkColors.surfaceContainerHighest,
    surfaceContainerLow = RegaDarkColors.surface,
    surfaceContainerLowest = RegaDarkColors.background,
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF7F8FA),
    inverseOnSurface = Color(0xFF08090A),
    inversePrimary = Color(0xFF4752B8),
    error = Color(0xFFE5484D),
    onError = Color.White,
)

val CinematicLightColorScheme: ColorScheme = lightColorScheme(
    primary = RegaLightColors.primaryAccent,
    onPrimary = RegaLightColors.onAccent,
    primaryContainer = Color(0xFFE7E9FB),
    onPrimaryContainer = Color(0xFF1F2470),
    secondary = RegaLightColors.textSecondary,
    onSecondary = RegaLightColors.background,
    secondaryContainer = RegaLightColors.surfaceVariant,
    onSecondaryContainer = RegaLightColors.textPrimary,
    tertiary = Color(0xFF2F7FE0),
    onTertiary = Color.White,
    background = RegaLightColors.background,
    onBackground = RegaLightColors.textPrimary,
    surface = RegaLightColors.surface,
    onSurface = RegaLightColors.textPrimary,
    surfaceVariant = RegaLightColors.surfaceVariant,
    onSurfaceVariant = RegaLightColors.textSecondary,
    surfaceContainer = RegaLightColors.surfaceContainer,
    surfaceContainerHigh = RegaLightColors.surfaceContainerHigh,
    surfaceContainerHighest = RegaLightColors.surfaceContainerHighest,
    surfaceContainerLow = RegaLightColors.surface,
    surfaceContainerLowest = RegaLightColors.background,
    outline = Color(0xFFCBD0D8),
    outlineVariant = Color(0xFFE3E6EA),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF17181B),
    inverseOnSurface = Color(0xFFF7F8F8),
    inversePrimary = Color(0xFF9BA3F0),
    error = Color(0xFFD93A3F),
    onError = Color.White,
)

val DefaultThemeColor = Color(0xFF5E6AD2)
val LegacyAmberThemeColor = Color(0xFFE5A93C)
val SystemDynamicThemeColor = Color.Transparent

@Composable
fun SonaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    MetrolistTheme(
        darkTheme = darkTheme,
        pureBlack = pureBlack,
        themeColor = themeColor,
        content = content,
    )
}

@Composable
fun RegaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) = SonaraTheme(
    darkTheme = darkTheme,
    pureBlack = pureBlack,
    themeColor = themeColor,
    content = content,
)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    // MONOCHROME SONIC is intentionally colour-blind: wallpaper seed colours and
    // dynamic colour are ignored so the interface always stays black, white and grey.
    val baseColorScheme = if (darkTheme) CinematicDarkColorScheme else CinematicLightColorScheme

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    val regaColors = when {
        darkTheme && pureBlack -> RegaDarkColors.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color.Black,
            glassBase = Color.Black,
            glassTint = Color.Black,
            glassModal = Color.Black,
        )
        darkTheme -> RegaDarkColors
        else -> RegaLightColors
    }

    CompositionLocalProvider(
        LocalRegaColors provides regaColors,
        LocalTabularTimeStyle provides TabularTimeStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RegaTypography,
            shapes = SonaraShapes,
            content = content,
        )
    }
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) {
        copy(
            surface = Color.Black,
            background = Color.Black,
        )
    } else {
        this
    }
