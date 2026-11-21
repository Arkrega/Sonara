package com.sonara.music.ui.screens.equalizer

import android.annotation.SuppressLint
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.music.LocalNavController
import com.sonara.music.LocalPlayerAwareWindowInsets
import com.sonara.music.LocalPlayerConnection
import com.sonara.music.R
import com.sonara.music.constants.ReverbEnabledKey
import com.sonara.music.constants.ReverbPresetKey
import com.sonara.music.constants.ReverbRoomSizeKey
import com.sonara.music.constants.ReverbDampingKey
import com.sonara.music.constants.ReverbWetLevelKey
import com.sonara.music.constants.ReverbDryLevelKey
import com.sonara.music.constants.ReverbWidthKey
import com.sonara.music.constants.ReverbDecayKey
import com.sonara.music.eq.data.ReverbConfig
import com.sonara.music.eq.data.ReverbPresetType
import kotlin.math.roundToInt
import com.sonara.music.eq.audio.RegaReverbManager
import com.sonara.music.eq.data.ParametricEQBand
import com.sonara.music.eq.data.SavedEQProfile
import com.sonara.music.ui.theme.RegaColor
import com.sonara.music.utils.rememberPreference
import kotlin.math.round
import com.sonara.music.ui.component.glass.SonaraSwitch
import com.sonara.music.ui.component.glass.regaTopAppBarGlass
import com.sonara.music.ui.component.glass.regaTopAppBarColors

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqScreen(
    viewModel: EQViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showError by remember { mutableStateOf<String?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<SavedEQProfile?>(null) }
    var showDeleteDialog by remember { mutableStateOf<SavedEQProfile?>(null) }

    var savedReverbEnabled by rememberPreference(ReverbEnabledKey, defaultValue = false)
    var savedReverbPreset by rememberPreference(ReverbPresetKey, defaultValue = 0)
    var savedReverbRoomSize by rememberPreference(ReverbRoomSizeKey, defaultValue = 0.35f)
    var savedReverbDamping by rememberPreference(ReverbDampingKey, defaultValue = 0.50f)
    var savedReverbWetLevel by rememberPreference(ReverbWetLevelKey, defaultValue = 0.25f)
    var savedReverbDryLevel by rememberPreference(ReverbDryLevelKey, defaultValue = 0.95f)
    var savedReverbWidth by rememberPreference(ReverbWidthKey, defaultValue = 0.70f)
    var savedReverbDecay by rememberPreference(ReverbDecayKey, defaultValue = 0.30f)

    val reverbSupported by RegaReverbManager.isSupported.collectAsStateWithLifecycle()
    val reverbEnabled by RegaReverbManager.isEnabled.collectAsStateWithLifecycle()
    val reverbActive by RegaReverbManager.isEffectActive.collectAsStateWithLifecycle()
    val reverbHasControl by RegaReverbManager.hasControl.collectAsStateWithLifecycle()
    val reverbPreset by RegaReverbManager.currentPreset.collectAsStateWithLifecycle()
    val reverbRoomSize by RegaReverbManager.roomSize.collectAsStateWithLifecycle()
    val reverbDamping by RegaReverbManager.damping.collectAsStateWithLifecycle()
    val reverbWetLevel by RegaReverbManager.wetLevel.collectAsStateWithLifecycle()
    val reverbDryLevel by RegaReverbManager.dryLevel.collectAsStateWithLifecycle()
    val reverbWidth by RegaReverbManager.width.collectAsStateWithLifecycle()
    val reverbDecay by RegaReverbManager.decay.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val presetType = ReverbPresetType.fromId(savedReverbPreset)
        if (presetType == ReverbPresetType.CUSTOM) {
            RegaReverbManager.applyFullConfig(
                ReverbConfig(
                    enabled = savedReverbEnabled,
                    presetType = ReverbPresetType.CUSTOM,
                    roomSize = savedReverbRoomSize,
                    damping = savedReverbDamping,
                    wetLevel = savedReverbWetLevel,
                    dryLevel = savedReverbDryLevel,
                    width = savedReverbWidth,
                    decay = savedReverbDecay
                )
            )
        } else {
            RegaReverbManager.setPreset(presetType)
            RegaReverbManager.setEnabled(savedReverbEnabled)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "custom_eq.txt"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex >= 0) {
                            val name = cursor.getString(displayNameIndex)
                            if (!name.isNullOrBlank()) {
                                fileName = name
                            }
                        }
                    }
                }

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importCustomProfile(
                        fileName = fileName,
                        inputStream = inputStream,
                        onSuccess = {},
                        onError = { error ->
                            showError = context.getString(R.string.import_error_title) + ": " + error.message
                        }
                    )
                } else {
                    showError = context.getString(R.string.error_file_read)
                }
            } catch (e: Exception) {
                showError = context.getString(R.string.error_file_open, e.message)
            }
        }
    }

    val activeProfile = state.profiles.find { it.id == state.activeProfileId }

    Scaffold(
        containerColor = RegaColor.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.equalizer_header),
                        style = MaterialTheme.typography.titleLarge,
                        color = RegaColor.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            tint = RegaColor.TextPrimary
                        )
                    }
                },
                actions = {
                    SonaraSwitch(
                        checked = state.isEnabled,
                        onCheckedChange = { viewModel.toggleEqualizer(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RegaColor.OnAccent,
                            checkedTrackColor = RegaColor.PrimaryAccent,
                            uncheckedThumbColor = RegaColor.TextSecondary,
                            uncheckedTrackColor = RegaColor.SurfaceContainerHigh
                        )
                    )
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                tint = RegaColor.TextPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reset_to_flat)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.refresh),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.resetToFlat()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save_as_new_preset)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.add),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    showSaveDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.eq_wizard)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.discover_tune),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    navController.navigate("eq_wizard")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_from_file)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.upload),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    filePickerLauncher.launch("text/plain")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.restore_default_presets)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.restore),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.restoreDefaults()
                                }
                            )
                        }
                    }
                },
                colors = regaTopAppBarColors(TopAppBarDefaults.topAppBarColors(
                    containerColor = RegaColor.Background,
                    titleContentColor = RegaColor.TextPrimary
                )),
                modifier = Modifier.regaTopAppBarGlass(),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                bottom = LocalPlayerAwareWindowInsets.current
                    .asPaddingValues()
                    .calculateBottomPadding() + 24.dp
            )
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = RegaColor.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RegaColor.BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FREQUENCY RESPONSE",
                                style = MaterialTheme.typography.labelSmall,
                                color = RegaColor.PrimaryAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (state.isEnabled) "ACTIVE" else "BYPASS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isEnabled) RegaColor.PrimaryAccent else RegaColor.TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        EqFrequencyResponseGraph(
                            bands = state.currentBands,
                            preamp = state.currentPreamp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .alpha(if (state.isEnabled) 1.0f else 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    color = RegaColor.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RegaColor.BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.preamp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = RegaColor.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Gain compensation to prevent digital clipping",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RegaColor.TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RegaColor.SurfaceContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "%+.1f dB".format(state.currentPreamp),
                                    color = RegaColor.PrimaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = state.currentPreamp.toFloat(),
                                onValueChange = {
                                    if (state.isEnabled) {
                                        val rounded = (round(it * 2) / 2).toDouble()
                                        viewModel.updatePreamp(rounded)
                                    }
                                },
                                valueRange = -12.0f..12.0f,
                                steps = 47,
                                modifier = Modifier.weight(1f),
                                enabled = state.isEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = RegaColor.PrimaryAccent,
                                    activeTrackColor = RegaColor.PrimaryAccent,
                                    inactiveTrackColor = RegaColor.SurfaceContainerHighest
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { viewModel.updatePreamp(0.0) },
                                enabled = state.isEnabled
                            ) {
                                Text(
                                    text = "0 dB",
                                    color = if (state.isEnabled) RegaColor.TextSecondary else Color.Transparent,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUDIO PRESETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = RegaColor.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        TextButton(
                            onClick = { showSaveDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null,
                                tint = RegaColor.PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.save_preset),
                                color = RegaColor.PrimaryAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.profiles.forEach { profile ->
                            val isSelected = state.activeProfileId == profile.id && state.isEnabled
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectProfile(profile.id) },
                                label = {
                                    Text(
                                        text = profile.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                trailingIcon = {
                                    if (profile.isCustom) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { showRenameDialog = profile },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.edit),
                                                    contentDescription = null,
                                                    tint = if (isSelected) RegaColor.OnAccent else RegaColor.TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(2.dp))
                                            IconButton(
                                                onClick = { showDeleteDialog = profile },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.delete),
                                                    contentDescription = null,
                                                    tint = if (isSelected) RegaColor.OnAccent else RegaColor.TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RegaColor.PrimaryAccent,
                                    selectedLabelColor = RegaColor.OnAccent,
                                    containerColor = RegaColor.Surface,
                                    labelColor = RegaColor.TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) Color.Transparent else RegaColor.BorderSubtle
                                )
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .alpha(if (state.isEnabled) 1.0f else 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    color = RegaColor.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RegaColor.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "10-BAND PARAMETRIC EQUALIZER",
                                style = MaterialTheme.typography.labelSmall,
                                color = RegaColor.PrimaryAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "±12 dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = RegaColor.TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.currentBands.forEach { band ->
                                BandSliderRow(
                                    band = band,
                                    enabled = state.isEnabled,
                                    onGainChange = { newGain ->
                                        viewModel.updateBandGain(band.frequency, newGain)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = RegaColor.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RegaColor.BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.reverb_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = RegaColor.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = when {
                                        !reverbSupported -> stringResource(R.string.reverb_unsupported)
                                        !reverbHasControl && reverbEnabled -> stringResource(R.string.reverb_control_unavailable)
                                        else -> stringResource(R.string.reverb_desc)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (reverbSupported && (reverbHasControl || !reverbEnabled)) RegaColor.TextSecondary else MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            SonaraSwitch(
                                checked = reverbEnabled && reverbSupported,
                                onCheckedChange = { isChecked ->
                                    RegaReverbManager.setEnabled(isChecked)
                                    savedReverbEnabled = isChecked
                                },
                                enabled = reverbSupported,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = RegaColor.OnAccent,
                                    checkedTrackColor = RegaColor.PrimaryAccent,
                                    uncheckedThumbColor = RegaColor.TextSecondary,
                                    uncheckedTrackColor = RegaColor.SurfaceContainerHigh
                                )
                            )
                        }

                        if (reverbSupported && reverbEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val reverbPresets = remember {
                                listOf(
                                    ReverbPresetType.SMALL_ROOM to R.string.reverb_preset_small_room,
                                    ReverbPresetType.MEDIUM_ROOM to R.string.reverb_preset_medium_room,
                                    ReverbPresetType.LARGE_HALL to R.string.reverb_preset_large_hall,
                                    ReverbPresetType.CATHEDRAL to R.string.reverb_preset_cathedral,
                                    ReverbPresetType.PLATE to R.string.reverb_preset_plate,
                                    ReverbPresetType.STUDIO to R.string.reverb_preset_studio,
                                    ReverbPresetType.CUSTOM to R.string.reverb_preset_custom
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                reverbPresets.forEach { (presetValue, stringResId) ->
                                    val isSelected = reverbPreset == presetValue
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            RegaReverbManager.setPreset(presetValue)
                                            savedReverbPreset = presetValue.id
                                        },
                                        label = { Text(stringResource(stringResId)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RegaColor.PrimaryAccent,
                                            selectedLabelColor = RegaColor.OnAccent,
                                            containerColor = RegaColor.SurfaceContainer,
                                            labelColor = RegaColor.TextPrimary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) Color.Transparent else RegaColor.BorderSubtle
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ReverbSliderRow(
                                    label = stringResource(R.string.reverb_room_size),
                                    value = reverbRoomSize,
                                    onValueChange = {
                                        RegaReverbManager.setRoomSize(it)
                                        savedReverbPreset = ReverbPresetType.CUSTOM.id
                                        savedReverbRoomSize = it
                                    }
                                )
                                ReverbSliderRow(
                                    label = stringResource(R.string.reverb_damping),
                                    value = reverbDamping,
                                    onValueChange = {
                                        RegaReverbManager.setDamping(it)
                                        savedReverbPreset = ReverbPresetType.CUSTOM.id
                                        savedReverbDamping = it
                                    }
                                )
                                ReverbSliderRow(
                                    label = stringResource(R.string.reverb_wet_level),
                                    value = reverbWetLevel,
                                    onValueChange = {
                                        RegaReverbManager.setWetLevel(it)
                                        savedReverbPreset = ReverbPresetType.CUSTOM.id
                                        savedReverbWetLevel = it
                                    }
                                )
                                ReverbSliderRow(
                                    label = stringResource(R.string.reverb_dry_level),
                                    value = reverbDryLevel,
                                    onValueChange = {
                                        RegaReverbManager.setDryLevel(it)
                                        savedReverbPreset = ReverbPresetType.CUSTOM.id
                                        savedReverbDryLevel = it
                                    }
                                )
                                ReverbSliderRow(
                                    label = stringResource(R.string.reverb_width),
                                    value = reverbWidth,
                                    onValueChange = {
                                        RegaReverbManager.setWidth(it)
                                        savedReverbPreset = ReverbPresetType.CUSTOM.id
                                        savedReverbWidth = it
                                    }
                                )
                                ReverbSliderRow(
                                    label = stringResource(R.string.reverb_decay),
                                    value = reverbDecay,
                                    onValueChange = {
                                        RegaReverbManager.setDecay(it)
                                        savedReverbPreset = ReverbPresetType.CUSTOM.id
                                        savedReverbDecay = it
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = RegaColor.Surface,
            title = {
                Text(
                    text = stringResource(R.string.save_as_new_preset),
                    color = RegaColor.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text(stringResource(R.string.preset_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RegaColor.PrimaryAccent,
                        focusedLabelColor = RegaColor.PrimaryAccent,
                        unfocusedBorderColor = RegaColor.BorderMuted,
                        focusedTextColor = RegaColor.TextPrimary,
                        unfocusedTextColor = RegaColor.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            viewModel.saveCustomPreset(presetName.trim())
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        color = RegaColor.PrimaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(
                        text = stringResource(android.R.string.cancel),
                        color = RegaColor.TextSecondary
                    )
                }
            }
        )
    }

    if (showRenameDialog != null) {
        val targetProfile = showRenameDialog!!
        var newName by remember { mutableStateOf(targetProfile.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            containerColor = RegaColor.Surface,
            title = {
                Text(
                    text = stringResource(R.string.rename_preset),
                    color = RegaColor.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.preset_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RegaColor.PrimaryAccent,
                        focusedLabelColor = RegaColor.PrimaryAccent,
                        unfocusedBorderColor = RegaColor.BorderMuted,
                        focusedTextColor = RegaColor.TextPrimary,
                        unfocusedTextColor = RegaColor.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameProfile(targetProfile.id, newName.trim())
                            showRenameDialog = null
                        }
                    }
                ) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        color = RegaColor.PrimaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text(
                        text = stringResource(android.R.string.cancel),
                        color = RegaColor.TextSecondary
                    )
                }
            }
        )
    }

    if (showDeleteDialog != null) {
        val targetProfile = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = RegaColor.Surface,
            title = {
                Text(
                    text = stringResource(R.string.delete_profile_desc),
                    color = RegaColor.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_profile_confirmation, targetProfile.name),
                    color = RegaColor.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(targetProfile.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(
                        text = stringResource(android.R.string.cancel),
                        color = RegaColor.TextSecondary
                    )
                }
            }
        )
    }

    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            containerColor = RegaColor.Surface,
            title = { Text(stringResource(R.string.error_title), color = RegaColor.TextPrimary) },
            text = { Text(showError ?: "", color = RegaColor.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showError = null }) {
                    Text(stringResource(android.R.string.ok), color = RegaColor.PrimaryAccent)
                }
            }
        )
    }
}

@Composable
private fun BandSliderRow(
    band: ParametricEQBand,
    enabled: Boolean,
    onGainChange: (Double) -> Unit
) {
    val freqLabel = remember(band.frequency) {
        if (band.frequency >= 1000.0) {
            val khz = band.frequency / 1000.0
            if (khz == khz.toLong().toDouble()) "${khz.toLong()} kHz" else "%.1f kHz".format(khz)
        } else {
            "${band.frequency.toLong()} Hz"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = freqLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = RegaColor.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(64.dp)
        )

        Slider(
            value = band.gain.toFloat(),
            onValueChange = {
                if (enabled) {
                    val rounded = (round(it * 2) / 2).toDouble()
                    onGainChange(rounded)
                }
            },
            valueRange = -12.0f..12.0f,
            steps = 47,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = RegaColor.PrimaryAccent,
                activeTrackColor = RegaColor.PrimaryAccent,
                inactiveTrackColor = RegaColor.SurfaceContainerHighest
            )
        )

        Box(
            modifier = Modifier
                .width(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(RegaColor.SurfaceContainer)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%+.1f".format(band.gain),
                style = MaterialTheme.typography.labelSmall,
                color = if (band.gain != 0.0) RegaColor.PrimaryAccent else RegaColor.TextSecondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReverbSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = RegaColor.TextSecondary,
            modifier = Modifier.width(96.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = RegaColor.PrimaryAccent,
                activeTrackColor = RegaColor.PrimaryAccent,
                inactiveTrackColor = RegaColor.SurfaceContainerHighest
            )
        )
        Box(
            modifier = Modifier
                .width(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(RegaColor.SurfaceContainer)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = RegaColor.PrimaryAccent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
