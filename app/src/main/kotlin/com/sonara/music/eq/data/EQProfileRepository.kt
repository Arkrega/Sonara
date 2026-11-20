package com.sonara.music.eq.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SavedEQProfile(
    val id: String,
    val name: String,
    val deviceModel: String,
    val bands: List<ParametricEQBand>,
    val preamp: Double = 0.0,
    val source: String = "unknown",
    val rig: String = "unknown",
    val isCustom: Boolean = false,
    val isActive: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Singleton
class EQProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nanosonic_eq_profiles",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val _profiles = MutableStateFlow<List<SavedEQProfile>>(emptyList())
    val profiles: StateFlow<List<SavedEQProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<SavedEQProfile?>(null)
    val activeProfile: StateFlow<SavedEQProfile?> = _activeProfile.asStateFlow()

    companion object {
        private const val TAG = "EQProfileRepository"
        private const val KEY_PROFILES = "eq_profiles"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"

        val STANDARD_FREQUENCIES = listOf(
            31.0, 63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0
        )

        fun createBands(gains: List<Double>): List<ParametricEQBand> {
            return STANDARD_FREQUENCIES.mapIndexed { index, freq ->
                ParametricEQBand(
                    frequency = freq,
                    gain = gains.getOrElse(index) { 0.0 },
                    q = 1.41,
                    filterType = FilterType.PK,
                    enabled = true
                )
            }
        }

        fun getDefaultPresets(): List<SavedEQProfile> {
            return listOf(
                SavedEQProfile(
                    id = "builtin_flat",
                    name = "Flat",
                    deviceModel = "Flat",
                    bands = createBands(listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)),
                    preamp = 0.0,
                    source = "Sonara System",
                    isCustom = false
                ),
                SavedEQProfile(
                    id = "builtin_rock",
                    name = "Rock",
                    deviceModel = "Rock",
                    bands = createBands(listOf(4.5, 3.0, -1.0, -2.0, -1.0, 1.0, 3.0, 4.0, 4.5, 4.5)),
                    preamp = -2.0,
                    source = "Sonara System",
                    isCustom = false
                ),
                SavedEQProfile(
                    id = "builtin_pop",
                    name = "Pop",
                    deviceModel = "Pop",
                    bands = createBands(listOf(-1.5, -1.0, 2.0, 3.5, 4.0, 3.0, 1.0, -1.0, -1.5, -2.0)),
                    preamp = -1.5,
                    source = "Sonara System",
                    isCustom = false
                ),
                SavedEQProfile(
                    id = "builtin_jazz",
                    name = "Jazz",
                    deviceModel = "Jazz",
                    bands = createBands(listOf(3.5, 2.5, 1.0, 1.5, -1.5, -1.5, 0.0, 1.5, 2.5, 3.5)),
                    preamp = -1.5,
                    source = "Sonara System",
                    isCustom = false
                ),
                SavedEQProfile(
                    id = "builtin_classical",
                    name = "Classical",
                    deviceModel = "Classical",
                    bands = createBands(listOf(4.5, 3.5, 2.5, 2.0, -1.5, -1.5, 0.0, 2.0, 3.0, 3.5)),
                    preamp = -2.0,
                    source = "Sonara System",
                    isCustom = false
                ),
                SavedEQProfile(
                    id = "builtin_bass_boost",
                    name = "Bass Boost",
                    deviceModel = "Bass Boost",
                    bands = createBands(listOf(6.0, 5.5, 4.0, 2.5, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)),
                    preamp = -3.0,
                    source = "Sonara System",
                    isCustom = false
                ),
                SavedEQProfile(
                    id = "builtin_vocal",
                    name = "Vocal",
                    deviceModel = "Vocal",
                    bands = createBands(listOf(-2.0, -2.5, -1.0, 1.5, 3.5, 4.0, 3.5, 1.5, 0.0, -1.5)),
                    preamp = -1.5,
                    source = "Sonara System",
                    isCustom = false
                )
            )
        }
    }

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        try {
            val profilesJson = prefs.getString(KEY_PROFILES, null)
            val defaults = getDefaultPresets()
            if (profilesJson != null) {
                val loadedProfiles = json.decodeFromString<List<SavedEQProfile>>(profilesJson)
                val merged = defaults.toMutableList()
                loadedProfiles.forEach { saved ->
                    if (saved.isCustom) {
                        merged.add(saved)
                    }
                }
                _profiles.value = merged

                val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
                _activeProfile.value = merged.find { it.id == activeId }
            } else {
                _profiles.value = defaults
                _activeProfile.value = defaults.firstOrNull()
                prefs.edit {
                    putString(KEY_PROFILES, json.encodeToString<List<SavedEQProfile>>(defaults))
                    putString(KEY_ACTIVE_PROFILE_ID, defaults.first().id)
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error loading EQ profiles")
            val defaults = getDefaultPresets()
            _profiles.value = defaults
            _activeProfile.value = defaults.firstOrNull()
        }
    }

    suspend fun saveProfile(profile: SavedEQProfile) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()
        val existingIndex = currentProfiles.indexOfFirst { it.id == profile.id }

        if (existingIndex >= 0) {
            currentProfiles[existingIndex] = profile
        } else {
            currentProfiles.add(profile)
        }

        if (profile.id == _activeProfile.value?.id) {
            _activeProfile.value = profile
        }

        val customOnly = currentProfiles.filter { it.isCustom }
        val profilesJson = json.encodeToString<List<SavedEQProfile>>(customOnly)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        _profiles.value = currentProfiles
    }

    suspend fun saveProfiles(profiles: List<SavedEQProfile>) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()
        val activeId = _activeProfile.value?.id

        profiles.forEach { newProfile ->
            val existingIndex = currentProfiles.indexOfFirst { it.id == newProfile.id }
            if (existingIndex >= 0) {
                currentProfiles[existingIndex] = newProfile
            } else {
                currentProfiles.add(newProfile)
            }
            if (newProfile.id == activeId) {
                _activeProfile.value = newProfile
            }
        }

        val customOnly = currentProfiles.filter { it.isCustom }
        val profilesJson = json.encodeToString<List<SavedEQProfile>>(customOnly)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        _profiles.value = currentProfiles
    }

    suspend fun renameProfile(profileId: String, newName: String) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()
        val index = currentProfiles.indexOfFirst { it.id == profileId }
        if (index >= 0) {
            val old = currentProfiles[index]
            val updated = old.copy(name = newName, deviceModel = newName)
            currentProfiles[index] = updated
            if (_activeProfile.value?.id == profileId) {
                _activeProfile.value = updated
            }
            val customOnly = currentProfiles.filter { it.isCustom }
            val profilesJson = json.encodeToString<List<SavedEQProfile>>(customOnly)
            prefs.edit { putString(KEY_PROFILES, profilesJson) }
            _profiles.value = currentProfiles
        }
    }

    suspend fun deleteProfile(profileId: String) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()
        currentProfiles.removeAll { it.id == profileId && it.isCustom }

        val customOnly = currentProfiles.filter { it.isCustom }
        val profilesJson = json.encodeToString<List<SavedEQProfile>>(customOnly)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        if (_activeProfile.value?.id == profileId) {
            val fallback = getDefaultPresets().first()
            _activeProfile.value = fallback
            prefs.edit { putString(KEY_ACTIVE_PROFILE_ID, fallback.id) }
        }

        _profiles.value = currentProfiles
    }

    suspend fun restoreDefaults() = withContext(Dispatchers.IO) {
        val defaults = getDefaultPresets()
        prefs.edit {
            remove(KEY_PROFILES)
            putString(KEY_ACTIVE_PROFILE_ID, defaults.first().id)
        }
        _profiles.value = defaults
        _activeProfile.value = defaults.first()
    }

    suspend fun setActiveProfile(profileId: String?) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value

        if (profileId == null) {
            _activeProfile.value = null
            prefs.edit { remove(KEY_ACTIVE_PROFILE_ID) }
        } else {
            val profile = currentProfiles.find { it.id == profileId }
            _activeProfile.value = profile
            prefs.edit { putString(KEY_ACTIVE_PROFILE_ID, profileId) }
        }
    }

    suspend fun importCustomProfile(
        name: String,
        parametricEQ: ParametricEQ
    ) = withContext(Dispatchers.IO) {
        val id = "custom_${System.currentTimeMillis()}_${name.hashCode()}"

        val customProfile = SavedEQProfile(
            id = id,
            name = name,
            deviceModel = name,
            bands = parametricEQ.bands,
            preamp = parametricEQ.preamp,
            isActive = false,
            isCustom = true
        )

        saveProfile(customProfile)
    }

    fun getSortedProfiles(): List<SavedEQProfile> {
        val defaults = _profiles.value.filter { !it.isCustom }
        val customs = _profiles.value.filter { it.isCustom }.sortedByDescending { it.addedTimestamp }
        return defaults + customs
    }
}
