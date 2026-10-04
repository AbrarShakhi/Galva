package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.MediaRepository

class ToggleFavoriteUseCase(
    private val mediaRepository: MediaRepository,
) {

    suspend operator fun invoke(ids: Collection<Long>, favorite: Boolean) {
        if (ids.isNotEmpty()) mediaRepository.setFavorite(ids, favorite)
    }
}
