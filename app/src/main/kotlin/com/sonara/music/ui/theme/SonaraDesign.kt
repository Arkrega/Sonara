/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SONARA — Linear-style dark UI, tactile surfaces.
 *
 * Compact type, hairline borders, layered greys, and one indigo accent used
 * only where it carries meaning (selection, progress, focus). Artwork remains
 * the only place where arbitrary colour is allowed.
 */
object SonaraSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
    val gutter: Dp = 16.dp
}

/** 6 / 8 / 10 / 12 — Linear's rounding scale, not pills. */
val SonaraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

object SonaraRadius {
    val none: Dp = 0.dp
    val sm: Dp = 6.dp
    val md: Dp = 8.dp
    val lg: Dp = 10.dp
    val xl: Dp = 12.dp
    val pill: Dp = 100.dp
}

/** 10–11px uppercase label, 0.06em tracking. */
val SonaraLabelStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    lineHeight = 13.sp,
    letterSpacing = 0.6.sp,
)

/** Compact display used for the wordmark and hero numbers. */
val SonaraDisplayStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.8).sp,
)

/** Faint indigo/blue wash used behind the player and the intro. */
val SonaraGlowBrush: Brush = Brush.radialGradient(
    0.00f to Color(0x4D5E6AD2),
    0.45f to Color(0x1A4EA7FC),
    1.00f to Color(0x0008090A),
)

val SonaraPanelBrush: Brush = Brush.verticalGradient(
    0f to Color(0xFF14161A),
    1f to Color(0xFF101113),
)

@Composable
fun SonaraTagline(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = "YOUR SOUND. YOUR WORLD",
        modifier = modifier,
        style = SonaraLabelStyle,
        color = color,
        maxLines = 1,
    )
}

@Composable
fun SonaraHairline(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    thickness: Dp = 1.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}

/** Flat surface with a 1px border and a soft top highlight. */
fun Modifier.sonaraPanel(
    shape: Shape = RoundedCornerShape(10.dp),
    color: Color = Color(0xFF101113),
    borderColor: Color = Color(0x24FFFFFF),
): Modifier = this
    .background(color, shape)
    .border(1.dp, borderColor, shape)

@Composable
fun SonaraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    index: String? = null,
    label: String? = null,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SonaraSpacing.gutter, end = SonaraSpacing.gutter, top = SonaraSpacing.lg, bottom = SonaraSpacing.sm),
        ) {
            if (index != null) {
                Text(
                    text = index,
                    style = SonaraLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(SonaraSpacing.sm))
            }
            Column(Modifier.weight(1f)) {
                if (label != null) {
                    Text(
                        text = label.uppercase(),
                        style = SonaraLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) {
                trailing()
            } else if (onAction != null && actionLabel != null) {
                SonaraLink(text = actionLabel, onClick = onAction)
            }
        }
    }
}

/** Small text action in accent blue. */
@Composable
fun SonaraLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Text(
        text = text,
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        style = SonaraLabelStyle.copy(letterSpacing = 0.3.sp),
        color = if (enabled) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        maxLines = 1,
    )
}

@Composable
fun SonaraMarker(
    modifier: Modifier = Modifier,
    size: Dp = 5.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier
            .width(size)
            .height(size)
            .background(color, CircleShape),
    )
}

@Composable
fun SonaraDot(
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier
            .width(size)
            .height(size)
            .background(color, CircleShape),
    )
}

@Composable
fun SonaraIndex(
    number: Int,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Text(
        text = number.toString().padStart(2, '0'),
        modifier = modifier.width(24.dp),
        style = SonaraLabelStyle.copy(fontSize = 9.sp),
        color = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        },
    )
}

@Composable
fun SonaraStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label.uppercase(),
            style = SonaraLabelStyle.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun Modifier.sonaraBorder(
    color: Color,
    width: Dp = 1.dp,
) = this.border(width, color, RoundedCornerShape(10.dp))
