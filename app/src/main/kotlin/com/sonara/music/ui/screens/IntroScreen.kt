/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sonara.music.ui.screens

import android.app.Activity
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sonara.music.R
import com.sonara.music.ui.theme.SonaraLabelStyle
import com.sonara.music.ui.theme.SonaraSpacing
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val STEP_INTRO = 0
private const val STEP_WELCOME = 1
private const val STEP_LOGIN = 2

private const val INTRO_DURATION_MS = 3_200
private const val INTRO_SECONDS = 3.2f

private val IntroBlack = Color(0xFF050505)
private val IntroWhite = Color(0xFFFFFFFF)
private val IntroMuted = Color(0xFFA0A0A0)
private val IntroDim = Color(0xFF707070)

/**
 * Native first-launch flow: title card, account choice, and the existing
 * YouTube Music WebView login rendered in place. Kept as a single full-screen
 * gate so Home can never flash before onboarding resolves.
 */
@Composable
fun SonaraOnboarding(
    navController: NavController,
    needsWelcome: Boolean,
    startAtWelcome: Boolean = false,
    onIntroFinished: () -> Unit,
    onGuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable {
        mutableIntStateOf(if (startAtWelcome) STEP_WELCOME else STEP_INTRO)
    }
    val activity = LocalContext.current as? Activity

    BackHandler(enabled = true) {
        when (step) {
            STEP_LOGIN -> step = STEP_WELCOME
            else -> activity?.moveTaskToBack(true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IntroBlack),
    ) {
        Crossfade(
            targetState = step,
            animationSpec = tween(durationMillis = 420),
            label = "onboardingStep",
        ) { current ->
            when (current) {
                STEP_INTRO -> SonaraIntro(
                    onFinished = {
                        onIntroFinished()
                        if (needsWelcome) step = STEP_WELCOME
                    },
                )
                STEP_WELCOME -> SonaraWelcome(
                    onGoogle = { step = STEP_LOGIN },
                    onGuest = onGuest,
                )
                else -> LoginScreen(
                    navController = navController,
                    onClose = { step = STEP_WELCOME },
                )
            }
        }
    }
}

/**
 * 3.2 second title card. One linear clock drives every element so the timeline
 * stays in sync; the clock is the only thing that decides when the intro ends.
 */
@Composable
private fun SonaraIntro(onFinished: () -> Unit) {
    // Wall-clock progress instead of an Animatable: system animations can be
    // disabled (animator scale 0), and the intro still has to last its full time.
    var progress by remember { mutableFloatStateOf(0f) }
    val reducedMotion = rememberReducedMotion()

    LaunchedEffect(Unit) {
        val startedAt = SystemClock.elapsedRealtime()
        while (true) {
            withFrameNanos { }
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            progress = (elapsed.toFloat() / INTRO_DURATION_MS).coerceIn(0f, 1f)
            if (elapsed >= INTRO_DURATION_MS) break
        }
        onFinished()
    }

    // Progress is read in draw/layer lambdas, not in composition, so the intro
    // does not recompose on every frame.
    val symbolAlpha = { smoothstep(0.03f, 0.35f, progress * INTRO_SECONDS) * introExit(progress) }
    val symbolScale = {
        if (reducedMotion) {
            1f
        } else {
            0.94f + 0.06f * smoothstep(0.03f, 0.75f, progress * INTRO_SECONDS)
        }
    }
    val wordmarkAlpha = { smoothstep(0.5f, 1.02f, progress * INTRO_SECONDS) * introExit(progress) }
    val wordmarkOffset = {
        if (reducedMotion) 0f else (1f - smoothstep(0.5f, 1.08f, progress * INTRO_SECONDS)) * 12f
    }
    val taglineAlpha = { smoothstep(1.1f, 1.68f, progress * INTRO_SECONDS) * introExit(progress) }
    val taglineOffset = {
        if (reducedMotion) 0f else (1f - smoothstep(1.1f, 1.74f, progress * INTRO_SECONDS)) * 8f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SonicCanvas(
            progress = { progress },
            reducedMotion = reducedMotion,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = SonaraSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(IntroWhite),
                modifier = Modifier
                    .size(148.dp)
                    .graphicsLayer {
                        alpha = symbolAlpha()
                        scaleX = symbolScale()
                        scaleY = symbolScale()
                    },
            )

            Spacer(Modifier.height(SonaraSpacing.md))

            Image(
                painter = painterResource(R.drawable.sonara_wordmark),
                contentDescription = null,
                colorFilter = ColorFilter.tint(IntroWhite),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .graphicsLayer {
                        alpha = wordmarkAlpha()
                        translationY = wordmarkOffset().dp.toPx()
                    },
            )

            Spacer(Modifier.height(SonaraSpacing.lg))

            Text(
                text = stringResource(R.string.sonara_intro_label),
                style = SonaraLabelStyle.copy(fontSize = 9.sp, letterSpacing = 4.sp),
                color = IntroMuted,
                modifier = Modifier.graphicsLayer {
                    alpha = taglineAlpha()
                    translationY = taglineOffset().dp.toPx()
                },
            )
        }

        IntroProgress(
            progress = { progress },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
        )
    }
}

@Composable
private fun IntroProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .width(132.dp)
            .height(2.dp),
    ) {
        val alpha = introExit(progress())
        drawLine(
            color = IntroWhite.copy(alpha = 0.16f * alpha),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = IntroWhite.copy(alpha = 0.9f * alpha),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width * progress(), size.height / 2f),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SonicCanvas(
    progress: () -> Float,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val phase =
        if (reducedMotion) {
            null
        } else {
            rememberInfiniteTransition(label = "sonicPhase").animateFloat(
                initialValue = 0f,
                targetValue = (2f * PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 9_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "sonicPhaseValue",
            )
        }

    Canvas(modifier = modifier) {
        val timeline = progress() * INTRO_SECONDS
        val intensity =
            smoothstep(0f, 0.28f, timeline) * (1f - smoothstep(2.5f, 3.15f, timeline))
        if (intensity <= 0.001f) return@Canvas

        val glowCenter = Offset(size.width / 2f, size.height * 0.46f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IntroWhite.copy(alpha = 0.055f * intensity), Color.Transparent),
                center = glowCenter,
                radius = size.width * 0.62f,
            ),
            radius = size.width * 0.62f,
            center = glowCenter,
        )

        val horizon = size.height * 0.72f
        drawLine(
            color = IntroWhite.copy(alpha = 0.055f * intensity),
            start = Offset(0f, horizon),
            end = Offset(size.width, horizon),
            strokeWidth = 1.dp.toPx(),
        )

        val bars = 64
        val bandWidth = size.width * 0.82f
        val left = (size.width - bandWidth) / 2f
        val stepX = bandWidth / (bars - 1)
        val maxAmplitude = size.height * 0.075f
        val phaseValue = phase?.value ?: 0f
        val stroke = 1.dp.toPx()

        for (index in 0 until bars) {
            val x = left + index * stepX
            val envelope = sin(PI * index / (bars - 1f)).toFloat()
            val movement =
                if (reducedMotion) {
                    0.5f
                } else {
                    (
                        sin(phaseValue + index * 0.42f) * 0.55f +
                            sin(phaseValue * 1.6f - index * 0.19f) * 0.3f
                    ).let { (it + 0.85f) / 1.7f }
                }
            val amplitude = maxAmplitude * envelope * (0.18f + abs(movement) * 0.82f) * intensity
            if (amplitude < 0.5f) continue
            drawLine(
                color = IntroWhite.copy(alpha = (0.06f + 0.16f * envelope) * intensity),
                start = Offset(x, horizon - amplitude),
                end = Offset(x, horizon + amplitude),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Monochrome "sonic halo" for the Welcome screen: rotating tick ring, three
 * breathing rings and the official symbol faintly at the centre. This fills
 * the dead space above the account actions without adding colour or clutter.
 */
@Composable
private fun WelcomeSonicOrb(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "welcomeSonicOrb")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbRotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbPulse",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val base = size.minDimension * 0.27f
            val gap = size.minDimension * 0.072f

            repeat(3) { index ->
                val radius = base + gap * index + pulse * (2.dp.toPx() + index * 2.dp.toPx())
                drawCircle(
                    color = IntroWhite.copy(
                        alpha = (0.11f - index * 0.025f) * (0.7f + pulse * 0.3f),
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            val tickCount = 72
            val tickInner = base + gap * 3f
            for (index in 0 until tickCount) {
                val angle = (rotation + index * (360f / tickCount)) * (PI.toFloat() / 180f)
                val wave = (sin(angle * 6f + pulse * 4f) * 0.5f + 0.5f)
                val length = (3.dp.toPx() + wave * 13.dp.toPx()) * (0.45f + pulse * 0.55f)
                val dx = cos(angle)
                val dy = sin(angle)
                drawLine(
                    color = IntroWhite.copy(alpha = 0.14f + wave * 0.16f),
                    start = Offset(center.x + dx * tickInner, center.y + dy * tickInner),
                    end = Offset(center.x + dx * (tickInner + length), center.y + dy * (tickInner + length)),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            drawCircle(
                color = IntroWhite.copy(alpha = 0.32f),
                radius = 1.5.dp.toPx(),
                center = center,
            )
        }

        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(IntroWhite),
            modifier = Modifier
                .size(58.dp)
                .alpha(0.16f),
        )
    }
}

@Composable
private fun SonaraWelcome(
    onGoogle: () -> Unit,
    onGuest: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(IntroBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Landscape phones do not have room for the bottom-aligned composition,
        // so collapse the flexible spacer and let the panel scroll instead.
        val compact = maxHeight < 620.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (compact) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = SonaraSpacing.xl, vertical = SonaraSpacing.xl),
        ) {
            Image(
                painter = painterResource(R.drawable.sonara_logo_horizontal),
                contentDescription = null,
                colorFilter = ColorFilter.tint(IntroWhite),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(22.dp)
                    .widthIn(max = 124.dp),
            )

            if (compact) {
                Spacer(Modifier.height(SonaraSpacing.lg))
                WelcomeSonicOrb(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp),
                )
                Spacer(Modifier.height(SonaraSpacing.lg))
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    WelcomeSonicOrb(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.sonara_welcome_headline),
                style = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.6).sp,
                ),
                color = IntroWhite,
            )

            Spacer(Modifier.height(SonaraSpacing.sm))

            Text(
                text = stringResource(R.string.sonara_welcome_description),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = IntroMuted,
            )

            Spacer(Modifier.height(SonaraSpacing.xxl))

            WelcomeAction(
                title = stringResource(R.string.sonara_welcome_google),
                subtitle = stringResource(R.string.sonara_welcome_google_subtitle),
                primary = true,
                onClick = onGoogle,
                icon = painterResource(R.drawable.ic_google_g),
                tintIcon = false,
            )

            Spacer(Modifier.height(SonaraSpacing.sm))

            WelcomeAction(
                title = stringResource(R.string.sonara_welcome_guest),
                subtitle = stringResource(R.string.sonara_welcome_guest_subtitle),
                primary = false,
                onClick = onGuest,
                icon = painterResource(R.drawable.person),
                tintIcon = true,
            )

            Spacer(Modifier.height(SonaraSpacing.lg))

            Text(
                text = stringResource(R.string.sonara_welcome_note),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = IntroDim,
            )
        }
    }
}

@Composable
private fun WelcomeAction(
    title: String,
    subtitle: String,
    primary: Boolean,
    onClick: () -> Unit,
    icon: Painter? = null,
    tintIcon: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "welcomePress",
    )
    val contentColor = if (primary) IntroBlack else IntroWhite
    val secondaryContent = if (primary) IntroBlack.copy(alpha = 0.58f) else IntroMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (primary) IntroWhite else Color(0xFF101113))
            .border(
                width = 1.dp,
                color = if (primary) Color.Transparent else IntroWhite.copy(alpha = 0.14f),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor.copy(alpha = 0.18f)),
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                },
            )
            .padding(horizontal = SonaraSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (icon != null) {
            Image(
                painter = icon,
                contentDescription = null,
                colorFilter = if (tintIcon) ColorFilter.tint(contentColor) else null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = contentColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = secondaryContent,
            )
        }

        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        // Android has no public reduce-motion flag; a zero animator scale is
        // the standard signal that system animations are turned off.
        val animatorScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        val transitionScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f,
        )
        animatorScale == 0f || transitionScale == 0f
    }
}

private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
    if (edge1 <= edge0) return if (value < edge0) 0f else 1f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun introExit(progress: Float): Float = 1f - smoothstep(2.58f, 3.1f, progress * INTRO_SECONDS)
