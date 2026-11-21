/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.music.ui.component.glass.LocalLiquidGlassEnabled
import com.sonara.music.ui.component.glass.RegaGlassMaterial
import com.sonara.music.ui.component.glass.RegaGlassStyle
import com.sonara.music.ui.component.glass.RegaLiquidGlass
import com.sonara.music.ui.component.glass.regaGlassSpecular
import com.sonara.music.ui.screens.Screens
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.ui.theme.SonaraLabelStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    val selectedIndex = remember(currentRoute, navigationItems) {
        navigationItems.indexOfFirst { screen ->
            isRouteSelected(currentRoute, screen.route, navigationItems)
        }
    }

    val barShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .windowInsetsPadding(
                NavigationBarDefaults.windowInsets.only(
                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                ),
            )
            .padding(
                horizontal = 14.dp,
                vertical = if (liquidGlassEnabled && slimNav) 0.dp else 6.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (liquidGlassEnabled && navigationItems.isNotEmpty()) {
            var lastValidIndex by remember { mutableIntStateOf(selectedIndex.coerceAtLeast(0)) }
            if (selectedIndex >= 0) {
                lastValidIndex = selectedIndex
            }
            val animatedIndex by animateFloatAsState(
                targetValue = lastValidIndex.coerceIn(0, navigationItems.lastIndex).toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 420f,
                ),
                label = "glassDockSlide",
            )
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (selectedIndex >= 0) 1f else 0f,
                animationSpec = tween(180),
                label = "glassDockIndicatorAlpha",
            )
            val regaColors = LocalRegaColors.current
            val density = LocalDensity.current

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (slimNav) 52.dp else 60.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.35f),
                    )
                    .regaGlassSpecular(
                        shape = CircleShape,
                        tintColor = regaColors.glassTint,
                        borderColor = regaColors.glassBorder,
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (indicatorAlpha > 0.01f) {
                    val tabWidth = maxWidth / navigationItems.size
                    val tabWidthPx = with(density) { tabWidth.toPx() }
                    RegaLiquidGlass(
                        modifier = Modifier
                            .offset { IntOffset((animatedIndex * tabWidthPx).roundToInt(), 0) }
                            .width(tabWidth)
                            .fillMaxHeight()
                            .graphicsLayer { alpha = indicatorAlpha },
                        shape = CircleShape,
                        borderColor = Color.White.copy(alpha = if (regaColors.isDark) 0.34f else 0.72f),
                        style = RegaGlassStyle.preset(
                            material = RegaGlassMaterial.Clear,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            borderRadius = 26.dp,
                            elevation = 0.dp,
                        ),
                    ) {}
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    navigationItems.forEach { screen ->
                        val isSelected = remember(currentRoute, screen.route) {
                            isRouteSelected(currentRoute, screen.route, navigationItems)
                        }
                        val currentIsSelected by rememberUpdatedState(isSelected)
                        val iconRes = remember(isSelected, screen) {
                            if (isSelected) screen.iconIdActive else screen.iconIdInactive
                        }

                        val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                        val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
                        val interactionSource = remember { MutableInteractionSource() }

                        if (isSearchItem || isHomeHoldItem) {
                            LaunchedEffect(interactionSource) {
                                var isLongClick = false
                                interactionSource.interactions.collectLatest { interaction ->
                                    when (interaction) {
                                        is PressInteraction.Press -> {
                                            isLongClick = false
                                            delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                            isLongClick = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                                        }
                                        is PressInteraction.Release -> {
                                            if (!isLongClick) currentOnItemClick(screen, currentIsSelected)
                                        }
                                        is PressInteraction.Cancel -> isLongClick = false
                                    }
                                }
                            }
                        }

                        val pressed by interactionSource.collectIsPressedAsState()
                        val cellScale by animateFloatAsState(
                            targetValue = if (pressed) 0.94f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 520f,
                            ),
                            label = "glassNavCellScale",
                        )
                        val contentTint by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(220),
                            label = "glassNavContentTint",
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = cellScale
                                    scaleY = cellScale
                                }
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(bounded = true),
                                    onClick = {
                                        if (!isSearchItem && !isHomeHoldItem) {
                                            currentOnItemClick(screen, currentIsSelected)
                                        }
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = stringResource(screen.titleId),
                                    tint = contentTint,
                                    modifier = Modifier.size(20.dp),
                                )
                                if (!slimNav) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(screen.titleId),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = SonaraLabelStyle.copy(fontSize = 9.sp, letterSpacing = 0.3.sp),
                                        color = contentTint,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return@Box
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (slimNav) 52.dp else 58.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = barShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.55f),
                )
                .clip(barShape)
                .background(
                    if (pureBlack) {
                        Color.Black.copy(alpha = 0.94f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    },
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = barShape,
                )
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationItems.forEach { screen ->
                val isSelected = remember(currentRoute, screen.route) {
                    isRouteSelected(currentRoute, screen.route, navigationItems)
                }
                val currentIsSelected by rememberUpdatedState(isSelected)
                val iconRes = remember(isSelected, screen) {
                    if (isSelected) screen.iconIdActive else screen.iconIdInactive
                }

                val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
                val interactionSource = remember { MutableInteractionSource() }

                if (isSearchItem || isHomeHoldItem) {
                    LaunchedEffect(interactionSource) {
                        var isLongClick = false
                        interactionSource.interactions.collectLatest { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> {
                                    isLongClick = false
                                    delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                    isLongClick = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                                }
                                is PressInteraction.Release -> {
                                    if (!isLongClick) currentOnItemClick(screen, currentIsSelected)
                                }
                                is PressInteraction.Cancel -> isLongClick = false
                            }
                        }
                    }
                }

                val pressed by interactionSource.collectIsPressedAsState()
                val cellScale by animateFloatAsState(
                    targetValue = if (pressed) 0.94f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 520f,
                    ),
                    label = "navCellScale",
                )
                val cellColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(220),
                    label = "navCellColor",
                )
                val contentTint by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(220),
                    label = "navContentTint",
                )
                val cellShape = RoundedCornerShape(14.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .graphicsLayer {
                            scaleX = cellScale
                            scaleY = cellScale
                        }
                        .clip(cellShape)
                        .background(cellColor)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                                    shape = cellShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true),
                            onClick = {
                                if (!isSearchItem && !isHomeHoldItem) {
                                    currentOnItemClick(screen, currentIsSelected)
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 3.dp)
                                .width(12.dp)
                                .height(2.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(1.dp),
                                ),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = stringResource(screen.titleId),
                            tint = contentTint,
                            modifier = Modifier.size(20.dp),
                        )
                        if (!slimNav) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = stringResource(screen.titleId),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = SonaraLabelStyle.copy(fontSize = 9.sp, letterSpacing = 0.3.sp),
                                color = contentTint,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val railItemShape = RoundedCornerShape(if (liquidGlassEnabled) 16.dp else 8.dp)

    val railItemsContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            navigationItems.forEach { screen ->
                val isSelected = remember(currentRoute, screen.route) {
                    isRouteSelected(currentRoute, screen.route, navigationItems)
                }
                val currentIsSelected by rememberUpdatedState(isSelected)
                val iconRes = remember(isSelected, screen) {
                    if (isSelected) screen.iconIdActive else screen.iconIdInactive
                }
                val interactionSource = remember { MutableInteractionSource() }
                val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null

                if (isSearchItem || isHomeHoldItem) {
                    LaunchedEffect(interactionSource) {
                        var isLongClick = false
                        interactionSource.interactions.collectLatest { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> {
                                    isLongClick = false
                                    delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                    isLongClick = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                                }
                                is PressInteraction.Release -> {
                                    if (!isLongClick) onItemClick(screen, currentIsSelected)
                                }
                                is PressInteraction.Cancel -> isLongClick = false
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(railItemShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = if (liquidGlassEnabled) 0.12f else 0.07f)
                            } else {
                                Color.Transparent
                            },
                            railItemShape,
                        )
                        .then(
                            if (isSelected && liquidGlassEnabled) {
                                Modifier.border(
                                    width = 0.75.dp,
                                    color = LocalRegaColors.current.glassBorder,
                                    shape = railItemShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true),
                            onClick = {
                                if (!isSearchItem && !isHomeHoldItem) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            },
                        )
                        .padding(vertical = 9.dp),
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = stringResource(screen.titleId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = SonaraLabelStyle.copy(fontSize = 8.5.sp, letterSpacing = 0.2.sp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }

    if (liquidGlassEnabled) {
        Box(
            modifier = modifier
                .width(76.dp)
                .fillMaxHeight()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Start))
                .padding(vertical = 8.dp, horizontal = 4.dp),
        ) {
            RegaLiquidGlass(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                borderColor = LocalRegaColors.current.glassBorder,
                style = RegaGlassStyle.preset(
                    material = RegaGlassMaterial.Clear,
                    tint = LocalRegaColors.current.glassTint,
                    borderRadius = 24.dp,
                    elevation = 8.dp,
                ),
                contentAlignment = Alignment.TopCenter,
            ) {
                railItemsContent()
            }
        }
    } else {
        Box(
            modifier = modifier
                .width(76.dp)
                .fillMaxHeight()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
                .windowInsetsPadding(NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Start)),
        ) {
            railItemsContent()
        }
    }
}
