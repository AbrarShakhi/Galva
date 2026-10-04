package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.MediaRepository
import com.abrarshakhi.galva.core.model.DeleteOutcome

class DeleteMediaUseCase(
    private val mediaRepository: MediaRepository,
) {

    suspend operator fun invoke(ids: Collection<Long>): DeleteOutcome =
        if (ids.isEmpty()) DeleteOutcome.Deleted(count = 0) else mediaRepository.delete(ids)

    suspend fun confirm(ids: Collection<Long>) = mediaRepository.forgetDeleted(ids)
}
