package com.sonara.music.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.sonara.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
fun SquareImageCropDialog(
    sourceUri: Uri,
    destFile: File,
    title: String,
    onDismiss: () -> Unit,
    onCropped: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var decodedBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(sourceUri) { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var zoom by remember(sourceUri) { mutableFloatStateOf(1f) }
    var offsetX by remember(sourceUri) { mutableFloatStateOf(0f) }
    var offsetY by remember(sourceUri) { mutableFloatStateOf(0f) }
    var viewportSizePx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(sourceUri) {
        val loaded = decodeSampledExifBitmap(context, sourceUri, 2048)
        if (loaded == null) {
            loadFailed = true
        } else {
            decodedBitmap = loaded
        }
    }

    LaunchedEffect(loadFailed) {
        if (loadFailed) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0A0C),
            contentColor = Color.White,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        enabled = !isSaving,
                        onClick = onDismiss,
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Button(
                        enabled = decodedBitmap != null && viewportSizePx > 0f && !isSaving,
                        onClick = {
                            val bmp = decodedBitmap ?: return@Button
                            val boxSize = viewportSizePx
                            if (boxSize <= 0f) return@Button
                            isSaving = true
                            scope.launch {
                                val resultUri =
                                    saveCroppedSquare(
                                        context = context,
                                        bitmap = bmp,
                                        destFile = destFile,
                                        boxSizePx = boxSize,
                                        zoom = zoom,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                    )
                                isSaving = false
                                if (resultUri != null) {
                                    onCropped(resultUri)
                                } else {
                                    onDismiss()
                                }
                            }
                        },
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.crop_apply))
                        }
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = decodedBitmap
                    if (bmp == null) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                        val density = LocalDensity.current
                        BoxWithConstraints(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clipToBounds()
                                    .background(Color.Black)
                                    .border(
                                        width = 1.5.dp,
                                        color = Color.White.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                        ) {
                            val boxPx = with(density) { maxWidth.toPx() }
                            LaunchedEffect(boxPx) {
                                viewportSizePx = boxPx
                            }
                            val baseScale =
                                maxOf(
                                    boxPx / bmp.width.toFloat(),
                                    boxPx / bmp.height.toFloat(),
                                )
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .pointerInput(bmp, boxPx) {
                                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                                val nextZoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                                val totalScale = baseScale * nextZoom
                                                val maxX = ((bmp.width * totalScale - boxPx) * 0.5f).coerceAtLeast(0f)
                                                val maxY = ((bmp.height * totalScale - boxPx) * 0.5f).coerceAtLeast(0f)
                                                zoom = nextZoom
                                                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                            }
                                        },
                            ) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = zoom
                                                scaleY = zoom
                                                translationX = offsetX
                                                translationY = offsetY
                                            },
                                )
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val gridColor = Color.White.copy(alpha = 0.25f)
                                    val stroke = 1.dp.toPx()
                                    val thirdW = size.width / 3f
                                    val thirdH = size.height / 3f
                                    drawLine(gridColor, Offset(thirdW, 0f), Offset(thirdW, size.height), stroke)
                                    drawLine(gridColor, Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, size.height), stroke)
                                    drawLine(gridColor, Offset(0f, thirdH), Offset(size.width, thirdH), stroke)
                                    drawLine(gridColor, Offset(0f, thirdH * 2f), Offset(size.width, thirdH * 2f), stroke)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private suspend fun decodeSampledExifBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int,
): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sampleSize = 1
            while ((bounds.outWidth / sampleSize) > maxDimension || (bounds.outHeight / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded =
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: return@runCatching null

            val orientation =
                resolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL

            applyExifTransform(decoded, orientation)
        }.getOrNull()
    }

private fun applyExifTransform(
    bitmap: Bitmap,
    orientation: Int,
): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setRotate(180f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        else -> return bitmap
    }
    val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (transformed !== bitmap) {
        bitmap.recycle()
    }
    return transformed
}

private suspend fun saveCroppedSquare(
    context: Context,
    bitmap: Bitmap,
    destFile: File,
    boxSizePx: Float,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
): Uri? =
    withContext(Dispatchers.IO) {
        runCatching {
            val bw = bitmap.width
            val bh = bitmap.height
            val baseScale = maxOf(boxSizePx / bw.toFloat(), boxSizePx / bh.toFloat())
            val totalScale = (baseScale * zoom).coerceAtLeast(1e-4f)

            val rawSize = (boxSizePx / totalScale).roundToInt().coerceIn(1, minOf(bw, bh))
            val left =
                ((bw * 0.5f) - (boxSizePx * 0.5f + offsetX) / totalScale)
                    .roundToInt()
                    .coerceIn(0, (bw - rawSize).coerceAtLeast(0))
            val top =
                ((bh * 0.5f) - (boxSizePx * 0.5f + offsetY) / totalScale)
                    .roundToInt()
                    .coerceIn(0, (bh - rawSize).coerceAtLeast(0))
            val cropSize = minOf(rawSize, bw - left, bh - top).coerceAtLeast(1)

            val cropped = Bitmap.createBitmap(bitmap, left, top, cropSize, cropSize)
            val scaled =
                if (cropped.width != 1024 || cropped.height != 1024) {
                    Bitmap.createScaledBitmap(cropped, 1024, 1024, true).also {
                        if (it !== cropped && cropped !== bitmap) {
                            cropped.recycle()
                        }
                    }
                } else {
                    cropped
                }

            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            if (scaled !== bitmap && !scaled.isRecycled) {
                scaled.recycle()
            }

            FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", destFile)
        }.getOrNull()
    }
