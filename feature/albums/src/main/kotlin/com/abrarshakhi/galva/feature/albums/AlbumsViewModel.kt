package com.abrarshakhi.galva.feature.albums

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.data.repository.SettingsRepository
import com.abrarshakhi.galva.core.domain.ObserveAlbumsUseCase
import com.abrarshakhi.galva.core.model.AlbumViewType
import com.abrarshakhi.galva.core.ui.mvi.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModel(
    observeAlbums: ObserveAlbumsUseCase,
    private val settingsRepository: SettingsRepository,
) : MviViewModel<AlbumsUiState, AlbumsIntent>(AlbumsUiState()) {

    init {
        settingsRepository.settings
            .onEach { settings ->
                setState { copy(sort = settings.albumSort, viewType = settings.albumViewType) }
            }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .map { it.albumSort }
            .distinctUntilChanged()
            .flatMapLatest { sort -> observeAlbums(sort) }
            .onEach { albums -> setState { copy(albums = albums, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    override suspend fun reduce(intent: AlbumsIntent) {
        when (intent) {
            is AlbumsIntent.SortSelected -> settingsRepository.setAlbumSort(intent.sort)

            AlbumsIntent.ViewTypeToggled -> settingsRepository.setAlbumViewType(
                if (currentState.viewType == AlbumViewType.GRID) AlbumViewType.LIST
                else AlbumViewType.GRID
            )
        }
    }
}
