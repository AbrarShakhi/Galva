package com.abrarshakhi.galva.core.vault.domain.model

/** Whether the vault exists, and whether its keys are in memory right now. */
sealed interface VaultState {

    /** No vault has been created on this device. */
    data object NotSetUp : VaultState

    /** A vault exists but is sealed: no key that can read it is in memory. */
    data object Locked : VaultState

    data class Unlocked(
        val items: List<SecretItem>,
        /**
         * The vault was opened with the recovery phrase, so the forgotten passphrase must be
         * replaced before anything else happens.
         */
        val needsNewPassphrase: Boolean = false,
    ) : VaultState
}
