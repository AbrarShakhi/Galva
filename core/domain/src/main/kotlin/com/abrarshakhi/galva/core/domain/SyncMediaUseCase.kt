package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.MediaRepository

class SyncMediaUseCase(
    private val mediaRepository: MediaRepository,
) {

    suspend operator fun invoke() = mediaRepository.sync()
}
