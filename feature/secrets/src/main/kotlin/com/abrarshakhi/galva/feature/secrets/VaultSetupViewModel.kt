package com.abrarshakhi.galva.feature.secrets

import com.abrarshakhi.galva.core.model.RecoveryWords
import com.abrarshakhi.galva.core.ui.mvi.MviViewModel
import com.abrarshakhi.galva.core.ui.vault.PassphraseRules
import com.abrarshakhi.galva.core.vault.VaultRepository
import java.nio.CharBuffer

class VaultSetupViewModel(
    private val vault: VaultRepository,
) : MviViewModel<VaultSetupUiState, VaultSetupIntent>(VaultSetupUiState()) {

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
                val words = currentState.recoveryWords.ifEmpty { vault.newRecoveryPhrase() }
                setState {
                    copy(step = SetupStep.RecoveryPhrase, recoveryWords = words, error = null)
                }
            }

            VaultSetupIntent.PhraseWrittenDown -> setState {
                copy(
                    step = SetupStep.ConfirmPhrase,
                    checkPositions = (0 until RecoveryWords.COUNT)
                        .shuffled()
                        .take(CHECKED_WORDS)
                        .sorted(),
                    error = null,
                )
            }

            is VaultSetupIntent.CheckSubmitted -> {
                val mismatch = firstMismatch(intent.answers)
                if (mismatch != null) {
                    setState {
                        copy(error = "Word ${mismatch + 1} doesn't match. Check what you wrote down.")
                    }
                } else {
                    create()
                }
            }

            VaultSetupIntent.SkipCheckRequested -> setState { copy(confirmingSkip = true) }

            VaultSetupIntent.SkipCheckDismissed -> setState { copy(confirmingSkip = false) }

            VaultSetupIntent.SkipCheckConfirmed -> {
                setState { copy(confirmingSkip = false) }
                create()
            }

            VaultSetupIntent.Back -> setState {
                val previous = when (step) {
                    SetupStep.Intro, SetupStep.Passphrase -> SetupStep.Intro
                    SetupStep.RecoveryPhrase -> SetupStep.Passphrase
                    SetupStep.ConfirmPhrase -> SetupStep.RecoveryPhrase
                }
                copy(step = previous, error = null, confirmingSkip = false)
            }
        }
    }

    private fun firstMismatch(answers: List<String>): Int? {
        val state = currentState
        return state.checkPositions.zip(answers).firstOrNull { (position, answer) ->
            answer.trim().lowercase() != state.recoveryWords[position]
        }?.first
    }

    private suspend fun create() {
        val state = currentState
        val secret = passphrase ?: run {
            setState { copy(step = SetupStep.Passphrase, error = "Choose your passphrase again") }
            return
        }

        setState { copy(isCreating = true, error = null) }
        try {
            vault.setUp(CharBuffer.wrap(secret), state.recoveryWords)
        } catch (error: Exception) {
            val reason = error.message ?: "Couldn't create Secrets"
            setState { copy(isCreating = false, error = reason) }
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
