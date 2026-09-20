package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.repository.AlbumRepository

class RenameAlbumUseCase(
    private val albumRepository: AlbumRepository,
) {

    /** A blank name is treated as "leave it alone" rather than an error, as when creating one. */
    suspend operator fun invoke(ref: AlbumRef.User, name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) albumRepository.renameAlbum(ref, trimmed)
    }
}

class DeleteAlbumUseCase(
    private val albumRepository: AlbumRepository,
) {

    suspend operator fun invoke(ref: AlbumRef.User) = albumRepository.deleteAlbum(ref)
}

class RemoveFromAlbumUseCase(
    private val albumRepository: AlbumRepository,
) {

    suspend operator fun invoke(ref: AlbumRef.User, mediaIds: Collection<Long>) {
        if (mediaIds.isNotEmpty()) albumRepository.removeFromAlbum(ref, mediaIds)
    }
}
