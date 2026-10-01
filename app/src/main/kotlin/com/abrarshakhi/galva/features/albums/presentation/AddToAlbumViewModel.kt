package com.abrarshakhi.galva.features.albums.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.media.domain.model.Album
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.usecase.AddToAlbumUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.CreateAlbumUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveUserAlbumsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the add-to-album sheet.
 *
 * Only user-created albums are offered. Adding to a device folder would mean moving the file on
 * disk, which is a different operation with different consequences — and Favorites already has its
 * own button in the same bar.
 */
class AddToAlbumViewModel(
    observeUserAlbums: ObserveUserAlbumsUseCase,
    private val createAlbum: CreateAlbumUseCase,
    private val addToAlbum: AddToAlbumUseCase,
) : ViewModel() {

    val albums: StateFlow<List<Album>> = observeUserAlbums().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList(),
    )

    private val _added = Channel<String>(Channel.BUFFERED)
    val added: Flow<String> = _added.receiveAsFlow()

    fun addTo(ref: AlbumRef.User, albumName: String, mediaIds: List<Long>) {
        viewModelScope.launch {
            addToAlbum(ref, mediaIds)
            _added.send(albumName)
        }
    }

    fun createAndAdd(name: String, mediaIds: List<Long>) {
        viewModelScope.launch {
            val ref = createAlbum(name) ?: return@launch
            addToAlbum(ref, mediaIds)
            _added.send(name.trim())
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
