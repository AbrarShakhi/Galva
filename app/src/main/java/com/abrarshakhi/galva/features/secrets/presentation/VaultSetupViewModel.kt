package com.abrarshakhi.galva.features.secrets.presentation

import com.abrarshakhi.galva.common.mvi.MviViewModel
import com.abrarshakhi.galva.core.vault.domain.model.RecoveryWords
import com.abrarshakhi.galva.core.vault.domain.repository.VaultRepository
import java.nio.CharBuffer

/**
 * Walks through creating the vault: a passphrase, then the recovery phrase, then a check that the
 * phrase was really written down — the only moment it is ever shown.
 *
 * Nothing is stored until the last step. The passphrase is held as a [CharArray] so it can be
 * overwritten, and the phrase leaves this ViewModel's state as soon as the vault exists.
 */
class VaultSetupViewModel(
    private val vault: VaultRepository,
) : MviViewModel<VaultSetupUiState, VaultSetupIntent, VaultSetupEffect>(VaultSetupUiState()) {

    private var passphrase: CharArray? = null

    override suspend fun reduce(intent: VaultSetupIntent) {
        when (intent) {
            VaultSetupIntent.Begin -> setState { copy(step = SetupStep.Passphrase, error = null) }

            is VaultSetupIntent.PassphraseChosen -> {
                val problem = PassphraseRules.problemWith(intent.passphrase, intent.confirmation)
                if (problem != null) {
                    setState { copy(error = problem) }
                    return
                }
                forgetPassphrase()
                passphrase = CharArray(intent.passphrase.length) { intent.passphrase[it] }
                // Going back to change the passphrase keeps the phrase already on paper.
                val words = currentState.recoveryWords.ifEmpty { vault.newRecoveryPhrase() }
                setState { copy(step = SetupStep.RecoveryPhrase, recoveryWords = words, error = null) }
            }

            VaultSetupIntent.PhraseWrittenDown -> setState {
                copy(
                    step = SetupStep.ConfirmPhrase,
                    checkPositions = (0 until RecoveryWords.COUNT).shuffled().take(CHECKED_WORDS).sorted(),
                    error = null,
                )
            }

            is VaultSetupIntent.CheckSubmitted -> create(intent.answers)

            VaultSetupIntent.Back -> setState {
                val previous = when (step) {
                    SetupStep.Intro, SetupStep.Passphrase -> SetupStep.Intro
                    SetupStep.RecoveryPhrase -> SetupStep.Passphrase
                    SetupStep.ConfirmPhrase -> SetupStep.RecoveryPhrase
                }
                copy(step = previous, error = null)
            }
        }
    }

    private suspend fun create(answers: List<String>) {
        val state = currentState
        val mismatch = state.checkPositions.zip(answers).firstOrNull { (position, answer) ->
            answer.trim().lowercase() != state.recoveryWords[position]
        }
        if (mismatch != null) {
            setState {
                copy(error = "Word ${mismatch.first + 1} doesn't match. Check what you wrote down.")
            }
            return
        }
        val secret = passphrase ?: run {
            setState { copy(step = SetupStep.Passphrase, error = "Choose your passphrase again") }
            return
        }

        setState { copy(isCreating = true, error = null) }
        try {
            vault.setUp(CharBuffer.wrap(secret), state.recoveryWords)
        } catch (error: Exception) {
            // Kept, so trying again does not mean retyping everything.
            setState { copy(isCreating = false, error = error.message ?: "Couldn't create Secrets") }
            return
        }
        forgetPassphrase()
        setState { VaultSetupUiState() }
    }

    private fun forgetPassphrase() {
        passphrase?.fill('\u0000')
        passphrase = null
    }

    override fun onCleared() {
        forgetPassphrase()
    }

    private companion object {
        const val CHECKED_WORDS = 3
    }
}
