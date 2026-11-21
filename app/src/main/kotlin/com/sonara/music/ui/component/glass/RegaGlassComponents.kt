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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
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

    RegaLiquidGlass(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        borderColor = if (enabled) borderColor else borderColor.copy(alpha = 0.38f),
        style = RegaGlassStyle.preset(
            material = RegaGlassMaterial.Thin,
            tint = if (enabled) containerColor else containerColor.copy(alpha = 0.38f),
            borderRadius = 24.dp,
            elevation = if (enabled) 6.dp else 0.dp,
        ),
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
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

    RegaLiquidGlass(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
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
                .clip(shape)
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
    val resolvedDescription = contentDescription ?: stringResource(R.string.play_pause)

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

    RegaLiquidGlass(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        borderColor = if (enabled) borderColor else borderColor.copy(alpha = 0.25f),
        style = RegaGlassStyle.preset(
            material = RegaGlassMaterial.Regular,
            tint = if (enabled) containerColor else containerColor.copy(alpha = 0.38f),
            borderRadius = 18.dp,
            elevation = if (enabled) 10.dp else 0.dp,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
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

    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else LocalRegaColors.current.glassTint,
        animationSpec = tween(durationMillis = 200),
        label = "chipContainer",
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else LocalRegaColors.current.textSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "chipContent",
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else LocalRegaColors.current.glassBorder,
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
            material = if (selected) RegaGlassMaterial.Thick else RegaGlassMaterial.Clear,
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

    val glassTint = resolveLiquidGlassSurfaceTint(
        baseTint = actualTint,
        material = RegaGlassMaterial.Thin,
        isDark = colors.isDark,
    )
    val luminousBorder =
        if (actualBorder.alpha <= 0.01f) {
            Color.Transparent
        } else {
            val sheen = if (colors.isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.56f)
            lerp(actualBorder, sheen, 0.35f)
        }

    if (backdropState != null && backdropState.enabled) {
        this
            .drawPrismalGlass(
                backdrop = backdropState.prismalBackdrop,
                shape = { shape },
                specular = { PrismalSpecular.Default },
                depthShadow = null,
                effects = {
                    val maxLens = (size.minDimension * 0.42f).coerceAtLeast(0f)
                    val safeHeight = minOf(with(density) { 10.dp.toPx() }, maxLens)
                    val safeAmount = minOf(with(density) { 18.dp.toPx() }, maxLens * 1.5f)
                    applyPrismalGlassEffects(
                        density = density,
                        adaptiveLuminance = false,
                        luminance = if (colors.isDark) 0.25f else 0.78f,
                        blurRadiusPx = with(density) { 12.dp.toPx() },
                        refractionHeightPx = safeHeight,
                        refractionAmountPx = safeAmount,
                        depthEffect = true,
                        chromaticAberration = 0.18f,
                        useVibrancy = true,
                    )
                },
                onDrawSurface = {
                    if (glassTint.alpha > 0.005f) {
                        drawRect(glassTint)
                    }
                },
            )
            .then(
                if (luminousBorder.alpha > 0.005f) {
                    Modifier.border(0.75.dp, luminousBorder, shape)
                } else {
                    Modifier
                },
            )
    } else {
        val fallbackTint = resolveLiquidGlassSurfaceTint(
            baseTint = actualTint,
            material = RegaGlassMaterial.Thin,
            isDark = colors.isDark,
            supportsBlur = false,
        )
        val topHighlight = lerp(fallbackTint, Color.White, if (colors.isDark) 0.14f else 0.30f)
        this
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topHighlight, fallbackTint)))
            .border(0.75.dp, luminousBorder, shape)
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
