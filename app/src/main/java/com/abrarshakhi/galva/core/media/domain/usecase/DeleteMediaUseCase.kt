package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.repository.MediaRepository

class DeleteMediaUseCase(
    private val mediaRepository: MediaRepository,
) {

    suspend operator fun invoke(ids: Collection<Long>): DeleteOutcome =
        if (ids.isEmpty()) DeleteOutcome.Deleted(count = 0) else mediaRepository.delete(ids)

    /** Called once the system delete dialog confirms, to prune the local index eagerly. */
    suspend fun confirm(ids: Collection<Long>) = mediaRepository.forgetDeleted(ids)
}
