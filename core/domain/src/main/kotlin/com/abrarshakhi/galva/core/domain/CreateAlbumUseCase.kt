package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.AlbumRepository
import com.abrarshakhi.galva.core.model.AlbumRef

class CreateAlbumUseCase(
    private val albumRepository: AlbumRepository,
) {

    suspend operator fun invoke(name: String): AlbumRef.User? {
        val trimmed = name.trim()
        return if (trimmed.isEmpty()) null else albumRepository.createAlbum(trimmed)
    }
}
