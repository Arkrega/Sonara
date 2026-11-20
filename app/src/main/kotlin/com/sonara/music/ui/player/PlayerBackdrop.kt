/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Mesh backdrop sampling inspired by SimpMusic (https://github.com/maxrave-dev/SimpMusic)
 */

package com.sonara.music.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.sonara.music.constants.PlayerBackgroundStyle
import com.sonara.music.ui.component.glass.RegaBackdropState
import com.sonara.music.ui.component.glass.regaBackdropSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.floor

@Immutable
internal data class PlayerMeshData(
    val bitmap: ImageBitmap,
    val topLuminance: Float,
    val primaryColor: Color,
)

private const val MESH_GRID_SIZE = 6
private const val MESH_OUT_SIZE = 32

internal suspend fun generatePlayerMeshFromUrl(
    context: android.content.Context,
    artworkUrl: String?,
    seed: Int,
): PlayerMeshData? {
    if (artworkUrl.isNullOrBlank()) return null
    val request = ImageRequest.Builder(context)
        .data(artworkUrl)
        .size(96, 96)
        .allowHardware(false)
        .build()
    val result = runCatching { context.imageLoader.execute(request) }.getOrNull() ?: return null
    val bitmap = result.image?.toBitmap() ?: return null
    return withContext(Dispatchers.Default) {
        buildPlayerMesh(bitmap, seed)
    }
}

internal fun buildPlayerMesh(artwork: Bitmap, seed: Int): PlayerMeshData? {
    val safeBitmap = if (artwork.config == Bitmap.Config.HARDWARE) {
        artwork.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        artwork
    } ?: return null

    val scaled = Bitmap.createScaledBitmap(safeBitmap, MESH_GRID_SIZE, MESH_GRID_SIZE, true)
    val rawPixels = IntArray(MESH_GRID_SIZE * MESH_GRID_SIZE)
    scaled.getPixels(rawPixels, 0, MESH_GRID_SIZE, 0, 0, MESH_GRID_SIZE, MESH_GRID_SIZE)
    if (scaled !== safeBitmap) scaled.recycle()
    if (safeBitmap !== artwork) safeBitmap.recycle()

    val gridR = FloatArray(MESH_GRID_SIZE * MESH_GRID_SIZE)
    val gridG = FloatArray(MESH_GRID_SIZE * MESH_GRID_SIZE)
    val gridB = FloatArray(MESH_GRID_SIZE * MESH_GRID_SIZE)

    var topLumaSum = 0f
    var bestColorWeight = -1f
    var bestR = 0.14f
    var bestG = 0.14f
    var bestB = 0.16f

    for (y in 0 until MESH_GRID_SIZE) {
        val rowShift = if (y >= MESH_GRID_SIZE / 2) ((seed ushr (y * 3)) and 0x3) else 0
        val mirrorRow = if (y >= MESH_GRID_SIZE / 2 && (seed and (1 shl y)) != 0) true else false
        for (x in 0 until MESH_GRID_SIZE) {
            val sx = (if (mirrorRow) MESH_GRID_SIZE - 1 - x else x + rowShift) % MESH_GRID_SIZE
            val px = rawPixels[y * MESH_GRID_SIZE + sx]
            val r = ((px ushr 16) and 0xFF) / 255f
            val g = ((px ushr 8) and 0xFF) / 255f
            val b = (px and 0xFF) / 255f
            val idx = y * MESH_GRID_SIZE + x
            val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
            // MONOCHROME SONIC: the backdrop samples shape, never colour.
            val mono = luma * 0.55f
            gridR[idx] = mono
            gridG[idx] = mono
            gridB[idx] = mono
            if (y == 0) topLumaSum += luma

            val weight = 1f - abs(luma - 0.42f)
            if (weight > bestColorWeight) {
                bestColorWeight = weight
                bestR = mono
                bestG = mono
                bestB = mono
            }
        }
    }

    val outPixels = IntArray(MESH_OUT_SIZE * MESH_OUT_SIZE)
    for (py in 0 until MESH_OUT_SIZE) {
        val fy = (py.toFloat() / (MESH_OUT_SIZE - 1)) * (MESH_GRID_SIZE - 1)
        val y0 = floor(fy).toInt().coerceIn(0, MESH_GRID_SIZE - 2)
        val y1 = y0 + 1
        val tyRaw = fy - y0
        val ty = tyRaw * tyRaw * (3f - 2f * tyRaw)

        for (px in 0 until MESH_OUT_SIZE) {
            val fx = (px.toFloat() / (MESH_OUT_SIZE - 1)) * (MESH_GRID_SIZE - 1)
            val x0 = floor(fx).toInt().coerceIn(0, MESH_GRID_SIZE - 2)
            val x1 = x0 + 1
            val txRaw = fx - x0
            val tx = txRaw * txRaw * (3f - 2f * txRaw)

            val i00 = y0 * MESH_GRID_SIZE + x0
            val i10 = y0 * MESH_GRID_SIZE + x1
            val i01 = y1 * MESH_GRID_SIZE + x0
            val i11 = y1 * MESH_GRID_SIZE + x1

            val r0 = gridR[i00] + (gridR[i10] - gridR[i00]) * tx
            val r1 = gridR[i01] + (gridR[i11] - gridR[i01]) * tx
            val r = ((r0 + (r1 - r0) * ty).coerceIn(0f, 1f) * 255f).toInt()

            val g0 = gridG[i00] + (gridG[i10] - gridG[i00]) * tx
            val g1 = gridG[i01] + (gridG[i11] - gridG[i01]) * tx
            val g = ((g0 + (g1 - g0) * ty).coerceIn(0f, 1f) * 255f).toInt()

            val b0 = gridB[i00] + (gridB[i10] - gridB[i00]) * tx
            val b1 = gridB[i01] + (gridB[i11] - gridB[i01]) * tx
            val b = ((b0 + (b1 - b0) * ty).coerceIn(0f, 1f) * 255f).toInt()

            outPixels[py * MESH_OUT_SIZE + px] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    val meshBitmap = Bitmap.createBitmap(outPixels, MESH_OUT_SIZE, MESH_OUT_SIZE, Bitmap.Config.ARGB_8888)
    return PlayerMeshData(
        bitmap = meshBitmap.asImageBitmap(),
        topLuminance = (topLumaSum / MESH_GRID_SIZE).coerceIn(0f, 1f),
        primaryColor = Color(bestR, bestG, bestB, 1f),
    )
}

@Composable
internal fun PlayerDynamicBackdrop(
    thumbnailUrl: String?,
    mediaId: String?,
    gradientColors: List<Color>,
    playerBackground: PlayerBackgroundStyle,
    useDarkTheme: Boolean,
    pureBlack: Boolean,
    backgroundAlpha: Float,
    backdropState: RegaBackdropState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var currentMesh by remember { mutableStateOf<PlayerMeshData?>(null) }
    var previousMesh by remember { mutableStateOf<ImageBitmap?>(null) }
    val crossfadeProgress = remember { Animatable(1f) }

    LaunchedEffect(mediaId, thumbnailUrl) {
        val loaded = generatePlayerMeshFromUrl(context, thumbnailUrl, (mediaId ?: thumbnailUrl).hashCode())
        if (loaded != null) {
            previousMesh = currentMesh?.bitmap
            currentMesh = loaded
            crossfadeProgress.snapTo(0f)
            crossfadeProgress.animateTo(1f, tween(durationMillis = 700))
        }
    }

    val baseBackgroundColor = when {
        playerBackground == PlayerBackgroundStyle.BLUR ||
            playerBackground == PlayerBackgroundStyle.GRADIENT -> Color(0xFF08090A)
        pureBlack && useDarkTheme -> Color.Black
        useDarkTheme -> Color(0xFF08090A)
        else -> Color(0xFFF7F8FA)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .regaBackdropSource(backdropState)
            .background(baseBackgroundColor),
    ) {
        when (playerBackground) {
            PlayerBackgroundStyle.BLUR -> {
                AnimatedContent(
                    targetState = thumbnailUrl,
                    transitionSpec = { fadeIn(tween(750)).togetherWith(fadeOut(tween(750))) },
                    label = "playerBlurBackdrop",
                ) { url ->
                    if (!url.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxSize().alpha(backgroundAlpha)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(url)
                                    .size(128, 128)
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                colorFilter = ColorFilter.colorMatrix(
                                    ColorMatrix().apply { setToSaturation(0f) },
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(64.dp, BlurredEdgeTreatment.Unbounded),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Black.copy(alpha = 0.24f),
                                            0.55f to Color.Black.copy(alpha = 0.44f),
                                            1f to Color.Black.copy(alpha = 0.78f),
                                        )
                                    ),
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.GRADIENT -> {
                val activeMesh = currentMesh
                val prev = previousMesh
                if (activeMesh != null) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(backgroundAlpha)
                            .blur(48.dp, BlurredEdgeTreatment.Unbounded),
                    ) {
                        val dst = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
                        if (prev != null && crossfadeProgress.value < 0.99f) {
                            drawImage(
                                image = prev,
                                dstOffset = IntOffset.Zero,
                                dstSize = dst,
                                alpha = 1f - crossfadeProgress.value,
                            )
                        }
                        drawImage(
                            image = activeMesh.bitmap,
                            dstOffset = IntOffset.Zero,
                            dstSize = dst,
                            alpha = crossfadeProgress.value,
                        )
                    }
                    val seed = activeMesh.primaryColor
                    val topColor = lerp(seed, Color.Black, 0.22f).copy(alpha = 0.32f)
                    val bottomColor = lerp(seed, Color.Black, 0.84f).copy(alpha = 0.88f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(backgroundAlpha)
                            .background(
                                Brush.verticalGradient(
                                    0f to topColor,
                                    0.45f to Color.Black.copy(alpha = 0.36f),
                                    1f to bottomColor,
                                )
                            ),
                    )
                } else {
                    AmbientRadialBackdrop(
                        gradientColors = gradientColors,
                        baseColor = baseBackgroundColor,
                        useDarkTheme = true,
                        backgroundAlpha = backgroundAlpha,
                    )
                }
            }

            else -> {
                AmbientRadialBackdrop(
                    gradientColors = gradientColors,
                    baseColor = baseBackgroundColor,
                    useDarkTheme = useDarkTheme,
                    backgroundAlpha = backgroundAlpha,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.AmbientRadialBackdrop(
    gradientColors: List<Color>,
    baseColor: Color,
    useDarkTheme: Boolean,
    backgroundAlpha: Float,
) {
    AnimatedContent(
        targetState = gradientColors,
        transitionSpec = { fadeIn(tween(750)).togetherWith(fadeOut(tween(750))) },
        label = "playerAmbientBackdrop",
    ) { colors ->
        val primary = colors.firstOrNull() ?: if (useDarkTheme) Color(0xFF28282E) else Color(0xFFDCDCDC)
        val secondary = colors.getOrNull(1) ?: primary
        val tertiary = colors.getOrNull(2) ?: secondary
        val pAlpha = if (useDarkTheme) 0.42f else 0.22f
        val sAlpha = if (useDarkTheme) 0.26f else 0.14f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(backgroundAlpha)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = pAlpha), Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.08f),
                            radius = size.maxDimension * 0.9f,
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(secondary.copy(alpha = sAlpha), Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.38f),
                            radius = size.maxDimension * 0.72f,
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(tertiary.copy(alpha = sAlpha * 0.75f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.78f),
                            radius = size.maxDimension * 0.68f,
                        )
                    )
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                baseColor.copy(alpha = if (useDarkTheme) 0.35f else 0.45f),
                                baseColor.copy(alpha = if (useDarkTheme) 0.92f else 0.90f),
                            ),
                            startY = size.height * 0.34f,
                            endY = size.height,
                        )
                    )
                },
        )
    }
}
