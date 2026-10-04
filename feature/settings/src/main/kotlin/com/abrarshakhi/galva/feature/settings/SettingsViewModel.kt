package com.abrarshakhi.galva.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.data.repository.SettingsRepository
import com.abrarshakhi.galva.core.model.AlbumViewType
import com.abrarshakhi.galva.core.model.AppSettings
import com.abrarshakhi.galva.core.model.ThemeColor
import com.abrarshakhi.galva.core.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AppSettings(),
    )

    fun setThemeMode(mode: ThemeMode) = update { setThemeMode(mode) }

    fun setThemeColor(color: ThemeColor) = update { setThemeColor(color) }

    fun setPureBlack(enabled: Boolean) = update { setPureBlack(enabled) }

    fun setGalleryColumns(columns: Int) = update { setGalleryColumns(columns) }

    fun setAlbumViewType(viewType: AlbumViewType) = update { setAlbumViewType(viewType) }

    private fun update(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
