package com.xfyc.music.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xfyc.music.data.repository.SettingsRepository
import com.xfyc.music.domain.model.PlayMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val defaultPlayMode: StateFlow<PlayMode> = settingsRepository.defaultPlayMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayMode.SEQUENTIAL)

    val scanOnStartup: StateFlow<Boolean> = settingsRepository.scanOnStartup
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoPauseHeadset: StateFlow<Boolean> = settingsRepository.autoPauseHeadset
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val playModes = PlayMode.entries.map { it.name }

    fun setDefaultPlayMode(mode: PlayMode) {
        viewModelScope.launch { settingsRepository.setDefaultPlayMode(mode) }
    }

    fun setScanOnStartup(scan: Boolean) {
        viewModelScope.launch { settingsRepository.setScanOnStartup(scan) }
    }

    fun setAutoPauseHeadset(pause: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPauseHeadset(pause) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
}
