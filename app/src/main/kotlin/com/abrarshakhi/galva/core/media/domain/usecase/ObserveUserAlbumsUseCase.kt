package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.Album
import com.abrarshakhi.galva.core.media.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserAlbumsUseCase(
    private val albumRepository: AlbumRepository,
) {

    operator fun invoke(): Flow<List<Album>> = albumRepository.observeUserAlbums()
}
