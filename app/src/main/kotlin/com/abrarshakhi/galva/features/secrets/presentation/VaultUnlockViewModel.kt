package com.abrarshakhi.galva.features.secrets.presentation

import com.abrarshakhi.galva.common.mvi.MviViewModel
import com.abrarshakhi.galva.core.vault.domain.model.RecoveryWords
import com.abrarshakhi.galva.core.vault.domain.model.UnlockResult
import com.abrarshakhi.galva.core.vault.domain.repository.VaultRepository
import com.abrarshakhi.galva.core.vault.domain.usecase.UnlockThrottle

/**
 * Opens the vault, from the Secrets tab or from the sheet other screens show before a move.
 *
 * Entry-scoped, so a half-typed attempt is dropped with the screen; the attempt count lives in the
 * shared [UnlockThrottle] so switching screens does not reset the back-off.
 */
class VaultUnlockViewModel(
    private val vault: VaultRepository,
    private val throttle: UnlockThrottle,
) : MviViewModel<VaultUnlockUiState, VaultUnlockIntent, VaultUnlockEffect>(VaultUnlockUiState()) {

    override suspend fun reduce(intent: VaultUnlockIntent) {
        when (intent) {
            is VaultUnlockIntent.PassphraseSubmitted ->
                if (intent.passphrase.isNotEmpty()) attempt { vault.unlock(intent.passphrase) }

            is VaultUnlockIntent.RecoveryPhraseSubmitted ->
                attempt { vault.unlockWithRecoveryPhrase(RecoveryWords.parse(intent.phrase)) }

            VaultUnlockIntent.UseRecoveryPhrase ->
                setState { copy(mode = UnlockMode.RecoveryPhrase, error = null) }

            VaultUnlockIntent.UsePassphrase ->
                setState { copy(mode = UnlockMode.Passphrase, error = null) }

            VaultUnlockIntent.ResetRequested -> setState { copy(confirmingReset = true) }

            VaultUnlockIntent.ResetDismissed -> setState { copy(confirmingReset = false) }

            VaultUnlockIntent.ResetConfirmed -> {
                setState { copy(confirmingReset = false, isWorking = true) }
                vault.reset()
                throttle.recordSuccess()
                setState { VaultUnlockUiState() }
                sendEffect(VaultUnlockEffect.ShowMessage("Secrets was reset"))
            }
        }
    }

    private suspend fun attempt(unlock: suspend () -> UnlockResult) {
        val waitMs = throttle.waitMs()
        if (waitMs > 0) {
            val seconds = (waitMs + 999) / 1000
            setState { copy(error = "Too many wrong attempts. Try again in $seconds seconds.") }
            return
        }
        setState { copy(isWorking = true, error = null) }
        val result = unlock()
        setState { copy(isWorking = false) }
        when (result) {
            UnlockResult.Unlocked -> {
                throttle.recordSuccess()
                setState { VaultUnlockUiState() }
                sendEffect(VaultUnlockEffect.Unlocked)
            }

            UnlockResult.WrongSecret -> {
                throttle.recordFailure()
                setState {
                    copy(
                        error = if (mode == UnlockMode.Passphrase) "That passphrase is wrong"
                        else "That recovery phrase doesn't open these Secrets",
                    )
                }
            }

            UnlockResult.InvalidPhrase -> setState {
                copy(error = "A word is misspelled, missing or out of order. Check all 24.")
            }

            is UnlockResult.Unavailable -> setState { copy(unavailable = result.reason) }
        }
    }
}
