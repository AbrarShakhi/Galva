package com.abrarshakhi.galva.features.secrets.presentation

import com.abrarshakhi.galva.common.mvi.UiEffect
import com.abrarshakhi.galva.common.mvi.UiIntent
import com.abrarshakhi.galva.common.mvi.UiState

enum class UnlockMode {
    Passphrase,
    RecoveryPhrase,
}

data class VaultUnlockUiState(
    val mode: UnlockMode = UnlockMode.Passphrase,
    val isWorking: Boolean = false,
    val error: String? = null,
    /** Set when nothing can open the vault on this device any more; only a reset is left. */
    val unavailable: String? = null,
    val confirmingReset: Boolean = false,
) : UiState

sealed interface VaultUnlockIntent : UiIntent {

    data class PassphraseSubmitted(val passphrase: CharSequence) : VaultUnlockIntent

    data class RecoveryPhraseSubmitted(val phrase: CharSequence) : VaultUnlockIntent

    data object UseRecoveryPhrase : VaultUnlockIntent

    data object UsePassphrase : VaultUnlockIntent

    data object ResetRequested : VaultUnlockIntent

    data object ResetDismissed : VaultUnlockIntent

    data object ResetConfirmed : VaultUnlockIntent
}

sealed interface VaultUnlockEffect : UiEffect {

    data object Unlocked : VaultUnlockEffect

    data class ShowMessage(val text: String) : VaultUnlockEffect
}
