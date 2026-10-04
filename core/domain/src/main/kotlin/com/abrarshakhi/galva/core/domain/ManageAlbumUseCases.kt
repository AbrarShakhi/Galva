package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.AlbumRepository
import com.abrarshakhi.galva.core.model.AlbumRef

class RenameAlbumUseCase(
    private val albumRepository: AlbumRepository,
) {

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
