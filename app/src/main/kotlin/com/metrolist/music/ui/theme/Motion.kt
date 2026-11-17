/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * MONOCHROME SONIC motion language.
 *
 * Everything moves on the same rhythm: quick, weighty, and never bouncy.
 * Animations are driven by Compose's frame clock, so they render at the
 * display's native refresh rate (60/90/120/144 Hz) without extra work.
 */

package com.metrolist.music.ui.theme

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object SonaraMotion {
    /** Micro feedback: ripples, icon toggles, press states. */
    const val Instant = 90

    /** Small transitions: chip selection, colour fades. */
    const val Fast = 160

    /** Standard page/element transitions. */
    const val Medium = 260

    /** Large surface choreography (mini player → full player). */
    const val Slow = 420

    /** Blur-to-focus entrances. */
    const val Reveal = 520

    val Standard: Easing = FastOutSlowInEasing

    /** Decelerate hard: content arriving and settling. */
    val EmphasizedDecel: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Accelerate hard: content leaving. */
    val EmphasizedAccel: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Symmetric, used for shared movement. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> settle() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> snappy() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}

/**
 * Reveals content with a blur-to-focus entrance: the element starts soft,
 * slightly lower and scaled down, then resolves into place.
 *
 * Blur is only applied on API 31+ (RenderEffect); older devices get a pure
 * alpha/scale/slide reveal that costs nothing.
 */
@Composable
fun Modifier.blurToFocus(
    key: Any? = Unit,
    enabled: Boolean = true,
    initialBlur: Dp = 14.dp,
    initialOffset: Dp = 12.dp,
    initialScale: Float = 0.985f,
    durationMillis: Int = SonaraMotion.Reveal,
): Modifier {
    if (!enabled) return this

    var started by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { started = true }

    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis, easing = SonaraMotion.EmphasizedDecel),
        label = "blurToFocus",
    )

    return this.graphicsLayer {
        alpha = progress
        val scale = initialScale + (1f - initialScale) * progress
        scaleX = scale
        scaleY = scale
        translationY = (1f - progress) * initialOffset.toPx()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurPx = (1f - progress) * initialBlur.toPx()
            renderEffect = if (blurPx > 0.35f) {
                BlurEffect(blurPx, blurPx, TileMode.Clamp)
            } else {
                null
            }
        }
    }
}

/**
 * Subtle album-artwork feedback: a gentle lift on hover (mouse / stylus /
 * hover-capable devices) and a soft press-down on touch. Press events are
 * observed on the Initial pass without consuming them, so parent click
 * handlers keep working.
 */
fun Modifier.artworkInteraction(
    hoverScale: Float = 1.035f,
    pressScale: Float = 0.975f,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> pressScale
            hovered -> hoverScale
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "artworkScale",
    )

    this
        .hoverable(interactionSource = interactionSource)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (up != null) {
                    interactionSource.tryEmit(PressInteraction.Release(press))
                } else {
                    interactionSource.tryEmit(PressInteraction.Cancel(press))
                }
            }
        }
}
