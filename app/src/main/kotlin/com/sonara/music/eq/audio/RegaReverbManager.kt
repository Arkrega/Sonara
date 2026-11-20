package com.sonara.music.eq.audio

import androidx.media3.common.util.UnstableApi
import com.sonara.music.eq.data.ReverbConfig
import com.sonara.music.eq.data.ReverbPresetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

@UnstableApi
object RegaReverbManager {
    const val PRESET_NONE: Short = 0
    const val PRESET_SMALL_ROOM: Short = 1
    const val PRESET_MEDIUM_ROOM: Short = 2
    const val PRESET_LARGE_ROOM: Short = 3
    const val PRESET_MEDIUM_HALL: Short = 4
    const val PRESET_LARGE_HALL: Short = 5
    const val PRESET_PLATE: Short = 6
    const val PRESET_CATHEDRAL: Short = 7
    const val PRESET_STUDIO: Short = 8
    const val PRESET_CUSTOM: Short = 9

    private val lock = Any()
    private val activeProcessors = Collections.synchronizedList(mutableListOf<RegaReverbAudioProcessor>())

    private val _config = MutableStateFlow(ReverbConfig())
    val config: StateFlow<ReverbConfig> = _config.asStateFlow()

    private val _isSupported = MutableStateFlow(true)
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    private val _isUserEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isUserEnabled.asStateFlow()

    private val _isEffectActive = MutableStateFlow(false)
    val isEffectActive: StateFlow<Boolean> = _isEffectActive.asStateFlow()

    private val _hasControl = MutableStateFlow(true)
    val hasControl: StateFlow<Boolean> = _hasControl.asStateFlow()

    private val _currentPreset = MutableStateFlow(ReverbPresetType.SMALL_ROOM)
    val currentPreset: StateFlow<ReverbPresetType> = _currentPreset.asStateFlow()

    private val _roomSize = MutableStateFlow(ReverbConfig.SMALL_ROOM.roomSize)
    val roomSize: StateFlow<Float> = _roomSize.asStateFlow()

    private val _damping = MutableStateFlow(ReverbConfig.SMALL_ROOM.damping)
    val damping: StateFlow<Float> = _damping.asStateFlow()

    private val _wetLevel = MutableStateFlow(ReverbConfig.SMALL_ROOM.wetLevel)
    val wetLevel: StateFlow<Float> = _wetLevel.asStateFlow()

    private val _dryLevel = MutableStateFlow(ReverbConfig.SMALL_ROOM.dryLevel)
    val dryLevel: StateFlow<Float> = _dryLevel.asStateFlow()

    private val _width = MutableStateFlow(ReverbConfig.SMALL_ROOM.width)
    val width: StateFlow<Float> = _width.asStateFlow()

    private val _decay = MutableStateFlow(ReverbConfig.SMALL_ROOM.decay)
    val decay: StateFlow<Float> = _decay.asStateFlow()

    fun addAudioProcessor(processor: RegaReverbAudioProcessor) {
        synchronized(lock) {
            if (!activeProcessors.contains(processor)) {
                activeProcessors.add(processor)
            }
            processor.updateConfig(_config.value)
            _isEffectActive.value = _isUserEnabled.value && activeProcessors.isNotEmpty()
        }
    }

    fun removeAudioProcessor(processor: RegaReverbAudioProcessor) {
        synchronized(lock) {
            activeProcessors.remove(processor)
            _isEffectActive.value = _isUserEnabled.value && activeProcessors.isNotEmpty()
        }
    }

    fun setEnabled(enabled: Boolean, unusedPlayer: Any? = null) {
        _isUserEnabled.value = enabled
        updateInternalConfig { it.copy(enabled = enabled) }
    }

    fun setPreset(type: ReverbPresetType) {
        _currentPreset.value = type
        if (type == ReverbPresetType.CUSTOM) {
            updateInternalConfig { it.copy(presetType = type) }
        } else {
            val base = ReverbConfig.forType(type)
            updateInternalConfig {
                it.copy(
                    presetType = type,
                    roomSize = base.roomSize,
                    damping = base.damping,
                    wetLevel = base.wetLevel,
                    dryLevel = base.dryLevel,
                    width = base.width,
                    decay = base.decay
                )
            }
        }
    }

    fun setPreset(legacyPresetId: Short) {
        val type = when (legacyPresetId) {
            PRESET_SMALL_ROOM -> ReverbPresetType.SMALL_ROOM
            PRESET_MEDIUM_ROOM -> ReverbPresetType.MEDIUM_ROOM
            PRESET_LARGE_ROOM, PRESET_MEDIUM_HALL, PRESET_LARGE_HALL -> ReverbPresetType.LARGE_HALL
            PRESET_PLATE -> ReverbPresetType.PLATE
            PRESET_CATHEDRAL -> ReverbPresetType.CATHEDRAL
            PRESET_STUDIO -> ReverbPresetType.STUDIO
            PRESET_CUSTOM -> ReverbPresetType.CUSTOM
            else -> ReverbPresetType.SMALL_ROOM
        }
        setPreset(type)
    }

    fun setRoomSize(value: Float) {
        _roomSize.value = value
        updateInternalConfig { it.copy(roomSize = value, presetType = ReverbPresetType.CUSTOM) }
        _currentPreset.value = ReverbPresetType.CUSTOM
    }

    fun setDamping(value: Float) {
        _damping.value = value
        updateInternalConfig { it.copy(damping = value, presetType = ReverbPresetType.CUSTOM) }
        _currentPreset.value = ReverbPresetType.CUSTOM
    }

    fun setWetLevel(value: Float) {
        _wetLevel.value = value
        updateInternalConfig { it.copy(wetLevel = value, presetType = ReverbPresetType.CUSTOM) }
        _currentPreset.value = ReverbPresetType.CUSTOM
    }

    fun setDryLevel(value: Float) {
        _dryLevel.value = value
        updateInternalConfig { it.copy(dryLevel = value, presetType = ReverbPresetType.CUSTOM) }
        _currentPreset.value = ReverbPresetType.CUSTOM
    }

    fun setWidth(value: Float) {
        _width.value = value
        updateInternalConfig { it.copy(width = value, presetType = ReverbPresetType.CUSTOM) }
        _currentPreset.value = ReverbPresetType.CUSTOM
    }

    fun setDecay(value: Float) {
        _decay.value = value
        updateInternalConfig { it.copy(decay = value, presetType = ReverbPresetType.CUSTOM) }
        _currentPreset.value = ReverbPresetType.CUSTOM
    }

    fun applyFullConfig(newConfig: ReverbConfig) {
        _isUserEnabled.value = newConfig.enabled
        _currentPreset.value = newConfig.presetType
        _roomSize.value = newConfig.roomSize
        _damping.value = newConfig.damping
        _wetLevel.value = newConfig.wetLevel
        _dryLevel.value = newConfig.dryLevel
        _width.value = newConfig.width
        _decay.value = newConfig.decay

        synchronized(lock) {
            _config.value = newConfig
            activeProcessors.forEach { it.updateConfig(newConfig) }
            _isEffectActive.value = newConfig.enabled && activeProcessors.isNotEmpty()
        }
    }

    private inline fun updateInternalConfig(transform: (ReverbConfig) -> ReverbConfig) {
        synchronized(lock) {
            val updated = transform(_config.value)
            _config.value = updated
            _roomSize.value = updated.roomSize
            _damping.value = updated.damping
            _wetLevel.value = updated.wetLevel
            _dryLevel.value = updated.dryLevel
            _width.value = updated.width
            _decay.value = updated.decay
            activeProcessors.forEach { it.updateConfig(updated) }
            _isEffectActive.value = updated.enabled && activeProcessors.isNotEmpty()
        }
    }

    fun attachPlayer(unusedPlayer: Any? = null) {}

    fun detachPlayer(unusedPlayer: Any? = null) {}

    fun release(unusedPlayer: Any? = null) {
        synchronized(lock) {
            activeProcessors.clear()
            _isEffectActive.value = false
        }
    }
}

typealias SonaraReverbManager = RegaReverbManager
