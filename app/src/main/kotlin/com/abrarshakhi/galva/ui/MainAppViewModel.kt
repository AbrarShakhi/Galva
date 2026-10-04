package com.abrarshakhi.galva.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.data.repository.SettingsRepository
import com.abrarshakhi.galva.core.domain.SyncMediaUseCase
import com.abrarshakhi.galva.core.mediastore.MediaAccess
import com.abrarshakhi.galva.core.mediastore.MediaStoreObserver
import com.abrarshakhi.galva.core.model.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
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

    val settings: StateFlow<AppSettings?> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
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
}
