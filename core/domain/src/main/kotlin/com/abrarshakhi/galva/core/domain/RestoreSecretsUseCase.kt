package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.MediaRepository
import com.abrarshakhi.galva.core.model.RestoreOutcome
import com.abrarshakhi.galva.core.vault.VaultRepository

class RestoreSecretsUseCase(
    private val vault: VaultRepository,
    private val mediaRepository: MediaRepository,
) {

    suspend operator fun invoke(secretIds: Collection<Long>): RestoreOutcome {
        val outcome = vault.restore(secretIds)
        if (outcome.restoredMediaIds.isNotEmpty()) {
            mediaRepository.sync()
            runCatching { mediaRepository.setFavorite(outcome.favoriteMediaIds, favorite = true) }
        }
        return outcome
    }
}
