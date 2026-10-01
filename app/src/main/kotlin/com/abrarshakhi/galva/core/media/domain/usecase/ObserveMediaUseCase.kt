package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class ObserveMediaUseCase(
    private val mediaRepository: MediaRepository,
) {

    operator fun invoke(source: MediaSource): Flow<List<MediaItem>> =
        mediaRepository.observeMedia(source)
}
