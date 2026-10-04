package com.abrarshakhi.galva.feature.secrets

import com.abrarshakhi.galva.core.ui.mvi.UiIntent
import com.abrarshakhi.galva.core.ui.mvi.UiState

enum class SetupStep {
    Intro,
    Passphrase,
    RecoveryPhrase,
    ConfirmPhrase,
}

data class VaultSetupUiState(
    val step: SetupStep = SetupStep.Intro,
    val recoveryWords: List<String> = emptyList(),
    val checkPositions: List<Int> = emptyList(),
    val error: String? = null,
    val isCreating: Boolean = false,
    val confirmingSkip: Boolean = false,
) : UiState

sealed interface VaultSetupIntent : UiIntent {

    data object Begin : VaultSetupIntent

    data class PassphraseChosen(
        val passphrase: CharSequence,
        val confirmation: CharSequence,
    ) : VaultSetupIntent

    data object PhraseWrittenDown : VaultSetupIntent

    data class CheckSubmitted(val answers: List<String>) : VaultSetupIntent

    data object SkipCheckRequested : VaultSetupIntent

    data object SkipCheckDismissed : VaultSetupIntent

    data object SkipCheckConfirmed : VaultSetupIntent

    data object Back : VaultSetupIntent
}
