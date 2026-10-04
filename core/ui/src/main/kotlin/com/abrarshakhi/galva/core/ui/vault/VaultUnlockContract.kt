package com.abrarshakhi.galva.core.ui.vault

import com.abrarshakhi.galva.core.ui.message.UserMessage
import com.abrarshakhi.galva.core.ui.mvi.UiIntent
import com.abrarshakhi.galva.core.ui.mvi.UiState

enum class UnlockMode {
    Passphrase,
    RecoveryPhrase,
}

data class VaultUnlockUiState(
    val mode: UnlockMode = UnlockMode.Passphrase,
    val isWorking: Boolean = false,
    val error: String? = null,
    val unavailable: String? = null,
    val confirmingReset: Boolean = false,
    val unlocked: Boolean = false,
    val messages: List<UserMessage> = emptyList(),
) : UiState

sealed interface VaultUnlockIntent : UiIntent {

    data class PassphraseSubmitted(val passphrase: CharSequence) : VaultUnlockIntent

    data class RecoveryPhraseSubmitted(val phrase: CharSequence) : VaultUnlockIntent

    data object UseRecoveryPhrase : VaultUnlockIntent

    data object UsePassphrase : VaultUnlockIntent

    data object ResetRequested : VaultUnlockIntent

    data object ResetDismissed : VaultUnlockIntent

    data object ResetConfirmed : VaultUnlockIntent

    data object UnlockHandled : VaultUnlockIntent

    data class MessageShown(val id: Long) : VaultUnlockIntent
}
