package com.metrolist.music.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.eq.EqualizerService
import com.metrolist.music.eq.data.EQProfileRepository
import com.metrolist.music.eq.data.ParametricEQBand
import com.metrolist.music.eq.data.ParametricEQParser
import com.metrolist.music.eq.data.SavedEQProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class EQViewModel @Inject constructor(
    private val eqProfileRepository: EQProfileRepository,
    private val equalizerService: EqualizerService
) : ViewModel() {

    private val _state = MutableStateFlow(EQState())
    val state: StateFlow<EQState> = _state.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            eqProfileRepository.profiles.collect { _ ->
                val sortedProfiles = eqProfileRepository.getSortedProfiles()
                _state.update {
                    it.copy(profiles = sortedProfiles)
                }
            }
        }

        viewModelScope.launch {
            eqProfileRepository.activeProfile.collect { activeProfile ->
                val bands = activeProfile?.bands ?: EQProfileRepository.createBands(List(10) { 0.0 })
                val preamp = activeProfile?.preamp ?: 0.0
                _state.update {
                    it.copy(
                        activeProfileId = activeProfile?.id,
                        currentBands = bands,
                        currentPreamp = preamp,
                        isEnabled = activeProfile != null
                    )
                }
                if (activeProfile != null) {
                    equalizerService.applyProfile(activeProfile)
                }
            }
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isEnabled = enabled) }
            if (enabled) {
                val current = _state.value
                val profileToApply = current.profiles.find { it.id == current.activeProfileId }
                    ?: current.profiles.firstOrNull()
                if (profileToApply != null) {
                    equalizerService.applyProfile(profileToApply)
                    eqProfileRepository.setActiveProfile(profileToApply.id)
                }
            } else {
                equalizerService.disable()
            }
        }
    }

    fun selectProfile(profileId: String?) {
        viewModelScope.launch {
            if (profileId == null) {
                equalizerService.disable()
                eqProfileRepository.setActiveProfile(null)
                _state.update { it.copy(isEnabled = false, activeProfileId = null) }
            } else {
                val profile = _state.value.profiles.find { it.id == profileId }
                if (profile != null) {
                    val result = equalizerService.applyProfile(profile)
                    result.onSuccess {
                        eqProfileRepository.setActiveProfile(profileId)
                        _state.update {
                            it.copy(
                                isEnabled = true,
                                activeProfileId = profileId,
                                currentBands = profile.bands,
                                currentPreamp = profile.preamp
                            )
                        }
                    }.onFailure { e ->
                        _state.update { it.copy(error = e.message ?: "Unknown error") }
                    }
                }
            }
        }
    }

    fun updateBandGain(frequency: Double, newGain: Double) {
        val updatedBands = _state.value.currentBands.map { band ->
            if (band.frequency == frequency) {
                band.copy(gain = newGain)
            } else {
                band
            }
        }
        _state.update { it.copy(currentBands = updatedBands) }
        applyLiveAdjustment(updatedBands, _state.value.currentPreamp)
    }

    fun updatePreamp(newPreamp: Double) {
        _state.update { it.copy(currentPreamp = newPreamp) }
        applyLiveAdjustment(_state.value.currentBands, newPreamp)
    }

    private fun applyLiveAdjustment(bands: List<ParametricEQBand>, preamp: Double) {
        val currentProfileId = _state.value.activeProfileId
        val activeProfile = _state.value.profiles.find { it.id == currentProfileId }
        val liveProfile = SavedEQProfile(
            id = activeProfile?.id ?: "live_adjustment",
            name = activeProfile?.name ?: "Custom",
            deviceModel = activeProfile?.deviceModel ?: "Custom",
            bands = bands,
            preamp = preamp,
            isCustom = true
        )
        equalizerService.applyProfile(liveProfile)
    }

    fun resetToFlat() {
        val flatBands = EQProfileRepository.createBands(List(10) { 0.0 })
        _state.update {
            it.copy(
                currentBands = flatBands,
                currentPreamp = 0.0
            )
        }
        applyLiveAdjustment(flatBands, 0.0)
    }

    fun saveCustomPreset(name: String) {
        viewModelScope.launch {
            val customProfile = SavedEQProfile(
                id = "custom_${System.currentTimeMillis()}_${name.hashCode()}",
                name = name,
                deviceModel = name,
                bands = _state.value.currentBands,
                preamp = _state.value.currentPreamp,
                isCustom = true
            )
            eqProfileRepository.saveProfile(customProfile)
            eqProfileRepository.setActiveProfile(customProfile.id)
        }
    }

    fun renameProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            eqProfileRepository.renameProfile(profileId, newName)
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            eqProfileRepository.deleteProfile(profileId)
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            eqProfileRepository.restoreDefaults()
            val defaultProfile = eqProfileRepository.activeProfile.value
            if (defaultProfile != null) {
                equalizerService.applyProfile(defaultProfile)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun importCustomProfile(
        fileName: String,
        inputStream: InputStream,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val content = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                val parametricEQ = ParametricEQParser.parseText(content)
                val validationErrors = ParametricEQParser.validate(parametricEQ)
                if (validationErrors.isNotEmpty()) {
                    onError(Exception("Invalid EQ file: ${validationErrors.first()}"))
                    return@launch
                }

                val profileName = fileName.removeSuffix(".txt")
                eqProfileRepository.importCustomProfile(profileName, parametricEQ)

                _state.update { it.copy(importStatus = "Successfully imported $profileName") }
                onSuccess()
            } catch (e: Exception) {
                onError(Exception("Failed to import EQ profile: ${e.message}"))
            }
        }
    }
}
