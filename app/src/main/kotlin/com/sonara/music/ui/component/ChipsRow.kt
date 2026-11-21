/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonara.music.R
import com.sonara.music.ui.component.glass.LocalLiquidGlassEnabled
import com.sonara.music.ui.component.glass.RegaGlassMaterial
import com.sonara.music.ui.component.glass.RegaGlassStyle
import com.sonara.music.ui.component.glass.RegaLiquidGlass
import com.sonara.music.ui.screens.OptionStats
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.ui.theme.SonaraLabelStyle

@Composable
private fun GlassFilterChipPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    RegaLiquidGlass(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .height(34.dp),
        shape = CircleShape,
        borderColor = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        } else {
            LocalRegaColors.current.glassBorder
        },
        style = RegaGlassStyle.preset(
            material = if (isSelected) RegaGlassMaterial.Thin else RegaGlassMaterial.Clear,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                LocalRegaColors.current.glassTint
            },
            borderRadius = 17.dp,
            elevation = 0.dp,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.uppercase(),
                style = SonaraLabelStyle.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val haptics = LocalHapticFeedback.current
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(24.dp))

        chips.forEach { (value, label) ->
            val isSelected = currentValue == value
            if (liquidGlassEnabled) {
                GlassFilterChipPill(
                    label = label,
                    isSelected = isSelected,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onValueUpdate(value)
                    },
                )
            } else {
                FilterChip(
                    label = {
                        Text(
                            text = label.uppercase(),
                            style = SonaraLabelStyle.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                    },
                    selected = isSelected,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = if (isSelected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onValueUpdate(value)
                    },
                    shape = RoundedCornerShape(6.dp),
                )
            }

            Spacer(Modifier.width(8.dp))
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun <Int> ChoiceChipsRow(
    chips: List<Pair<Int, String>>,
    options: List<Pair<OptionStats, String>>,
    selectedOption: OptionStats,
    onSelectionChange: (OptionStats) -> Unit,
    currentValue: Int,
    onValueUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val haptics = LocalHapticFeedback.current
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    var expandIconDegree by remember { mutableFloatStateOf(0f) }
    val rotationAnimation by animateFloatAsState(
        targetValue = expandIconDegree,
        animationSpec = tween(durationMillis = 400),
        label = "",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column {
            if (liquidGlassEnabled) {
                RegaLiquidGlass(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .height(34.dp),
                    shape = CircleShape,
                    borderColor = LocalRegaColors.current.glassBorder,
                    style = RegaGlassStyle.preset(
                        material = RegaGlassMaterial.Thin,
                        tint = containerColor,
                        borderRadius = 17.dp,
                        elevation = 0.dp,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                expanded = !expanded
                                expandIconDegree -= 180
                            }
                            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when (selectedOption) {
                                OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                                OptionStats.MONTHS -> stringResource(id = R.string.months)
                                OptionStats.YEARS -> stringResource(id = R.string.years)
                                OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                            },
                            style = SonaraLabelStyle.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                        )
                    }
                }
            } else {
                AssistChip(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        expanded = !expanded
                        expandIconDegree -= 180
                    },
                    label = {
                        Text(
                            text = when (selectedOption) {
                                OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                                OptionStats.MONTHS -> stringResource(id = R.string.months)
                                OptionStats.YEARS -> stringResource(id = R.string.years)
                                OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                            },
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                        )
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = containerColor,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandIn() + fadeIn(),
                exit = shrinkOut() + fadeOut(),
            ) {
                DropdownMenu(
                    modifier = Modifier.padding(start = 12.dp),
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                        expandIconDegree -= 180
                    },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.second) },
                            onClick = {
                                onSelectionChange(option.first)
                                expandIconDegree -= 180
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedOption,
            transitionSpec = { slideInHorizontally() + fadeIn() togetherWith slideOutHorizontally() + fadeOut() },
            label = "",
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                chips.forEach { (value, label) ->
                    val isSelected = currentValue == value
                    Spacer(Modifier.width(8.dp))

                    if (liquidGlassEnabled) {
                        GlassFilterChipPill(
                            label = label,
                            isSelected = isSelected,
                            onClick = { onValueUpdate(value) },
                        )
                    } else {
                        FilterChip(
                            label = {
                                Text(
                                    text = label.uppercase(),
                                    style = SonaraLabelStyle.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                )
                            },
                            selected = isSelected,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            border = if (isSelected) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            },
                            onClick = { onValueUpdate(value) },
                            shape = RoundedCornerShape(6.dp),
                        )
                    }
                }
            }
        }
    }
}
