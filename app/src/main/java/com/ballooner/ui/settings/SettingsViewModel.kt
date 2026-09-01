package com.ballooner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.settings.SettingsRepository
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.TextSizeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.observeSettings()
        .map { SettingsUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setDefaultFont(font: BalloonFont) {
        viewModelScope.launch { settingsRepository.setDefaultFont(font) }
    }

    fun setHideFontSelector(hide: Boolean) {
        viewModelScope.launch { settingsRepository.setHideFontSelector(hide) }
    }

    fun setTextSizeMode(mode: TextSizeMode) {
        viewModelScope.launch { settingsRepository.setTextSizeMode(mode) }
    }

    fun setLayoutColumns(columns: Int) {
        viewModelScope.launch { settingsRepository.setLayoutColumns(columns) }
    }
}
