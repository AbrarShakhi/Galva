package com.abrarshakhi.galva.features.albums.presentation

import com.abrarshakhi.galva.common.mvi.UiEffect
import com.abrarshakhi.galva.common.mvi.UiIntent
import com.abrarshakhi.galva.common.mvi.UiState
import com.abrarshakhi.galva.core.media.domain.model.Album
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.AlbumSort
import com.abrarshakhi.galva.core.settings.domain.AlbumViewType

data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    val viewType: AlbumViewType = AlbumViewType.GRID,
    val sort: AlbumSort = AlbumSort.RECENT_FIRST,
    val isLoading: Boolean = true,
) : UiState {

    /**
      * Empty device folders cannot exist, so an empty one is stale. App-owned albums are kept
      * regardless: a freshly created album has nothing in it yet and hiding it would look broken.
      */
    val visibleAlbums: List<Album>
        get() = albums.filter { it.itemCount > 0 || it.ref !is AlbumRef.Device }

    val isEmpty: Boolean get() = !isLoading && visibleAlbums.none { it.itemCount > 0 }
}

sealed interface AlbumsIntent : UiIntent {

    data class AlbumTapped(val album: Album) : AlbumsIntent

    data class SortSelected(val sort: AlbumSort) : AlbumsIntent

    data object ViewTypeToggled : AlbumsIntent
}

sealed interface AlbumsEffect : UiEffect {

    data class OpenAlbum(val albumRef: AlbumRef) : AlbumsEffect
}
