package com.abrarshakhi.galva.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.media.domain.model.AlbumSort
import com.abrarshakhi.galva.core.settings.domain.AlbumViewType
import com.abrarshakhi.galva.core.settings.domain.AppSettings
import com.abrarshakhi.galva.core.settings.domain.SettingsRepository
import com.abrarshakhi.galva.core.settings.domain.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Plain [ViewModel] rather than an MVI one: the settings drawer has no intermediate state of its
 * own — every control writes straight through to storage and re-reads the same flow.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AppSettings(),
    )

    fun setThemeMode(mode: ThemeMode) = update { setThemeMode(mode) }

    fun setGalleryColumns(columns: Int) = update { setGalleryColumns(columns) }

    fun setAlbumViewType(viewType: AlbumViewType) = update { setAlbumViewType(viewType) }

    fun setAlbumSort(sort: AlbumSort) = update { setAlbumSort(sort) }

    private fun update(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
