package com.abrarshakhi.galva.features.secrets.presentation

import com.abrarshakhi.galva.common.mvi.UiEffect
import com.abrarshakhi.galva.common.mvi.UiIntent
import com.abrarshakhi.galva.common.mvi.UiState

enum class SetupStep {
    Intro,
    Passphrase,
    RecoveryPhrase,
    ConfirmPhrase,
}

data class VaultSetupUiState(
    val step: SetupStep = SetupStep.Intro,
    val recoveryWords: List<String> = emptyList(),
    /** Zero-based positions of the words the user types back to prove they wrote them down. */
    val checkPositions: List<Int> = emptyList(),
    val error: String? = null,
    val isCreating: Boolean = false,
) : UiState

sealed interface VaultSetupIntent : UiIntent {

    data object Begin : VaultSetupIntent

    data class PassphraseChosen(
        val passphrase: CharSequence,
        val confirmation: CharSequence,
    ) : VaultSetupIntent

    data object PhraseWrittenDown : VaultSetupIntent

    data class CheckSubmitted(val answers: List<String>) : VaultSetupIntent

    data object Back : VaultSetupIntent
}

/** Setup finishes by the vault unlocking, which the Secrets tab follows; nothing is one-shot. */
sealed interface VaultSetupEffect : UiEffect
