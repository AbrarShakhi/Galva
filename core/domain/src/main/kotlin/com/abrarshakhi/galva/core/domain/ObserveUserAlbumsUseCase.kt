package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.AlbumRepository
import com.abrarshakhi.galva.core.model.Album
import kotlinx.coroutines.flow.Flow

class ObserveUserAlbumsUseCase(
    private val albumRepository: AlbumRepository,
) {

    operator fun invoke(): Flow<List<Album>> = albumRepository.observeUserAlbums()
}
