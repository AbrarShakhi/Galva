package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.model.AddToVaultOutcome
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.VaultState
import com.abrarshakhi.galva.core.vault.VaultRepository

class MoveToSecretsActions(
    private val vault: VaultRepository,
    private val selectionActions: MediaSelectionActions,
) {

    private var inFlight: InFlight? = null

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

    data class NeedsConsent(val originalIds: List<Long>, val originalUris: List<String>) : MoveStart

    data object NeedsUnlock : MoveStart

    data object NeedsSetup : MoveStart

    data class Failed(val reason: String) : MoveStart
}
