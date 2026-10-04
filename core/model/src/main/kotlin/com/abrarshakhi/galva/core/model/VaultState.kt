package com.abrarshakhi.galva.core.model

sealed interface VaultState {

    data object NotSetUp : VaultState

    data object Locked : VaultState

    data class Unlocked(
        val items: List<SecretItem>,
        val needsNewPassphrase: Boolean = false,
    ) : VaultState
}
