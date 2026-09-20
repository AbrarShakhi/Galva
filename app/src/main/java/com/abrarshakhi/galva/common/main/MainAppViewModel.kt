package com.abrarshakhi.galva.common.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.media.data.mediastore.MediaStoreObserver
import com.abrarshakhi.galva.core.media.domain.usecase.SyncMediaUseCase
import com.abrarshakhi.galva.core.permission.MediaAccess
import com.abrarshakhi.galva.core.settings.domain.SettingsRepository
import com.abrarshakhi.galva.core.settings.domain.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainAppViewModel(
    private val syncMedia: SyncMediaUseCase,
    mediaStoreObserver: MediaStoreObserver,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _access = MutableStateFlow(MediaAccess.DENIED)
    val access: StateFlow<MediaAccess> = _access.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = settingsRepository.settings.map { it.themeMode }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ThemeMode.SYSTEM,
    )

    init {
        _access.filter { it.canReadMedia }.flatMapLatest { mediaStoreObserver.changes() }
            .onEach { syncMedia() }.launchIn(viewModelScope)
    }

    fun onAccessChanged(access: MediaAccess) {
        val previous = _access.value
        _access.value = access
        if (access.canReadMedia && previous != access) refresh()
    }

    fun refresh() {
        viewModelScope.launch { syncMedia() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
