package com.sonara.music.ui.screens.equalizer

import com.sonara.music.eq.data.ParametricEQBand
import com.sonara.music.eq.data.SavedEQProfile

data class EQState(
    val isEnabled: Boolean = true,
    val profiles: List<SavedEQProfile> = emptyList(),
    val activeProfileId: String? = null,
    val currentBands: List<ParametricEQBand> = emptyList(),
    val currentPreamp: Double = 0.0,
    val importStatus: String? = null,
    val error: String? = null
)
