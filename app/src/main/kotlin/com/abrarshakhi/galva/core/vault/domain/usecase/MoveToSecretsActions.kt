package com.abrarshakhi.galva.core.vault.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.usecase.MediaSelectionActions
import com.abrarshakhi.galva.core.vault.domain.model.AddToVaultOutcome
import com.abrarshakhi.galva.core.vault.domain.model.VaultState
import com.abrarshakhi.galva.core.vault.domain.repository.VaultRepository

/**
 * "Move to Secrets" from the timeline, an album, search results or the viewer.
 *
 * A move is two steps with the system delete dialog in between: [start] encrypts and commits the
 * items, then the caller asks the user to delete the originals, then [resolve] either settles the
 * move or undoes it. Held as a collaborator, like [MediaSelectionActions], so the four surfaces
 * share one definition; each gets its own instance, which remembers the move in flight.
 */
class MoveToSecretsActions(
    private val vault: VaultRepository,
    private val selectionActions: MediaSelectionActions,
) {

    private var inFlight: InFlight? = null

    /** Why a move cannot start right now, or null when it can. Cheap: no encryption happens. */
    fun blocker(): MoveStart? = when (val state = vault.state.value) {
        VaultState.NotSetUp -> MoveStart.NeedsSetup
        VaultState.Locked -> MoveStart.NeedsUnlock
        is VaultState.Unlocked -> if (state.needsNewPassphrase) MoveStart.NeedsSetup else null
    }

    suspend fun start(items: List<MediaItem>, onProgress: (done: Int) -> Unit = {}): MoveStart {
        if (items.isEmpty()) return MoveStart.Failed("Nothing was selected")
        blocker()?.let { return it }
        return when (val outcome = vault.add(items, onProgress)) {
            is AddToVaultOutcome.Failed -> MoveStart.Failed(outcome.reason)
            is AddToVaultOutcome.Added -> {
                inFlight = InFlight(outcome.secretIds, outcome.originalIds)
                MoveStart.NeedsConsent(outcome.originalIds, outcome.originalUris)
            }
        }
    }

    /**
     * Settles the move once the user has answered the delete dialog. Returns how many items moved,
     * or null when the originals stayed and the vault copies were destroyed.
     */
    suspend fun resolve(confirmed: Boolean): Int? {
        val move = inFlight ?: return null
        inFlight = null
        return if (confirmed) {
            vault.confirmMove(move.secretIds)
            selectionActions.confirmDeleted(move.originalIds)
            move.secretIds.size
        } else {
            vault.undoMove(move.secretIds)
            null
        }
    }

    private class InFlight(val secretIds: List<Long>, val originalIds: List<Long>)
}

sealed interface MoveStart {

    /** Encrypted and committed; the originals must now go through the system delete dialog. */
    data class NeedsConsent(val originalIds: List<Long>, val originalUris: List<String>) : MoveStart

    data object NeedsUnlock : MoveStart

    /** There is no usable vault yet: it has not been set up, or is mid-recovery. */
    data object NeedsSetup : MoveStart

    data class Failed(val reason: String) : MoveStart
}
