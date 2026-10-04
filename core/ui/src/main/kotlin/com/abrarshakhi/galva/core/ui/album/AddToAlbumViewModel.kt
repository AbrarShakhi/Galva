package com.abrarshakhi.galva.core.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.domain.AddToAlbumUseCase
import com.abrarshakhi.galva.core.domain.CreateAlbumUseCase
import com.abrarshakhi.galva.core.domain.ObserveUserAlbumsUseCase
import com.abrarshakhi.galva.core.model.Album
import com.abrarshakhi.galva.core.model.AlbumRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddToAlbumUiState(
    val albums: List<Album> = emptyList(),
    val addedTo: String? = null,
)

class AddToAlbumViewModel(
    observeUserAlbums: ObserveUserAlbumsUseCase,
    private val createAlbum: CreateAlbumUseCase,
    private val addToAlbum: AddToAlbumUseCase,
) : ViewModel() {

    private val addedTo = MutableStateFlow<String?>(null)

    val state: StateFlow<AddToAlbumUiState> =
        combine(observeUserAlbums(), addedTo, ::AddToAlbumUiState).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = AddToAlbumUiState(),
        )

    fun addTo(ref: AlbumRef.User, albumName: String, mediaIds: List<Long>) {
        viewModelScope.launch {
            addToAlbum(ref, mediaIds)
            addedTo.value = albumName
        }
    }

    fun createAndAdd(name: String, mediaIds: List<Long>) {
        viewModelScope.launch {
            val ref = createAlbum(name) ?: return@launch
            addToAlbum(ref, mediaIds)
            addedTo.value = name.trim()
        }
    }

    fun onAddedHandled() {
        addedTo.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
