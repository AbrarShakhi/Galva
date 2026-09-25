package com.abrarshakhi.galva.core.vault.domain.usecase

import com.abrarshakhi.galva.core.media.domain.repository.MediaRepository
import com.abrarshakhi.galva.core.vault.domain.model.RestoreOutcome
import com.abrarshakhi.galva.core.vault.domain.repository.VaultRepository

/**
 * Puts items back into the gallery, favourites included.
 *
 * Restored files get new MediaStore ids, and a favourite can only point at a row the index already
 * has, so the index is synced before the flags are re-applied.
 */
class RestoreSecretsUseCase(
    private val vault: VaultRepository,
    private val mediaRepository: MediaRepository,
) {

    suspend operator fun invoke(secretIds: Collection<Long>): RestoreOutcome {
        val outcome = vault.restore(secretIds)
        if (outcome.restoredMediaIds.isNotEmpty()) {
            mediaRepository.sync()
            // Losing a favourite flag is not worth failing a restore that already succeeded.
            runCatching { mediaRepository.setFavorite(outcome.favoriteMediaIds, favorite = true) }
        }
        return outcome
    }
}
