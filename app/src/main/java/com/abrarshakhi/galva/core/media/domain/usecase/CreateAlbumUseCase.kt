package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.repository.AlbumRepository

class CreateAlbumUseCase(
    private val albumRepository: AlbumRepository,
) {

    /**
     * @return the new album, or null when [name] is blank — callers treat that as "nothing to do"
     * rather than an error, so an empty text field cannot create an unnameable album.
     */
    suspend operator fun invoke(name: String): AlbumRef.User? {
        val trimmed = name.trim()
        return if (trimmed.isEmpty()) null else albumRepository.createAlbum(trimmed)
    }
}
