package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.repository.MediaRepository

class ToggleFavoriteUseCase(
    private val mediaRepository: MediaRepository,
) {

    suspend operator fun invoke(ids: Collection<Long>, favorite: Boolean) {
        if (ids.isNotEmpty()) mediaRepository.setFavorite(ids, favorite)
    }
}
