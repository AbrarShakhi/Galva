package com.abrarshakhi.galva.feature.albums

import com.abrarshakhi.galva.core.model.Album
import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.model.AlbumSort
import com.abrarshakhi.galva.core.model.AlbumViewType
import com.abrarshakhi.galva.core.ui.mvi.UiIntent
import com.abrarshakhi.galva.core.ui.mvi.UiState

data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    val viewType: AlbumViewType = AlbumViewType.GRID,
    val sort: AlbumSort = AlbumSort.RECENT_FIRST,
    val isLoading: Boolean = true,
) : UiState {

    val visibleAlbums: List<Album>
        get() = albums.filter { it.itemCount > 0 || it.ref !is AlbumRef.Device }

    val isEmpty: Boolean get() = !isLoading && visibleAlbums.none { it.itemCount > 0 }
}

sealed interface AlbumsIntent : UiIntent {

    data class SortSelected(val sort: AlbumSort) : AlbumsIntent

    data object ViewTypeToggled : AlbumsIntent
}
