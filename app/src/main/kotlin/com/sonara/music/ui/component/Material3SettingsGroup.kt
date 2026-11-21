/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.music.ui.component.glass.LocalLiquidGlassEnabled
import com.sonara.music.ui.component.glass.RegaGlassMaterial
import com.sonara.music.ui.component.glass.RegaGlassStyle
import com.sonara.music.ui.component.glass.RegaLiquidGlass
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.ui.theme.SonaraHairline
import com.sonara.music.ui.theme.SonaraLabelStyle
import com.sonara.music.ui.theme.SonaraSpacing

@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    useLowContrast: Boolean = false
) {
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        title?.let {
            Text(
                text = it.uppercase(),
                style = SonaraLabelStyle.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SonaraSpacing.xs, top = SonaraSpacing.xl, bottom = SonaraSpacing.sm)
            )
        }

        val groupContent: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    if (index > 0) {
                        SonaraHairline(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Material3SettingsItemRow(item = item, liquidGlassEnabled = liquidGlassEnabled)
                }
            }
        }

        if (liquidGlassEnabled) {
            RegaLiquidGlass(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                borderColor = LocalRegaColors.current.glassBorder,
                style = RegaGlassStyle.preset(
                    material = RegaGlassMaterial.Clear,
                    tint = MaterialTheme.colorScheme.surface,
                    borderRadius = 18.dp,
                    elevation = 0.dp,
                ),
                contentAlignment = Alignment.TopStart,
            ) {
                groupContent()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clip(RoundedCornerShape(10.dp))
            ) {
                groupContent()
            }
        }
    }
}

@Composable
private fun Material3SettingsItemRow(
    item: Material3SettingsItem,
    liquidGlassEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.enabled && item.onClick != null,
                onClick = { item.onClick?.invoke() }
            )
            .padding(horizontal = SonaraSpacing.lg, vertical = SonaraSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.leadingContent != null) {
            item.leadingContent.invoke()
            Spacer(modifier = Modifier.width(SonaraSpacing.md))
        } else if (item.icon != null) {
            val iconShape = if (liquidGlassEnabled) CircleShape else RoundedCornerShape(8.dp)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (liquidGlassEnabled) 0.08f else 0.06f),
                        shape = iconShape
                    )
                    .then(
                        if (liquidGlassEnabled) {
                            Modifier.border(
                                width = 0.75.dp,
                                color = LocalRegaColors.current.glassBorder,
                                shape = iconShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center
            ) {
                val tint = when {
                    !item.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    item.isHighlighted -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
                if (item.showBadge) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = MaterialTheme.colorScheme.primary)
                        }
                    ) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(SonaraSpacing.md))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    color = if (!item.enabled)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                item.title()
            }

            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodySmall.copy(
                        color = if (!item.enabled)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    desc()
                }
            }
        }

        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(SonaraSpacing.md))
            trailing()
        }
    }
}

/**
 * Data class for Material 3 settings item
 */
data class Material3SettingsItem(
    val icon: Painter? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)
