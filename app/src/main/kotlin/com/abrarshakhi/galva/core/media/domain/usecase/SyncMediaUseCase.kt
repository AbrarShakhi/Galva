package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.repository.MediaRepository

class SyncMediaUseCase(
    private val mediaRepository: MediaRepository,
) {

    suspend operator fun invoke() = mediaRepository.sync()
}
