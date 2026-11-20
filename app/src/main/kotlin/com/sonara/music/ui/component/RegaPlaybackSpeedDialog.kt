package com.sonara.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackParameters
import com.sonara.music.LocalPlayerConnection
import com.sonara.music.R
import com.sonara.music.constants.PlaybackPitchKey
import com.sonara.music.constants.PlaybackSpeedKey
import com.sonara.music.constants.VarispeedKey
import com.sonara.music.ui.component.glass.RegaGlassChip
import com.sonara.music.ui.component.glass.RegaGlassSwitch
import com.sonara.music.ui.theme.LocalRegaColors
import com.sonara.music.utils.rememberPreference
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun RegaPlaybackSpeedDialog(
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val regaColors = LocalRegaColors.current
    var savedSpeed by rememberPreference(PlaybackSpeedKey, defaultValue = 1.0f)
    var savedPitch by rememberPreference(PlaybackPitchKey, defaultValue = 0.0f)
    var varispeedMode by rememberPreference(VarispeedKey, defaultValue = false)

    var currentSpeed by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeSemitones by remember {
        val rawPitch = playerConnection.player.playbackParameters.pitch
        mutableIntStateOf(if (rawPitch > 0f) round(12 * log2(rawPitch)).toInt() else 0)
    }

    val speedPresets = remember {
        listOf(0.50f, 0.75f, 0.90f, 1.00f, 1.10f, 1.25f, 1.50f, 1.75f, 2.00f)
    }

    fun applyPlaybackParameters(newSpeed: Float, newTranspose: Int, isVarispeed: Boolean) {
        val calculatedPitch = if (isVarispeed) {
            newSpeed
        } else {
            2f.pow(newTranspose.toFloat() / 12f)
        }
        playerConnection.player.playbackParameters = PlaybackParameters(newSpeed, calculatedPitch)
        savedSpeed = newSpeed
        savedPitch = newTranspose.toFloat()
    }

    DefaultDialog(
        onDismiss = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.playback_speed_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "%.2fx".format(currentSpeed),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        },
        buttons = {
            Row(modifier = Modifier.weight(1f)) {
                TextButton(
                    onClick = {
                        currentSpeed = 1.0f
                        transposeSemitones = 0
                        applyPlaybackParameters(1.0f, 0, varispeedMode)
                    },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reset),
                        color = regaColors.textSecondary,
                    )
                }
            }
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = stringResource(android.R.string.ok),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.speed),
                style = MaterialTheme.typography.labelMedium,
                color = regaColors.textSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                speedPresets.forEach { preset ->
                    val isSelected = (round(currentSpeed * 100) / 100) == preset
                    RegaGlassChip(
                        selected = isSelected,
                        onClick = {
                            currentSpeed = preset
                            applyPlaybackParameters(preset, transposeSemitones, varispeedMode)
                        },
                        text = "%.2fx".format(preset),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = currentSpeed,
                onValueChange = {
                    val rounded = (round(it * 20) / 20).coerceIn(0.50f, 2.00f)
                    currentSpeed = rounded
                    applyPlaybackParameters(rounded, transposeSemitones, varispeedMode)
                },
                valueRange = 0.50f..2.00f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = regaColors.borderMuted,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.varispeed_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.varispeed_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = regaColors.textSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                RegaGlassSwitch(
                    checked = varispeedMode,
                    onCheckedChange = { isChecked ->
                        varispeedMode = isChecked
                        applyPlaybackParameters(currentSpeed, transposeSemitones, isChecked)
                    },
                )
            }

            if (!varispeedMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.pitch_adjustment),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${if (transposeSemitones > 0) "+" else ""}$transposeSemitones st",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Slider(
                    value = transposeSemitones.toFloat(),
                    onValueChange = {
                        val rounded = it.roundToInt().coerceIn(-12, 12)
                        transposeSemitones = rounded
                        applyPlaybackParameters(currentSpeed, rounded, false)
                    },
                    valueRange = -12f..12f,
                    steps = 23,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = regaColors.borderMuted,
                    ),
                )
            }
        }
    }
}

@Composable
fun SonaraPlaybackSpeedDialog(onDismiss: () -> Unit) = RegaPlaybackSpeedDialog(onDismiss)
