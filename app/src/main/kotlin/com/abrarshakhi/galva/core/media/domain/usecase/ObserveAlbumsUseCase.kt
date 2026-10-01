package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.Album
import com.abrarshakhi.galva.core.media.domain.model.AlbumSort
import com.abrarshakhi.galva.core.media.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

class ObserveAlbumsUseCase(
    private val albumRepository: AlbumRepository,
) {

    operator fun invoke(sort: AlbumSort): Flow<List<Album>> = albumRepository.observeAlbums(sort)
}
