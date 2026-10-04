package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.MediaRepository
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.MediaSource
import kotlinx.coroutines.flow.Flow

class ObserveMediaUseCase(
    private val mediaRepository: MediaRepository,
) {

    operator fun invoke(source: MediaSource): Flow<List<MediaItem>> =
        mediaRepository.observeMedia(source)
}
