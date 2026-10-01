package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.repository.AlbumRepository

class AddToAlbumUseCase(
    private val albumRepository: AlbumRepository,
) {

    suspend operator fun invoke(ref: AlbumRef.User, mediaIds: Collection<Long>) {
        if (mediaIds.isNotEmpty()) albumRepository.addToAlbum(ref, mediaIds)
    }
}
