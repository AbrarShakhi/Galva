package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.AlbumRepository
import com.abrarshakhi.galva.core.model.AlbumRef

class AddToAlbumUseCase(
    private val albumRepository: AlbumRepository,
) {

    suspend operator fun invoke(ref: AlbumRef.User, mediaIds: Collection<Long>) {
        if (mediaIds.isNotEmpty()) albumRepository.addToAlbum(ref, mediaIds)
    }
}
