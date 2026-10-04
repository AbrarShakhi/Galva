package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.AlbumRepository
import com.abrarshakhi.galva.core.model.Album
import com.abrarshakhi.galva.core.model.AlbumSort
import kotlinx.coroutines.flow.Flow

class ObserveAlbumsUseCase(
    private val albumRepository: AlbumRepository,
) {

    operator fun invoke(sort: AlbumSort): Flow<List<Album>> = albumRepository.observeAlbums(sort)
}
