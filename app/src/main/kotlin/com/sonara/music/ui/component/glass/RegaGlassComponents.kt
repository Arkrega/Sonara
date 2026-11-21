package com.sonara.music.ui.component.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonara.music.R
import com.sonara.music.ui.theme.LocalRegaColors
import com.styropyr0.prismal.components.PrismalGlassToggle
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.specular.PrismalSpecular

val RegaPillShape = RoundedCornerShape(2.dp)
val RegaSquircleShape = RoundedCornerShape(6.dp)
val RegaCardShape = RoundedCornerShape(8.dp)
val RegaDialogShape = RoundedCornerShape(10.dp)

@Composable
fun RegaGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RegaCardShape,
    tintColor: Color = LocalRegaColors.current.glassTint,
    borderColor: Color = LocalRegaColors.current.glassBorder,
    borderWidth: Dp = 1.dp,
    material: RegaGlassMaterial = RegaGlassMaterial.Regular,
    content: @Composable BoxScope.() -> Unit,
) {
    val radius = when (shape) {
        CircleShape -> 32.dp
        RegaSquircleShape -> 6.dp
        RegaDialogShape -> 10.dp
        else -> 20.dp
    }
    RegaLiquidGlass(
        modifier = modifier,
        shape = shape,
        borderColor = borderColor,
        style = RegaGlassStyle.preset(
            material = material,
            tint = tintColor,
            borderRadius = radius,
        ),
        contentAlignment = Alignment.TopStart,
        content = content,
    )
}

@Composable
fun RegaGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RegaCardShape,
    tintColor: Color = LocalRegaColors.current.glassTint,
    borderColor: Color = LocalRegaColors.current.glassBorder,
    borderWidth: Dp = 1.dp,
    material: RegaGlassMaterial = RegaGlassMaterial.Thin,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            onClick = onClick,
        )
    } else {
        Modifier
    }

    RegaLiquidGlass(
        modifier = modifier,
        shape = shape,
        borderColor = borderColor,
        style = RegaGlassStyle.preset(
            material = material,
            tint = tintColor,
            borderRadius = 20.dp,
        ),
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(clickModifier),
            content = content,
        )
    }
}

@Composable
fun RegaPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RegaPillShape,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentPadding: Dp = 20.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "primaryButtonScale",
    )

    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val resolvedShape = resolveLiquidGlassShape(shape, liquidGlassEnabled)

    if (liquidGlassEnabled) {
        val glassContentColor = if (enabled) containerColor else containerColor.copy(alpha = 0.45f)
        RegaLiquidGlass(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .defaultMinSize(minHeight = 50.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = resolvedShape,
            borderColor = if (enabled) containerColor.copy(alpha = 0.50f) else containerColor.copy(alpha = 0.20f),
            style = RegaGlassStyle.preset(
                material = RegaGlassMaterial.Thin,
                tint = if (enabled) containerColor.copy(alpha = 0.16f) else containerColor.copy(alpha = 0.08f),
                borderRadius = 25.dp,
                elevation = if (enabled) 8.dp else 0.dp,
            ),
        ) {
            Box(
                modifier = Modifier
                    .clip(resolvedShape)
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, color = glassContentColor.copy(alpha = 0.22f)),
                        onClick = onClick,
                    )
                    .padding(horizontal = contentPadding, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides glassContentColor) {
                    ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            content = content,
                        )
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.38f))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentColor.copy(alpha = 0.2f)),
                onClick = onClick,
            )
            .padding(horizontal = contentPadding, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) contentColor else contentColor.copy(alpha = 0.5f)) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

@Composable
fun RegaSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RegaSquircleShape,
    containerColor: Color = LocalRegaColors.current.glassTint,
    borderColor: Color = LocalRegaColors.current.glassBorder,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "secondaryButtonScale",
    )

    val resolvedShape = resolveLiquidGlassShape(shape, LocalLiquidGlassEnabled.current)

    RegaLiquidGlass(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = resolvedShape,
        borderColor = if (enabled) borderColor else borderColor.copy(alpha = 0.38f),
        style = RegaGlassStyle.preset(
            material = RegaGlassMaterial.Clear,
            tint = if (enabled) containerColor else containerColor.copy(alpha = 0.38f),
            borderRadius = 24.dp,
            elevation = if (enabled) 6.dp else 0.dp,
        ),
    ) {
        Box(
            modifier = Modifier
                .clip(resolvedShape)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = onClick,
                )
                .padding(horizontal = contentPadding, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides if (enabled) contentColor else contentColor.copy(alpha = 0.4f)) {
                ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
fun RegaIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    containerColor: Color = LocalRegaColors.current.glassTint,
    borderColor: Color = LocalRegaColors.current.glassBorder,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconButtonScale",
    )

    val resolvedShape = resolveLiquidGlassShape(shape, LocalLiquidGlassEnabled.current)

    RegaLiquidGlass(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = resolvedShape,
        borderColor = if (enabled) borderColor else borderColor.copy(alpha = 0.25f),
        style = RegaGlassStyle.preset(
            material = RegaGlassMaterial.Clear,
            tint = if (enabled) containerColor else containerColor.copy(alpha = 0.38f),
            borderRadius = size / 2,
            elevation = if (enabled) 6.dp else 0.dp,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(resolvedShape)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, radius = size / 2),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onClick()
                    },
                )
                .semantics {
                    if (contentDescription != null) {
                        this.contentDescription = contentDescription
                    }
                    this.role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides if (enabled) contentColor else contentColor.copy(alpha = 0.38f)) {
                content()
            }
        }
    }
}

@Composable
fun RegaPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 72.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    iconColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "playButtonScale",
    )
    val isDark = LocalRegaColors.current.isDark
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val resolvedDescription = contentDescription ?: stringResource(R.string.play_pause)

    if (liquidGlassEnabled) {
        val glassIconColor = if (enabled) containerColor else containerColor.copy(alpha = 0.4f)
        RegaLiquidGlass(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            borderColor = if (enabled) containerColor.copy(alpha = if (isDark) 0.42f else 0.55f) else containerColor.copy(alpha = 0.16f),
            style = RegaGlassStyle.preset(
                material = RegaGlassMaterial.Thin,
                tint = if (enabled) containerColor.copy(alpha = 0.14f) else containerColor.copy(alpha = 0.06f),
                borderRadius = size / 2,
                elevation = if (enabled) 10.dp else 0.dp,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false, radius = size / 2, color = glassIconColor.copy(alpha = 0.25f)),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onClick()
                        },
                    )
                    .semantics {
                        this.contentDescription = resolvedDescription
                        this.role = Role.Button
                    },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.6f) + fadeIn(tween(durationMillis = 110))) togetherWith
                            (scaleOut(targetScale = 0.6f) + fadeOut(tween(durationMillis = 90)))
                    },
                    label = "playPauseIcon",
                ) { playing ->
                    Icon(
                        painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = glassIconColor,
                        modifier = Modifier.size(size * 0.44f),
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = Color(0x665E6AD2),
                spotColor = Color(0x805E6AD2),
            )
            .clip(CircleShape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.38f))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2, color = iconColor.copy(alpha = 0.25f)),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                },
            )
            .semantics {
                this.contentDescription = resolvedDescription
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(initialScale = 0.6f) + fadeIn(tween(durationMillis = 110))) togetherWith
                    (scaleOut(targetScale = 0.6f) + fadeOut(tween(durationMillis = 90)))
            },
            label = "playPauseIcon",
        ) { playing ->
            Icon(
                painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = if (enabled) iconColor else iconColor.copy(alpha = 0.4f),
                modifier = Modifier.size(size * 0.44f),
            )
        }
    }
}

@Composable
fun RegaGlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 56.dp,
    shape: Shape = RegaSquircleShape,
    containerColor: Color = LocalRegaColors.current.glassTint,
    borderColor: Color = LocalRegaColors.current.glassBorder,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "glassFabScale",
    )

    val colors = LocalRegaColors.current
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val resolvedShape = if (liquidGlassEnabled) CircleShape else resolveLiquidGlassShape(shape, liquidGlassEnabled = false)
    val resolvedBorder =
        if (liquidGlassEnabled) {
            if (enabled) Color.White.copy(alpha = if (colors.isDark) 0.38f else 0.76f) else borderColor.copy(alpha = 0.20f)
        } else {
            if (enabled) borderColor else borderColor.copy(alpha = 0.25f)
        }

    RegaLiquidGlass(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = resolvedShape,
        borderColor = resolvedBorder,
        style = RegaGlassStyle.preset(
            material = RegaGlassMaterial.Clear,
            tint = if (enabled) containerColor else containerColor.copy(alpha = 0.38f),
            borderRadius = if (liquidGlassEnabled) size / 2 else 18.dp,
            highlightIntensity = if (liquidGlassEnabled) 0.86f else 0.72f,
            elevation = if (enabled) 10.dp else 0.dp,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(resolvedShape)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, radius = size / 2),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onClick()
                    },
                )
                .semantics {
                    if (contentDescription != null) {
                        this.contentDescription = contentDescription
                    }
                    this.role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides if (enabled) contentColor else contentColor.copy(alpha = 0.38f)) {
                content()
            }
        }
    }
}

@Composable
fun RegaGlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = null,
    icon: Painter? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "chipScale",
    )

    val liquidGlassEnabled = LocalLiquidGlassEnabled.current

    val containerColor by animateColorAsState(
        targetValue = when {
            selected && liquidGlassEnabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            selected -> MaterialTheme.colorScheme.primary
            else -> LocalRegaColors.current.glassTint
        },
        animationSpec = tween(durationMillis = 200),
        label = "chipContainer",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            selected && liquidGlassEnabled -> MaterialTheme.colorScheme.onSurface
            selected -> MaterialTheme.colorScheme.onPrimary
            else -> LocalRegaColors.current.textSecondary
        },
        animationSpec = tween(durationMillis = 200),
        label = "chipContent",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            selected && liquidGlassEnabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            selected -> Color.Transparent
            else -> LocalRegaColors.current.glassBorder
        },
        animationSpec = tween(durationMillis = 200),
        label = "chipBorder",
    )

    RegaLiquidGlass(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        borderColor = borderColor,
        style = RegaGlassStyle.preset(
            material = if (selected) RegaGlassMaterial.Thin else RegaGlassMaterial.Clear,
            tint = containerColor,
            borderRadius = 19.dp,
            elevation = if (selected) 4.dp else 0.dp,
        ),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = onClick,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (text != null) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
fun RegaGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val backdropState = LocalRegaBackdrop.current
    if (liquidGlassEnabled && backdropState != null && backdropState.enabled) {
        Box(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .alpha(if (enabled) 1f else 0.4f),
            contentAlignment = Alignment.Center,
        ) {
            PrismalGlassToggle(
                selected = { checked },
                onSelect = { newValue -> if (enabled) onCheckedChange(newValue) },
                backdrop = backdropState.prismalBackdrop,
            )
        }
        return
    }

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "switchThumbOffset",
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else LocalRegaColors.current.glassTint,
        animationSpec = tween(durationMillis = 180),
        label = "switchTrackColor",
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimary else LocalRegaColors.current.textPrimary,
        animationSpec = tween(durationMillis = 180),
        label = "switchThumbColor",
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) Color.Transparent else LocalRegaColors.current.glassBorder,
        animationSpec = tween(durationMillis = 180),
        label = "switchBorderColor",
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .width(52.dp)
            .height(30.dp)
            .regaGlassSpecular(
                shape = CircleShape,
                tintColor = trackColor,
                borderColor = borderColor,
            )
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}

@Composable
fun SonaraSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    if (!LocalLiquidGlassEnabled.current) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
        return
    }

    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val isDark = LocalRegaColors.current.isDark

    val knobOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "sonaraSwitchKnobOffset",
    )
    val knobScale by animateFloatAsState(
        targetValue = if (isPressed) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "sonaraSwitchKnobScale",
    )
    val trackFill by animateColorAsState(
        targetValue = if (checked) primary.copy(alpha = if (isDark) 0.50f else 0.62f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "sonaraSwitchTrackFill",
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) Color.White.copy(alpha = 0.97f) else Color.White.copy(alpha = if (isDark) 0.78f else 0.90f),
        animationSpec = tween(durationMillis = 200),
        label = "sonaraSwitchKnobColor",
    )
    val toggleModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = source,
                indication = null,
                onValueChange = { value ->
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onCheckedChange(value)
                },
            )
        } else {
            Modifier
        }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(width = 52.dp, height = 32.dp)
            .regaGlassSpecular(
                shape = CircleShape,
                tintColor = LocalRegaColors.current.glassTint,
                borderColor = LocalRegaColors.current.glassBorder,
            )
            .clip(CircleShape)
            .background(trackFill)
            .then(toggleModifier)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(24.dp)
                .graphicsLayer {
                    scaleX = knobScale
                    scaleY = knobScale
                }
                .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(knobColor)
                .border(0.75.dp, Color.White.copy(alpha = 0.72f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbContent != null) {
                CompositionLocalProvider(
                    LocalContentColor provides if (checked) primary else Color(0xFF3A3A3F),
                ) {
                    thumbContent()
                }
            }
        }
    }
}

fun Modifier.regaTopAppBarGlass(): Modifier = composed {
    if (!LocalLiquidGlassEnabled.current) return@composed this
    val regaColors = LocalRegaColors.current
    val backdropState = LocalRegaBackdrop.current
    val barShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    if (backdropState != null && backdropState.enabled) {
        this.regaGlassSpecular(
            shape = barShape,
            tintColor = regaColors.glassTint,
            borderColor = regaColors.glassBorder,
        )
    } else {
        val frostTop = regaColors.surface.copy(alpha = if (regaColors.isDark) 0.82f else 0.88f)
        val frostBottom = regaColors.surface.copy(alpha = if (regaColors.isDark) 0.66f else 0.74f)
        val rim = if (regaColors.isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.70f)
        this
            .clip(barShape)
            .background(Brush.verticalGradient(listOf(frostTop, frostBottom)))
            .border(0.85.dp, rim, barShape)
    }
}

@Composable
fun regaTopAppBarColors(base: TopAppBarColors): TopAppBarColors {
    if (LocalLiquidGlassEnabled.current) {
        return TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            navigationIconContentColor = base.navigationIconContentColor,
            titleContentColor = base.titleContentColor,
            actionIconContentColor = base.actionIconContentColor,
        )
    }
    return base
}

@Composable
fun regaTopAppBarColors(
    containerColor: Color = Color.Unspecified,
    scrolledContainerColor: Color = Color.Unspecified,
): TopAppBarColors {
    if (LocalLiquidGlassEnabled.current) {
        return TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    }
    return TopAppBarDefaults.topAppBarColors(
        containerColor = containerColor,
        scrolledContainerColor = scrolledContainerColor,
    )
}

fun Modifier.regaGlass(
    shape: Shape = RegaCardShape,
    tintColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
): Modifier = composed {
    val actualTint = if (tintColor != Color.Unspecified) tintColor else LocalRegaColors.current.glassTint
    val actualBorder = if (borderColor != Color.Unspecified) borderColor else LocalRegaColors.current.glassBorder
    this
        .clip(shape)
        .background(actualTint)
        .border(borderWidth, actualBorder, shape)
}

fun Modifier.regaGlassSpecular(
    shape: Shape = RegaCardShape,
    tintColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
): Modifier = composed {
    val colors = LocalRegaColors.current
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val liquidGlassPreset = LocalLiquidGlassPreset.current
    val backdropState = LocalRegaBackdrop.current
    val density = LocalDensity.current
    val actualTint = if (tintColor != Color.Unspecified) tintColor else colors.glassTint
    val actualBorder = if (borderColor != Color.Unspecified) borderColor else colors.glassBorder

    if (!liquidGlassEnabled) {
        return@composed this
            .clip(shape)
            .background(actualTint)
            .border(borderWidth, actualBorder, shape)
    }

    val glassShape = resolveLiquidGlassShape(shape, liquidGlassEnabled = true)
    val glassTint = resolveLiquidGlassSurfaceTint(
        baseTint = actualTint,
        material = RegaGlassMaterial.Clear,
        isDark = colors.isDark,
        preset = liquidGlassPreset,
    )
    val luminousBorder =
        if (actualBorder.alpha <= 0.01f) {
            Color.Transparent
        } else {
            val sheen = if (colors.isDark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.65f)
            lerp(actualBorder, sheen, 0.45f)
        }
    val specularRimBrush =
        if (luminousBorder.alpha > 0.005f) {
            Brush.linearGradient(
                colors = if (colors.isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.46f),
                        luminousBorder.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.26f),
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.88f),
                        luminousBorder.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.62f),
                    )
                },
            )
        } else {
            null
        }

    if (backdropState != null && backdropState.enabled) {
        this
            .drawPrismalGlass(
                backdrop = backdropState.prismalBackdrop,
                shape = { glassShape },
                specular = { PrismalSpecular.Default },
                depthShadow = null,
                effects = {
                    val maxLens = (size.minDimension * 0.42f).coerceAtLeast(0f)
                    val safeHeight = minOf(with(density) { 12.dp.toPx() }, maxLens)
                    val safeAmount = minOf(with(density) { 24.dp.toPx() }, maxLens * 1.6f)
                    applyPrismalGlassEffects(
                        density = density,
                        adaptiveLuminance = false,
                        luminance = if (colors.isDark) 0.25f else 0.78f,
                        blurRadiusPx = with(density) { resolveLiquidGlassBlurDp(RegaGlassMaterial.Thin, liquidGlassPreset).toPx() },
                        refractionHeightPx = safeHeight,
                        refractionAmountPx = safeAmount,
                        depthEffect = true,
                        chromaticAberration = 0.24f,
                        useVibrancy = true,
                    )
                },
                onDrawSurface = {
                    if (glassTint.alpha > 0.005f) {
                        drawRect(glassTint)
                    }
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (colors.isDark) 0.10f else 0.28f),
                                Color.Transparent,
                            ),
                        ),
                    )
                },
            )
            .then(
                if (specularRimBrush != null) {
                    Modifier.border(0.85.dp, specularRimBrush, glassShape)
                } else {
                    Modifier
                },
            )
    } else {
        val topSheen = lerp(glassTint, Color.White, if (colors.isDark) 0.16f else 0.58f)
        val bottomSheen = lerp(glassTint, Color.White, if (colors.isDark) 0.06f else 0.32f)
        this
            .clip(glassShape)
            .background(Brush.verticalGradient(listOf(topSheen, glassTint, bottomSheen)))
            .then(
                if (specularRimBrush != null) {
                    Modifier.border(0.85.dp, specularRimBrush, glassShape)
                } else {
                    Modifier
                },
            )
    }
}

typealias SonaraGlassMaterial = RegaGlassMaterial
typealias SonaraGlassAppearance = RegaGlassAppearance
typealias SonaraGlassQuality = RegaGlassQuality
typealias SonaraGlassStyle = RegaGlassStyle
val SonaraPillShape = RegaPillShape
val SonaraSquircleShape = RegaSquircleShape
val SonaraCardShape = RegaCardShape
val SonaraDialogShape = RegaDialogShape
