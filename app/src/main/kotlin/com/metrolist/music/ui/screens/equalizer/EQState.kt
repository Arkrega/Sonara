package com.metrolist.music.ui.screens.equalizer

import com.metrolist.music.eq.data.ParametricEQBand
import com.metrolist.music.eq.data.SavedEQProfile

data class EQState(
    val isEnabled: Boolean = true,
    val profiles: List<SavedEQProfile> = emptyList(),
    val activeProfileId: String? = null,
    val currentBands: List<ParametricEQBand> = emptyList(),
    val currentPreamp: Double = 0.0,
    val importStatus: String? = null,
    val error: String? = null
)
