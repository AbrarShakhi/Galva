package com.abrarshakhi.galva.features.albums.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.common.mvi.MviViewModel
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveAlbumsUseCase
import com.abrarshakhi.galva.core.settings.domain.AlbumViewType
import com.abrarshakhi.galva.core.settings.domain.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Sort and view type are read back from settings rather than held locally, so the choice the user
 * makes here is the same one they see after a restart — the behaviour Ente's albums tab has.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModel(
    observeAlbums: ObserveAlbumsUseCase,
    private val settingsRepository: SettingsRepository,
) : MviViewModel<AlbumsUiState, AlbumsIntent, AlbumsEffect>(AlbumsUiState()) {

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
            is AlbumsIntent.AlbumTapped -> sendEffect(AlbumsEffect.OpenAlbum(intent.album.ref))

            is AlbumsIntent.SortSelected -> settingsRepository.setAlbumSort(intent.sort)

            AlbumsIntent.ViewTypeToggled -> settingsRepository.setAlbumViewType(
                if (currentState.viewType == AlbumViewType.GRID) AlbumViewType.LIST
                else AlbumViewType.GRID
            )
        }
    }
}
